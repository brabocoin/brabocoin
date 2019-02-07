package org.brabocoin.brabocoin.gui.dialog;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import org.brabocoin.brabocoin.util.Destructible;
import org.controlsfx.validation.ValidationMessage;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Dialog that prompts for a password to unlock an object.
 *
 * When the dialog is used for creation, a confirmation password is also required by the user.
 *
 * @param <T> The type of the object to unlock.
 */
public class UnlockDialog<T> extends BraboDialog<T> {

    private T object;

    private Function<Destructible<char[]>, T> unlocker;

    private ValidationSupport validationSupport;
    private PasswordField passwordField;
    private PasswordField confirmField;
    private Label messageLabel;

    public UnlockDialog(boolean creation, @NotNull Function<@NotNull Destructible<char[]>, @Nullable T> unlocker) {
        this(creation, unlocker, "Unlock");
    }

    public UnlockDialog(boolean creation, @NotNull Function<@NotNull Destructible<char[]>, @Nullable T> unlocker, String okButtonText) {
        super(false);
        this.unlocker = unlocker;

        // Add buttons
        ButtonType unlockButtonType = new ButtonType(okButtonText, ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(unlockButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Validation support
        validationSupport = new ValidationSupport();

        // Add message label
        messageLabel = new Label("");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.getStyleClass().add("error");
        grid.add(messageLabel, 0, 0, 2, 1);

        // Add password field
        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        validationSupport.registerValidator(passwordField, false, Validator.createEmptyValidator("Password is required"));

        grid.addRow(1, new Label("Password:"), passwordField);

        // If for creation, create a confirm password field
        if (creation) {
            confirmField = new PasswordField();
            confirmField.setPromptText("Confirm password");
            validationSupport.registerValidator(confirmField, false, Validator.createEmptyValidator("Password confirmation is required"));

            grid.addRow(2, new Label("Confirm password:"), confirmField);
        }

        getDialogPane().setContent(grid);

        // Disable/enable button depending on validation
        Button unlockButton = (Button)getDialogPane().lookupButton(unlockButtonType);
        unlockButton.setDisable(true);
        validationSupport.invalidProperty().addListener((obs, old, invalid) -> {
            unlockButton.setDisable(invalid);
        });

        // Unlock button validation
        unlockButton.addEventFilter(
            ActionEvent.ACTION,
            creation ? this::attemptCreation : this::attemptUnlock
        );

        // Result converter for the dialog
        setResultConverter(button -> {
            if (button == unlockButtonType) {
                return object;
            }

            return null;
        });

        Platform.runLater(passwordField::requestFocus);
    }

    private void attemptUnlock(ActionEvent event) {
        T object = unlocker.apply(
            new Destructible<>(() ->passwordField.getText().toCharArray())
        );

        if (object == null) {
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
            messageLabel.setText("Password is incorrect.");

            validationSupport.getValidationDecorator().removeDecorations(passwordField);
            validationSupport.getValidationDecorator().applyValidationDecoration(
                ValidationMessage.error(passwordField, "Password is incorrect"));

            getDialogPane().getScene().getWindow().sizeToScene();

            event.consume();
        }
        else {
            this.object = object;
        }
    }

    private void attemptCreation(ActionEvent event) {
        // Check if passwords match
        if (!passwordField.getText().equals(confirmField.getText())) {
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
            messageLabel.setText("Passwords do not match.");

            validationSupport.getValidationDecorator().removeDecorations(confirmField);
            validationSupport.getValidationDecorator().applyValidationDecoration(
                ValidationMessage.error(confirmField, "Password does not match"));

            getDialogPane().getScene().getWindow().sizeToScene();

            event.consume();
        }
        else {
            this.object = unlocker.apply(
                new Destructible<>(() -> passwordField.getText().toCharArray())
            );
        }
    }
}
