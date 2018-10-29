package org.brabocoin.brabocoin.gui.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.brabocoin.brabocoin.gui.BraboControl;

/**
 * Main view for the Brabocoin application.
 */
public class MainView extends BraboControl {

    @FXML
    private Button blockchain;

    @FXML
    private void onBlockchain() {
        System.out.println("Is niet zo spannend...");
    }
}
