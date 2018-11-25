package org.brabocoin.brabocoin.gui.view;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.node.state.State;
import org.controlsfx.control.HiddenSidesPane;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main view for the Brabocoin application.
 */
public class MainView extends BorderPane implements BraboControl, Initializable {

    private final @NotNull State state;

    @FXML private ToggleButton logPaneToggleButton;
    @FXML private HiddenSidesPane sidesPane;
    @FXML private BorderPane viewContainer;

    @FXML private ToggleButton stateToggleButton;

    @FXML private LogPane logPane;

    private CurrentStateView currentStateView;

    /**
     * Create the main view.
     *
     * @param state The application state.
     */
    public MainView(@NotNull State state) {
        super();
        this.state = state;

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind log pane toggle button to the log pane
        ObjectBinding<Side> paneSideBinding = Bindings.createObjectBinding(
            () -> logPaneToggleButton.isSelected() ? Side.BOTTOM : null,
            logPaneToggleButton.selectedProperty()
        );
        sidesPane.pinnedSideProperty().bind(paneSideBinding);

        // Set log pane close binding
        logPane.setOnCloseRequest(() -> logPaneToggleButton.setSelected(false));

        // Initialize menu views
        currentStateView = new CurrentStateView(state.getBlockchain());

        // Bind main nav buttons to views
        stateToggleButton.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) {
                viewContainer.setCenter(currentStateView);
            }
        });

        // Set initial view
        viewContainer.setCenter(currentStateView);
    }
}
