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
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.LogTextArea;
import org.brabocoin.brabocoin.gui.control.TextAreaFinder;
import org.brabocoin.brabocoin.logging.BraboLogLevel;
import org.brabocoin.brabocoin.logging.TextAreaHandler;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Pinned pane showing the application logs.
 */
public class LogPane extends BorderPane implements BraboControl, Initializable {

    private final Level initLogLevel;
    private final MinerView minerView;
    @FXML private ToolBar paneBar;
    @FXML private ComboBox<Level> logLevelComboBox;

    @FXML private VBox contentWrapper;
    @FXML private LogTextArea logTextArea;

    @FXML private ToggleButton wordWrapToggleButton;
    @FXML private ToggleButton scrollToEndToggleButton;
    @FXML private ToggleButton findToggleButton;

    private final static Level MIN_MINING_LOG_LEVEL = Level.FINE;

    /**
     * Called when the log pane wants to close itself. This should be set by the parent element
     * that can actually close this pane.
     */
    private @Nullable Runnable onCloseRequest;

    private @Nullable TextAreaFinder finder;

    /**
     * Create a new log pane.
     */
    public LogPane(Level initLogLevel, MinerView minerView) {
        super();
        this.initLogLevel = initLogLevel;
        this.minerView = minerView;
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
                    setText(level.toString().toUpperCase().charAt(0) + level.toString()
                        .toLowerCase()
                        .substring(1));
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
        rootLogger.setLevel(initLogLevel);
        logLevelComboBox.setValue(rootLogger.getLevel());
        logLevelComboBox.valueProperty().addListener((obs, old, val) -> {
            minerView.setMiningDisabled(val.intValue() <= MIN_MINING_LOG_LEVEL.intValue());
            rootLogger.setLevel(val);
        });

        // Bind word wrap toggle
        wordWrapToggleButton.selectedProperty().bindBidirectional(logTextArea.wrapTextProperty());

        // Bind scroll to end toggle
        scrollToEndToggleButton.selectedProperty()
            .bindBidirectional(logTextArea.autoScrollToEndProperty());

        // Bind find button
        findToggleButton.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) {
                finder = new TextAreaFinder(logTextArea);
                finder.setOnCloseRequest(() -> findToggleButton.setSelected(false));

                contentWrapper.getChildren().add(0, finder);
                VBox.setVgrow(finder, Priority.NEVER);

                logTextArea.getStyleClass().add("search-enabled");
            }
            else {
                contentWrapper.getChildren().remove(finder);
                logTextArea.getStyleClass().remove("search-enabled");
            }
        });

        // Add shortcuts for find button
        logTextArea.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.isControlDown() && event.getCode() == KeyCode.F) {
                findToggleButton.setSelected(true);
                event.consume();
            }
            else if (event.getCode() == KeyCode.ESCAPE) {
                findToggleButton.setSelected(false);
                event.consume();
            }
        });

        // Setup resize of pane by dragging pane bar
        AtomicReference<Double> mouseLocationY = new AtomicReference<>();

        paneBar.setOnMousePressed(event -> {
            mouseLocationY.set(event.getSceneY());
        });

        paneBar.setOnMouseDragged(event -> {
            double y = event.getSceneY();

            // Make sure pane cannot be resized out of window
            if (y >= 0 && y <= this.getScene().getHeight() - this.getMinHeight()) {
                double deltaY = y - mouseLocationY.get();
                double newHeight = this.getPrefHeight() - deltaY;
                this.setPrefHeight(newHeight);
                this.getParent().requestLayout();

                mouseLocationY.set(y);
            }
        });

        logTextArea.appendText(
            "Connected to the target VM, address: '127.0.0.1:51973', transport: 'socket'\n" +
                "nov 04, 2018 1:50:39 PM javafx.fxml.FXMLLoader$ValueElement processValue\n" +
                "WARNING: Loading FXML document with JavaFX API of version 8.0.171 by JavaFX "
                + "runtime of version 8.0.121\n" + "nov 04, 2018 1:50:43 PM javafx.fxml"
                + ".FXMLLoader$ValueElement processValue\n" + "WARNING: Loading FXML document "
                + "with JavaFX API of version 8.0.171 by JavaFX runtime of version 8.0.121\n" +
                "nov 04, 2018 1:50:50 PM javafx.fxml.FXMLLoader$ValueElement processValue\n" +
                "WARNING: Loading FXML document with JavaFX API of version 8.0.171 by JavaFX "
                + "runtime of version 8.0.121\n" + "nov 04, 2018 1:51:09 PM javafx.fxml"
                + ".FXMLLoader$ValueElement processValue\n" + "WARNING: Loading FXML document "
                + "with JavaFX API of version 8.0.171 by JavaFX runtime of version 8.0"
                + ".121\nConnected to the target VM, address: '127.0.0.1:51973', transport: "
                + "'socket'\n" + "nov 04, 2018 1:50:39 PM javafx.fxml.FXMLLoader$ValueElement "
                + "processValue\n" + "WARNING: Loading FXML document with JavaFX API of version 8"
                + ".0.171 by JavaFX runtime of version 8.0.121\n" + "nov 04, 2018 1:50:43 PM "
                + "javafx.fxml.FXMLLoader$ValueElement processValue\n" + "WARNING: Loading FXML "
                + "document with JavaFX API of version 8.0.171 by JavaFX runtime of version 8.0"
                + ".121\n" + "nov 04, 2018 1:50:50 PM javafx.fxml.FXMLLoader$ValueElement "
                + "processValue\n" + "WARNING: Loading FXML document with JavaFX API of version 8"
                + ".0.171 by JavaFX runtime of version 8.0.121\n" + "nov 04, 2018 1:51:09 PM "
                + "javafx.fxml.FXMLLoader$ValueElement processValue\n" + "WARNING: Loading FXML "
                + "document with JavaFX API of version 8.0.171 by JavaFX runtime of version 8.0"
                + ".121\n");
    }

    @FXML
    private void closePane(ActionEvent event) {
        if (onCloseRequest != null) {
            onCloseRequest.run();
        }
    }

    /**
     * Sets the close request handler.
     * <p>
     * The handler is when the log pane wants to close itself. This should be set by the parent
     * element that can actually close this pane.
     *
     * @param closeRequest
     *     The close request handler.
     */
    public void setOnCloseRequest(@Nullable Runnable closeRequest) {
        this.onCloseRequest = closeRequest;
    }
}
