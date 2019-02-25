package org.brabocoin.brabocoin.gui.control;

import javafx.scene.control.Skin;
import javafx.scene.control.TableView;

/**
 * @author Sten Wessel
 */
public class AutoResizeTableView<S> extends TableView<S> {

    @Override
    protected Skin<?> createDefaultSkin() {
        return new AutoResizeTableSkin<>(this);
    }
}
