package org.brabocoin.brabocoin.gui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.gui.dialog.BraboDialog;
import org.brabocoin.brabocoin.gui.dialog.UnlockDialog;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.UnsignedTransaction;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidationResult;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.brabocoin.brabocoin.wallet.TransactionSigningResult;
import org.brabocoin.brabocoin.wallet.TransactionSigningStatus;
import org.brabocoin.brabocoin.wallet.Wallet;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public class WalletUtils {

    public static Optional<Object> saveWallet(State state) {
        UnlockDialog<Object> passwordDialog = new UnlockDialog<>(
            false,
            (d) -> {
                try {
                    state.getWalletIO().getCipher().decyrpt(
                        Files.readAllBytes(state.getWalletFile().toPath()),
                        d.getReference().get()
                    );
                }
                catch (CipherException | IOException e) {
                    try {
                        d.destruct();
                    }
                    catch (DestructionException e1) {
                        // ignore
                    }
                    return null;
                }

                try {
                    state.getWalletIO().write(
                        state.getWallet(),
                        state.getWalletFile(),
                        state.getTxHistoryFile(),
                        d
                    );
                    d.destruct();
                }
                catch (IOException | DestructionException | CipherException e) {
                    return null;
                }
                return new Object();
            },
            "Ok"
        );

        passwordDialog.setTitle("Wallet password");
        passwordDialog.setHeaderText("Enter a password to encrypt your wallet");

        return passwordDialog.showAndWait();
    }

    public static String getPendingStyle(long difference) {
        String textColor = "black";
        if (difference > 0) {
            textColor = "green";
        }
        else if (difference < 0) {
            textColor = "red";
        }

        return "-fx-text-fill: " + textColor;
    }

    public static TransactionSigningResult signTransaction(UnsignedTransaction unsignedTransaction,
                                                           Wallet wallet) {
        TransactionSigningResult result = null;
        do {
            try {
                result = wallet.signTransaction(unsignedTransaction);
            }
            catch (DatabaseException | DestructionException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error occurred");
                alert.setHeaderText("Transaction creation error");
                alert.setContentText(
                    "Transaction signing failed due to database or destruction error.");
                BraboDialog.setBraboStyling(alert.getDialogPane());

                alert.showAndWait();
                break;
            }

            if (result.getStatus() == TransactionSigningStatus.PRIVATE_KEY_LOCKED) {
                KeyPair lockedKeyPair = result.getLockedKeyPair();

                UnlockDialog<Object> privateKeyUnlockDialog = new UnlockDialog<>(
                    false,
                    (d) -> {
                        try {
                            lockedKeyPair.getPrivateKey().unlock(d);
                        }
                        catch (CipherException | DestructionException e) {
                            return null;
                        }

                        return new Object();
                    }
                );

                privateKeyUnlockDialog.setTitle("Unlock private key");
                privateKeyUnlockDialog.setHeaderText("Enter password for address: " + lockedKeyPair.getPublicKey()
                    .getBase58Address());

                Optional<Object> unlockResult = privateKeyUnlockDialog.showAndWait();

                if (!unlockResult.isPresent()) {
                    break;
                }
            }
        }
        while (result.getStatus() == TransactionSigningStatus.PRIVATE_KEY_LOCKED);

        return result;
    }

    public static boolean sendTransaction(Transaction transaction,
                                          TransactionValidator transactionValidator,
                                          NodeEnvironment environment, Wallet wallet) {
        TransactionValidationResult validationResult = transactionValidator.validate(
            transaction,
            TransactionValidator.ALL,
            true
        );

        boolean valid = validationResult.isPassed();
        Alert alert = new Alert(
            valid ? Alert.AlertType.CONFIRMATION : Alert.AlertType.WARNING
        );

        if (!valid) {
            alert.getButtonTypes().add(ButtonType.CANCEL);
        }
        BraboDialog.setBraboStyling(alert.getDialogPane());

        alert.setTitle("Send transaction");
        alert.setHeaderText(String.format("Your transaction is %s.", validationResult.toString()));
        alert.setContentText("Are you sure you want to send the transaction to your peers?");

        Optional<ButtonType> result = alert.showAndWait();

        if (!result.isPresent()) {
            return false;
        }

        if (result.get() != ButtonType.OK) {
            return false;
        }

        ValidationStatus status = environment.processNewlyCreatedTransaction(transaction);

        if (status == ValidationStatus.VALID) {
            transaction.getInputs().forEach(wallet::addUsedInput);
        }
        return true;
    }
}
