package org.brabocoin.brabocoin.gui.view;

import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.PreferencesFxEvent;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.exceptions.IllegalConfigMappingException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.BrabocoinGUI;
import org.brabocoin.brabocoin.gui.NotificationManager;
import org.brabocoin.brabocoin.gui.task.TaskManager;
import org.brabocoin.brabocoin.gui.util.BraboConfigUtil;
import org.brabocoin.brabocoin.node.state.State;
import org.controlsfx.control.HiddenSidesPane;
import org.controlsfx.control.StatusBar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * Main view for the Brabocoin application.
 */
public class MainView extends BorderPane implements BraboControl, Initializable {

    private final @NotNull State state;
    private final Level initLogLevel;

    @FXML private ToggleButton logPaneToggleButton;
    @FXML private HiddenSidesPane sidesPane;
    @FXML private BorderPane viewContainer;

    @FXML private ToggleGroup toggleGroupMainNav;
    @FXML private ToggleButton stateToggleButton;
    @FXML private ToggleButton miningToggleButton;
    @FXML private ToggleButton walletToggleButton;
    @FXML private ToggleButton networkToggleButton;

    @FXML private StatusBar statusBar;

    private @NotNull TaskManager taskManager;

    private Map<Toggle, Node> toggleToViewMap;
    private CurrentStateView currentStateView;
    private MinerView minerView;
    private WalletView walletView;
    private NetworkView networkView;

    private @NotNull NotificationManager notificationManager;

    /**
     * Create the main view.
     *
     * @param state
     *     The application state.
     */
    public MainView(@NotNull State state, @Nullable Level initLogLevel) {
        super();
        this.state = state;
        this.initLogLevel = initLogLevel;

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize task manager
        taskManager = new TaskManager(statusBar);

        // Bind log pane toggle button to the log pane
        ObjectBinding<Side> paneSideBinding = Bindings.createObjectBinding(
            () -> logPaneToggleButton.isSelected() ? Side.BOTTOM : null,
            logPaneToggleButton.selectedProperty()
        );
        sidesPane.pinnedSideProperty().bind(paneSideBinding);

        // Initialize menu view mapping
        currentStateView = new CurrentStateView(state);
        minerView = new MinerView(
            state,
            taskManager
        );

        // Set log pane
        LogPane logPane = new LogPane(initLogLevel, minerView);
        sidesPane.setBottom(logPane);

        // Set log pane close binding
        logPane.setOnCloseRequest(() -> logPaneToggleButton.setSelected(false));

        walletView = new WalletView(state, taskManager);
        networkView = new NetworkView(state, taskManager);

        toggleToViewMap = new HashMap<>();
        toggleToViewMap.put(stateToggleButton, currentStateView);
        toggleToViewMap.put(miningToggleButton, minerView);
        toggleToViewMap.put(walletToggleButton, walletView);
        toggleToViewMap.put(networkToggleButton, networkView);

        toggleGroupMainNav.selectedToggleProperty().addListener((obs, old, selected) -> {
            viewContainer.setCenter(toggleToViewMap.get(selected));
        });

        // Set initial view
        viewContainer.setCenter(currentStateView);

        // Create notification manager
        notificationManager = new NotificationManager(state);
    }

    @FXML
    private void openSettings() {
        PreferencesFx preferencesFx;
        try {
            preferencesFx = BraboConfigUtil.getConfigPreferences(state.getConfig());
        }
        catch (IllegalConfigMappingException e) {
            return;
        }

        preferencesFx.addEventHandler(
            PreferencesFxEvent.EVENT_PREFERENCES_SAVED,
            event -> {
                BraboConfigUtil.updateConfig(state.getConfigAdapter());
                try {
                    BraboConfigUtil.writeConfig(state.getConfig(), state.getConfigPath());
                }
                catch (IllegalConfigMappingException | IOException e) {
                    // TODO: log
                }
            }
        );

        preferencesFx.show();
    }
}
