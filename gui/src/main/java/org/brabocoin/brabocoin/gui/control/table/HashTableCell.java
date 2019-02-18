package org.brabocoin.brabocoin.gui.control.table;

import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.util.ByteUtil;

/**
 * Table cell that formats a hash to to a hexadecimal string.
 */
public class HashTableCell<T> extends CopyableTableCell<T, Hash> {

    private final int size;

    public HashTableCell(int size) {
        super();
        this.size = size;
        getStyleClass().add("hash");
    }

    @Override
    protected void updateItem(Hash item, boolean empty) {
        if (empty) {
            setText(null);
        }
        else {
            setText(ByteUtil.toHexString(item.getValue(), size));
        }
    }
}
