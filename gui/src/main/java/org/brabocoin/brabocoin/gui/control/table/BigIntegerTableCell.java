package org.brabocoin.brabocoin.gui.control.table;

import java.math.BigInteger;

/**
 * Table cell that formats a hash to to a hexadecimal string.
 */
public class BigIntegerTableCell<T> extends CopyableTableCell<T, BigInteger> {

    private final int radix;

    public BigIntegerTableCell(int radix) {
        super();
        this.radix = radix;
        getStyleClass().add("hash");
    }

    @Override
    protected void updateItem(BigInteger item, boolean empty) {
        if (empty) {
            setText(null);
        }
        else {
            setText(item.toString(radix));
        }
    }

}
