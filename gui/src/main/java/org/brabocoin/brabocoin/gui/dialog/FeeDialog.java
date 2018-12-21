package org.brabocoin.brabocoin.gui.dialog;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.gui.BrabocoinGUI;
import org.brabocoin.brabocoin.gui.control.KeyDropDown;
import org.brabocoin.brabocoin.gui.control.NumberTextField;
import org.brabocoin.brabocoin.wallet.Wallet;

import java.util.AbstractMap;
import java.util.Map;

public class FeeDialog extends Dialog<Map.Entry<Long, PublicKey>> {
    private Label messageLabel;
    private Node buttonNode;

    public FeeDialog(Wallet wallet, long inputSum, long outputSum) {
        this.setHeaderText("Enter transaction fee");
        this.setTitle("Transaction fee");

        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        buttonNode = this.getDialogPane().lookupButton(ButtonType.OK);
        buttonNode.setDisable(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Add message label
        messageLabel = new Label("");
        messageLabel.getStyleClass().add("error");
        grid.add(messageLabel, 0, 0, 2, 1);
        hideErrorLabel();


        NumberTextField feeField = new NumberTextField();
        KeyDropDown dropDown = new KeyDropDown(wallet);

        feeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (feeField.getText().equals("")) {
                buttonNode.setDisable(true);
                return;
            }

            long fee = Long.parseLong(feeField.getText());
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

        this.getDialogPane()
            .getStylesheets()
            .add(BrabocoinGUI.class.getResource("brabocoin.css").toExternalForm());

        this.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new AbstractMap.SimpleEntry<>(
                    Long.parseLong(feeField.getText()),
                    dropDown.getSelectionModel().getSelectedItem()
                );
            }
            return null;
        });
    }

    private void setError(String message) {
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.setText(message);

        buttonNode.setDisable(true);

        getDialogPane().getScene().getWindow().sizeToScene();
    }

    private void hideErrorLabel() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        buttonNode.setDisable(false);

        getDialogPane().getScene().getWindow().sizeToScene();
    }
}
