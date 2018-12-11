package org.brabocoin.brabocoin.gui;

import javafx.scene.control.Dialog;
import org.fxmisc.cssfx.CSSFX;

import java.nio.file.Paths;

public class BraboDialog extends Dialog {
    public BraboDialog() {
        // Auto CSS reloading
        CSSFX.addConverter(uri -> Paths.get(uri.startsWith("file:/") ? uri.replace("file:/", "")
            .replace("out/production/", "src/main/") : uri)).start();

        // Add base stylesheet
        this.getDialogPane().getStylesheets().add(BraboDialog.class.getResource("brabocoin.css").toExternalForm());
    }
}
