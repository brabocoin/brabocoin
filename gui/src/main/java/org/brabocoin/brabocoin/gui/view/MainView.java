package org.brabocoin.brabocoin.gui.view;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.NotificationManager;
import org.brabocoin.brabocoin.gui.task.TaskManager;
import org.brabocoin.brabocoin.node.state.State;
import org.controlsfx.control.HiddenSidesPane;
import org.controlsfx.control.StatusBar;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Main view for the Brabocoin application.
 */
public class MainView extends BorderPane implements BraboControl, Initializable {

    private final @NotNull State state;

    @FXML private ToggleButton logPaneToggleButton;
    @FXML private HiddenSidesPane sidesPane;
    @FXML private BorderPane viewContainer;

    @FXML private ToggleGroup toggleGroupMainNav;
    @FXML private ToggleButton stateToggleButton;
    @FXML private ToggleButton miningToggleButton;
    @FXML private ToggleButton walletToggleButton;
    @FXML private ToggleButton networkToggleButton;

    @FXML private StatusBar statusBar;
    @FXML private LogPane logPane;

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
    public MainView(@NotNull State state) {
        super();
        this.state = state;

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

        // Set log pane close binding
        logPane.setOnCloseRequest(() -> logPaneToggleButton.setSelected(false));

        // Initialize menu view mapping
        currentStateView = new CurrentStateView(state);
        minerView = new MinerView(
            state,
            taskManager
        );

        walletView = new WalletView(state);
        networkView = new NetworkView(state);

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
}
