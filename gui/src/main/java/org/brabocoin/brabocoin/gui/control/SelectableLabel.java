package org.brabocoin.brabocoin.gui.control;

import com.sun.javafx.scene.control.skin.TextFieldSkin;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Selectable labels. Because why not.
 */
public class SelectableLabel extends HBox implements BraboControl, Initializable {

    private final StringProperty text = new SimpleStringProperty();

    @FXML private TextField textField;
    @FXML private Label before;
    @FXML private Label after;
    private boolean truncateNeeded;

    public SelectableLabel() {
        super();
        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        textField.textProperty().bind(text);
        textField.focusTraversableProperty().bind(focusTraversableProperty());

        textField.skinProperty().addListener((obs, old, skin) -> {
            Text text = ((Text)((Pane)((TextFieldSkin)skin).getChildren()
                .get(0)).getChildren().filtered(n -> n instanceof Text).get(0));

            text.boundsInParentProperty().addListener((obsT, oldT, bounds) -> {
                if (!truncateNeeded) return;

                boolean showBefore = bounds.getMinX() < 0;
                boolean showAfter = bounds.getMaxX() > textField.getWidth();
                Platform.runLater(() -> {
                    before.setManaged(showBefore);
                    before.setVisible(showBefore);
                    after.setManaged(showAfter);
                    after.setVisible(showAfter);
                });
            });

            textField.widthProperty().addListener((obsT, oldT, width) -> {
                truncateNeeded = width.doubleValue() < text.getBoundsInParent().getWidth();
                Platform.runLater(() -> {
                    if (!truncateNeeded) {
                        before.setManaged(false);
                        before.setVisible(false);
                        after.setManaged(false);
                        after.setVisible(false);
                    }
                    else {
                        boolean showBefore = text.getBoundsInParent().getMinX() < 0;
                        boolean showAfter = text.getBoundsInParent().getMaxX() > textField.getWidth();
                        Platform.runLater(() -> {
                            before.setManaged(showBefore);
                            before.setVisible(showBefore);
                            after.setManaged(showAfter);
                            after.setVisible(showAfter);
                        });
                    }
                });
            });
        });

    }

    public String getText() {
        return text.get();
    }

    public void setText(String value) {
        this.text.set(value);
    }

    public StringProperty textProperty() {
        return text;
    }
}
