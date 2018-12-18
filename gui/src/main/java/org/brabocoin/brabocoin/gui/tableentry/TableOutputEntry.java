package org.brabocoin.brabocoin.gui.tableentry;

import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.util.ByteUtil;

public class TableOutputEntry extends Output {

    private int index;

    public TableOutputEntry(Output output, int index) {
        super(output.getAddress(), output.getAmount());
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public String getAddressHash() {
        return ByteUtil.toHexString(this.getAddress().getValue(), 32);
    }
}