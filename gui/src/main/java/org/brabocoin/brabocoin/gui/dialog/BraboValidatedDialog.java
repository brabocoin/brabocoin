package org.brabocoin.brabocoin.gui.dialog;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class BraboValidatedDialog<T> extends BraboDialog<T> {
    Label messageLabel;
    Node buttonNode;

    GridPane grid = new GridPane();

    public BraboValidatedDialog() {
        super(false);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        buttonNode = this.getDialogPane().lookupButton(ButtonType.OK);

        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Add message label
        messageLabel = new Label("");
        messageLabel.getStyleClass().add("error");
        hideErrorLabel();
    }


    void setError(String message) {
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.setText(message);

        buttonNode.setDisable(true);

        getDialogPane().getScene().getWindow().sizeToScene();
    }

    void hideErrorLabel() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        buttonNode.setDisable(false);

        getDialogPane().getScene().getWindow().sizeToScene();
    }
}
