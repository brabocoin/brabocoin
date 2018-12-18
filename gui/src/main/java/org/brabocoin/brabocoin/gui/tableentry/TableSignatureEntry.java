package org.brabocoin.brabocoin.gui.tableentry;

import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.util.ByteUtil;

public class TableSignatureEntry extends Signature {
    private int index;
    public TableSignatureEntry(Signature signature, int index) {
        super(signature.getR(), signature.getS(), signature.getPublicKey());
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public String getRInHex() {
        return getR().toString(16);
    }

    public String getSInHex() {
        return getS().toString(16);
    }

    public String getPublicKeyPoint() {
        return ByteUtil.toHexString(getPublicKey().toCompressed());
    }
}
