package org.brabocoin.brabocoin.gui.control.table;

import javafx.geometry.Pos;

public class IntegerTableCell<S> extends CopyableTableCell<S, Integer> {

    @Override
    protected void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
        } else {
            setText(item.toString());
        }

        setAlignment(Pos.CENTER_RIGHT);
    }
}
