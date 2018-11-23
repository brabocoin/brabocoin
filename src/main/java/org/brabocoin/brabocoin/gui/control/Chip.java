package org.brabocoin.brabocoin.gui.control;

import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 * A label that this styled as a chip.
 */
public class Chip extends Label {

    public Chip() {
        super();
        initialize();
    }

    public Chip(String text) {
        super(text);
        initialize();
    }

    public Chip(String text, Node graphic) {
        super(text, graphic);
        initialize();
    }

    private void initialize() {
        getStyleClass().add("chip");
    }
}
