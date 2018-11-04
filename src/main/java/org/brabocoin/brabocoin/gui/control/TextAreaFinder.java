package org.brabocoin.brabocoin.gui.control;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.controlsfx.control.textfield.CustomTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sten Wessel
 */
public class TextAreaFinder extends ToolBar implements BraboControl, Initializable {

    @FXML private CustomTextField searchField;
    @FXML private CheckBox regexCheckBox;
    @FXML private CheckBox caseCheckBox;
    @FXML private Label errorText;

    private final @NotNull TextArea textArea;
    private Pattern pattern;
    private int currentIndex;

    /**
     * Called when the finder wants to close itself. This should be set by the parent element
     * that can actually close this node.
     */
    private @Nullable Runnable onCloseRequest;

    /**
     * Create a text area finder for the given text area.
     */
    public TextAreaFinder(@NotNull TextArea textArea) {
        super();
        BraboControlInitializer.initialize(this);

        this.textArea = textArea;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Perform find when text is typed
        searchField.textProperty().addListener((obs, old, text) -> performFind());

        regexCheckBox.selectedProperty().addListener((obs, old, selected) -> performFind());
        caseCheckBox.selectedProperty().addListener((obs, old, selected) -> performFind());

        // Add close binding
        searchField.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            }
        });

        Platform.runLater(() -> searchField.requestFocus());
    }

    private void performFind() {
        clear();

        String text = searchField.getText();

        if (text.length() == 0) {
            return;
        }

        int flags = 0;

        if (!regexCheckBox.isSelected()) {
            flags += Pattern.LITERAL;
        }

        if (!caseCheckBox.isSelected()) {
            flags += Pattern.CASE_INSENSITIVE;
        }

        try {
            pattern = Pattern.compile(text, flags);
        }
        catch (Exception e) {
            errorText.setText("Invalid expression");
            // TODO: show error
            return;
        }

        matchNext();
    }

    private void clear() {
        errorText.setText(null);
        currentIndex = 0;
    }

    private void selectCurrentMatch(int start, int end) {
        textArea.selectRange(start, end);
    }

    @FXML
    private void matchNext() {
        Matcher matcher = pattern.matcher(textArea.getText());

        if (matcher.find(currentIndex) || (currentIndex != 0 && matcher.find())) {
            currentIndex = matcher.end();
            selectCurrentMatch(matcher.start(), matcher.end());
        }
        else {
            errorText.setText("No matches");
        }
    }

    @FXML
    private void close() {
        if (onCloseRequest != null) {
            onCloseRequest.run();
        }
    }

    public void setOnCloseRequest(@Nullable Runnable onCloseRequest) {
        this.onCloseRequest = onCloseRequest;
    }
}
