package org.brabocoin.brabocoin.gui.control.table;

/**
 * Table cell that shows the row index in the table.
 */
public class NumberedTableCell<T> extends CopyableTableCell<T, Integer> {

    public NumberedTableCell() {
        super();
    }

    @Override
    protected void updateItem(Integer item, boolean empty) {
        if (empty) {
            setText(null);
        }
        else {
            setText(String.valueOf(getIndex()));
        }
    }

}
