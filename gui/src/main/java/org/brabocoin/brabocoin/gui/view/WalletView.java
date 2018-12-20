package org.brabocoin.brabocoin.gui.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.window.TransactionCreationWindow;
import org.brabocoin.brabocoin.node.state.State;

import java.net.URL;
import java.util.ResourceBundle;

public class WalletView extends TabPane implements BraboControl, Initializable {

    private final State state;

    @FXML public Button buttonCreateTransaction;

    public WalletView(State state) {
        super();
        this.state = state;

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    private void createTransaction(ActionEvent event) {
        new TransactionCreationWindow(
            state.getWallet(),
            state.getBlockchain(),
            state.getConsensus()
        ).showAndWait();
    }
}
