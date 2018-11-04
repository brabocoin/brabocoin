package org.brabocoin.brabocoin.gui.view;

import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import org.apache.commons.lang.StringUtils;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.LogTextArea;
import org.brabocoin.brabocoin.logging.BraboLogLevel;
import org.brabocoin.brabocoin.logging.TextAreaHandler;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Sten Wessel
 */
public class LogPane extends BorderPane implements BraboControl, Initializable {

    @FXML private ComboBox<Level> logLevelComboBox;
    @FXML private LogTextArea logTextArea;

    @FXML private ToggleButton wordWrapToggleButton;
    @FXML private ToggleButton scrollToEndToggleButton;

    private Runnable onCloseRequest;

    public LogPane() {
        super();
        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Install log handler for whole application
        Handler logHandler = new TextAreaHandler(logTextArea);
        Logger rootLogger = Logger.getLogger("org.brabocoin.brabocoin");
        rootLogger.addHandler(logHandler);

        // Set combo box rendering of log level instances
        Callback<ListView<Level>, ListCell<Level>> cellFactory = listView -> new ListCell<Level>() {
            @Override
            protected void updateItem(Level level, boolean empty) {
                super.updateItem(level, empty);
                if (empty || level == null) {
                    setText(null);
                }
                else {
                    setText(StringUtils.capitalize(level.toString().toLowerCase()));
                }
            }
        };

        logLevelComboBox.setCellFactory(cellFactory);
        logLevelComboBox.setButtonCell(cellFactory.call(null));

        // Reset skin due to JDK BUG (fixed in Java 9)
        // https://bugs.openjdk.java.net/browse/JDK-8152532
        // Resetting the skin here apparently solves the issue
        logLevelComboBox.setSkin(new ComboBoxListViewSkin<>(logLevelComboBox));

        // Gather log levels
        ObservableList<Level> logLevels = Arrays.stream(BraboLogLevel.class.getFields())
            .filter(f -> Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers()))
            .filter(f -> f.getType().isAssignableFrom(Level.class))
            .map(f -> {
                try {
                    return (Level)f.get(null);
                }
                catch (IllegalAccessException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(Level::intValue))
            .collect(Collectors.toCollection(FXCollections::observableArrayList));

        logLevelComboBox.setItems(logLevels);

        // Bind log level combo box to log handler
        rootLogger.setLevel(Level.FINE);
        logLevelComboBox.setValue(rootLogger.getLevel());
        logLevelComboBox.valueProperty().addListener((obs, old, val) -> rootLogger.setLevel(val));

        // Bind word wrap toggle
        wordWrapToggleButton.selectedProperty().bindBidirectional(logTextArea.wrapTextProperty());

        // Bind scroll to end toggle
        scrollToEndToggleButton.selectedProperty().bindBidirectional(logTextArea.autoScrollToEndProperty());
    }

    @FXML
    private void closePane(ActionEvent event) {
        if (onCloseRequest != null) {
            onCloseRequest.run();
        }
    }

    public void setOnCloseRequest(Runnable closeRequest) {
        this.onCloseRequest = closeRequest;
    }
}
