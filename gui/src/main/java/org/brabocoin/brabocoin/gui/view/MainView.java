package org.brabocoin.brabocoin.gui.view;

import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.PreferencesFxEvent;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
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
import org.brabocoin.brabocoin.gui.util.BraboConfigPreferencesFX;
import org.brabocoin.brabocoin.node.state.State;
import org.controlsfx.control.HiddenSidesPane;
import org.controlsfx.control.StatusBar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

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
        /**
         * Clear PreferencesFX storage
         */
        try {
            Preferences preferences = Preferences.userNodeForPackage(BrabocoinGUI.class);
            preferences.clear();
        } catch (BackingStoreException e) {
            // ignored
        }

        BraboConfigPreferencesFX braboConfigPreferencesFX = new BraboConfigPreferencesFX();

        PreferencesFx preferencesFx;
        try {
            preferencesFx = braboConfigPreferencesFX.getConfigPreferences(state.getConfig());
        }
        catch (IllegalConfigMappingException e) {
            return;
        }

        /**
         * IMPORTANT NOTE:
         *
         * PreferencesFx has a bug causing the {@link PreferencesFxEvent.EVENT_PREFERENCES_SAVED}
         * event to be fired even though the dialog cancel button was pressed, see the open issue
         * <a href="https://github.com/dlemmermann/PreferencesFX/issues/13>here</a>
         *
         * However, the {@link PreferencesFxEvent.EVENT_PREFERENCES_NOT_SAVED} is also fired when
         * cancelled, so we can recover the old config after the new config has been saved.
         * Because the relevant methods are private in {@link PreferencesFx} and
         * {@link com.dlsc.preferencesfx.view.PreferencesFxDialog}, this is the best hack we can
         * do until the issue is patched.
         */
        BraboConfig previousDelegate = state.getConfigAdapter().getDelegator();

        preferencesFx.addEventHandler(
            PreferencesFxEvent.EVENT_PREFERENCES_NOT_SAVED,
            event -> {
                state.getConfigAdapter().setDelegator(previousDelegate);
                try {
                    braboConfigPreferencesFX.writeConfig(state.getConfig(), state.getConfigPath());
                }
                catch (IllegalConfigMappingException | IOException e) {
                    // TODO: Alert to user
                }
            }
        );

        preferencesFx.addEventHandler(
            PreferencesFxEvent.EVENT_PREFERENCES_SAVED,
            event -> {
                braboConfigPreferencesFX.updateConfig(state.getConfigAdapter());
                try {
                    braboConfigPreferencesFX.writeConfig(state.getConfig(), state.getConfigPath());
                }
                catch (IllegalConfigMappingException | IOException e) {
                    // TODO: Alert to user
                }
            }
        );

        preferencesFx.show(true);
    }
}
