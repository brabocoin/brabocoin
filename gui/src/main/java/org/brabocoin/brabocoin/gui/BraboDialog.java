package org.brabocoin.brabocoin.gui;

import javafx.scene.control.Dialog;

public class BraboDialog extends Dialog {
    public BraboDialog() {
        // Add base stylesheet
        this.getDialogPane().getStylesheets().add(BraboDialog.class.getResource("brabocoin.css").toExternalForm());
    }
}
