package org.brabocoin.brabocoin.gui.view;

import javafx.scene.layout.BorderPane;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;

/**
 * Main view for the Brabocoin application.
 */
public class MainView extends BorderPane implements BraboControl {

    public MainView() {
        BraboControlInitializer.initialize(this);
    }

//    @FXML
//    private Button blockchain;
//
//    @FXML
//    private void onBlockchain() {
//        System.out.println("Is niet zo spannend...");
//    }
}
