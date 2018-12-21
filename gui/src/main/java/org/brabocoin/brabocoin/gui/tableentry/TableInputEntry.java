package org.brabocoin.brabocoin.gui.tableentry;

import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.util.ByteUtil;

public class TableInputEntry extends Input {
    private int index;
    public TableInputEntry(Input input, int index) {
        super(input.getReferencedTransaction(), input.getReferencedOutputIndex());
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getReferencedTransactionHash() {
        return ByteUtil.toHexString(this.getReferencedTransaction().getValue(), 32);
    }
}
