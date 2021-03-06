package org.brabocoin.brabocoin.gui.converter;

import javafx.util.StringConverter;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.util.ByteUtil;

public class HashStringConverter extends StringConverter<Hash> {

    @Override
    public String toString(Hash object) {
        if (object == null) {
            return "";
        }
        return ByteUtil.toHexString(object.getValue());
    }

    @Override
    public Hash fromString(String string) {
        try {
            return new Hash(ByteUtil.fromHexString(string));
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }
}
