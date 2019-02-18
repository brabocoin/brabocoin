package org.brabocoin.brabocoin.gui.control.table;

public class StringTableCell<S> extends CopyableTableCell<S, String> {

    @Override
    protected void updateItem(String item, boolean empty) {
        if (empty || item == null) {
            setText(null);
        }
        else {
            setText(item);
        }
    }
}
