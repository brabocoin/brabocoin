package org.brabocoin.brabocoin.gui;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public class BraboDialog<T> extends Dialog<T> {
    public BraboDialog() {
        setDialogPane(new BraboDialogPane());

        // Add base stylesheet
        this.getDialogPane().getStylesheets().add(BraboDialog.class.getResource("brabocoin.css").toExternalForm());

        // Add close button
        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = this.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.managedProperty().bind(closeButton.visibleProperty());
        closeButton.setVisible(false);
    }
}
