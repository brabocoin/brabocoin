package org.brabocoin.brabocoin.gui.dialog;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.brabocoin.brabocoin.gui.BrabocoinGUI;

import java.util.stream.Collectors;

public class BraboDialog<T> extends Dialog<T> {

    public BraboDialog() {
        this(true);
    }

    public BraboDialog(Boolean removeDialogButtons) {
        if (removeDialogButtons) {
            setDialogPane(new BraboDialogPane());
        }

        // Add base stylesheet
        this.getDialogPane()
            .getStylesheets()
            .add(BrabocoinGUI.class.getResource("brabocoin.css").toExternalForm());

        // Add close button
        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = this.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.managedProperty().bind(closeButton.visibleProperty());
        closeButton.setVisible(false);

        ((Stage)this.getDialogPane().getScene().getWindow()).getIcons()
            .addAll(BrabocoinGUI.ICONS.stream()
                .map(path -> new Image(BrabocoinGUI.class.getResourceAsStream(path)))
                .collect(Collectors.toList()));
    }
}
