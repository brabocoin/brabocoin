package org.brabocoin.brabocoin.gui.converter;

import com.google.protobuf.ByteString;
import javafx.util.StringConverter;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.model.Hash;

public class Base58StringConverter extends StringConverter<Hash> {

    @Override
    public String toString(Hash object) {
        if (object == null || object.getValue().equals(ByteString.EMPTY)) {
            return "";
        }
        return PublicKey.getBase58AddressFromHash(object);
    }

    @Override
    public Hash fromString(String string) {
        try {
            return PublicKey.getHashFromBase58Address(string);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
