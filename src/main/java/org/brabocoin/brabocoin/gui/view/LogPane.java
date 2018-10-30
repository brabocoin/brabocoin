package org.brabocoin.brabocoin.gui.view;

import javafx.scene.layout.AnchorPane;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;

/**
 * @author Sten Wessel
 */
public class LogPane extends AnchorPane implements BraboControl {

    public LogPane() {
        BraboControlInitializer.initialize(this);
    }

}
