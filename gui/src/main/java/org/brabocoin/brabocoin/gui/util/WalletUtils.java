package org.brabocoin.brabocoin.gui.util;

import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.gui.dialog.UnlockDialog;
import org.brabocoin.brabocoin.node.state.State;

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
                } catch (CipherException | IOException e) {
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
}
