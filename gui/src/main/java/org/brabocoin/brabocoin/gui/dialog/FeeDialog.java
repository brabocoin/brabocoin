package org.brabocoin.brabocoin.gui.dialog;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.gui.control.DecimalTextField;
import org.brabocoin.brabocoin.gui.control.KeyDropDown;
import org.brabocoin.brabocoin.wallet.Wallet;

import java.util.AbstractMap;
import java.util.Map;

public class FeeDialog extends BraboValidatedDialog<Map.Entry<Long, PublicKey>> {

    public FeeDialog(Wallet wallet, long inputSum, long outputSum) {
        super();
        this.setHeaderText("Enter transaction fee");
        this.setTitle("Transaction fee");

        grid.add(messageLabel, 0, 0, 2, 1);

        DecimalTextField feeField = new DecimalTextField();
        KeyDropDown dropDown = new KeyDropDown(wallet);

        feeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (feeField.getText().equals("")) {
                okButtonNode.setDisable(true);
                return;
            }

            long fee = feeField.getCents();
            long changeValue = inputSum - outputSum - fee;
            if (changeValue <= 0) {
                setError("Insufficient input for fee");
                return;
            } else if (fee <= 0) {
                setError("Fee should be positive");
                return;
            }
            hideErrorLabel();
        });

        grid.add(new Label("Fee:"), 0, 1);
        grid.add(feeField, 1, 1);
        grid.add(new Label("Output address:"), 0, 2);
        grid.add(dropDown, 1, 2);

        this.getDialogPane().setContent(grid);

        this.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new AbstractMap.SimpleEntry<>(
                    feeField.getCents(),
                    dropDown.getSelectionModel().getSelectedItem()
                );
            }
            return null;
        });
    }
}
