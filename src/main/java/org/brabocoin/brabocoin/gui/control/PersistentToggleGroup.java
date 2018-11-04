package org.brabocoin.brabocoin.gui.control;

import javafx.scene.control.ToggleGroup;

/**
 * Toggle group that always maintains a selected toggle.
 */
public class PersistentToggleGroup extends ToggleGroup {

    /**
     * Create a new persistent toggle group.
     */
    public PersistentToggleGroup() {
        super();

        selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            }
        });
    }
}
