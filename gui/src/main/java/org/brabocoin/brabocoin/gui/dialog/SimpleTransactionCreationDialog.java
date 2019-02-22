package org.brabocoin.brabocoin.gui.dialog;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.gui.auxil.SimpleTransactionCreationResult;
import org.brabocoin.brabocoin.gui.control.DecimalTextField;
import org.brabocoin.brabocoin.gui.control.KeyDropDown;
import org.brabocoin.brabocoin.gui.util.GUIUtils;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.util.Base58Check;
import org.brabocoin.brabocoin.validation.Consensus;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.text.MessageFormat;

public class SimpleTransactionCreationDialog extends BraboDialog<SimpleTransactionCreationResult> {

    private final State state;

    public SimpleTransactionCreationDialog(State state) {
        super(false);
        this.state = state;

        // Add buttons
        ButtonType sendButtonType = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);

        long spendableCents = state.getWallet().computeBalance(true);
        this.setGraphic(null);
        this.setHeaderText(MessageFormat.format(
            "Spendable balance: {0}",
            GUIUtils.formatValue(spendableCents, true)
        ));
        this.setTitle("Transaction creation");

        GridPane gridPane = new GridPane();

        // Validation support
        ValidationSupport validationSupport = new ValidationSupport();

        TextField addressField = new TextField();
        validationSupport.registerValidator(
            addressField,
            false,
            Validator.createEmptyValidator("Address is required")
        );
        validationSupport.registerValidator(
            addressField,
            false,
            Validator.<String>createPredicateValidator(
                s -> {
                    try {
                        Base58Check.decode(s);
                    }
                    catch (IllegalArgumentException e) {
                        return false;
                    }
                    return true;
                }, "Invalid address"
            )
        );

        Validator<String> isDoubleValidator = Validator.createPredicateValidator(
            s -> {
                try {
                    Double.parseDouble(s);
                }
                catch (NumberFormatException e) {
                    return false;
                }
                return true;
            }, "Not a valid number"
        );

        Validator<String> positiveValidator = Validator.createPredicateValidator(
            s -> {
                try {
                    double d = Double.parseDouble(s);
                    return d > 0.0;
                }
                catch (NumberFormatException e) {
                    return false;
                }
            }, "Value should be positive"
        );

        DecimalTextField amountField = new DecimalTextField();
        validationSupport.registerValidator(amountField, false, Validator.combine(
            isDoubleValidator,
            positiveValidator,
            Validator.createPredicateValidator(
                s -> {
                    try {
                        double d = Double.parseDouble(s);
                        return d * Consensus.COIN < spendableCents;
                    }
                    catch (NumberFormatException e) {
                        return false;
                    }
                }, "Amount should be less than spendable balance"
            )
        ));

        DecimalTextField feeField = new DecimalTextField();
        validationSupport.registerValidator(feeField, false, Validator.combine(
            isDoubleValidator,
            positiveValidator,
            Validator.createPredicateValidator(
                s -> {
                    try {
                        double amount = Double.parseDouble(amountField.getText());
                        double fee = Double.parseDouble(s);
                        return (amount + fee) * Consensus.COIN <= spendableCents;
                    }
                    catch (NumberFormatException e) {
                        return false;
                    }
                }, "Output amount plus fee must be less or equal to spendable balance"
            )
        ));

        KeyDropDown changeAddress = new KeyDropDown(state.getWallet());

        gridPane.setHgap(10);
        gridPane.setVgap(10);

        gridPane.add(new Label("Address:"), 0, 0, 1, 1);
        gridPane.add(addressField, 1, 0, 1, 1);
        gridPane.add(new Label("Amount:"), 0, 1, 1, 1);
        gridPane.add(amountField, 1, 1, 1, 1);
        gridPane.add(new Separator(), 0, 2, 2, 1);
        gridPane.add(new Label("Fee:"), 0, 3, 1, 1);
        gridPane.add(feeField, 1, 3, 1, 1);
        gridPane.add(new Separator(), 0, 4, 2, 1);
        gridPane.add(new Label("Change address"), 0, 5, 1, 1);
        gridPane.add(changeAddress, 1, 5, 1, 1);

        this.getDialogPane().setContent(gridPane);

        BraboDialog.setBraboStyling(this.getDialogPane());

        // Disable/enable button depending on validation
        Button okButton = (Button)getDialogPane().lookupButton(sendButtonType);
        okButton.setDisable(true);
        validationSupport.invalidProperty().addListener((obs, old, invalid) -> {
            okButton.setDisable(invalid);
        });

        this.setOnShowing(e -> addressField.requestFocus());

        this.setResultConverter(button -> {
            if (button == sendButtonType) {
                return new SimpleTransactionCreationResult(
                    PublicKey.getHashFromBase58Address(addressField.getText()),
                    changeAddress.getSelectionModel().getSelectedItem().getHash(),
                    (long)(Double.parseDouble(amountField.getText()) * Consensus.COIN),
                    (long)(Double.parseDouble(feeField.getText()) * Consensus.COIN)
                );
            }

            return null;
        });
    }
}
