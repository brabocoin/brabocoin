package org.brabocoin.brabocoin.gui.converter;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.util.Base58Check;

public class Base58StringConverter extends javafx.util.StringConverter<Hash> {

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
            return new Hash(Base58Check.decode(string));
        } catch (IllegalArgumentException e) {
            return new Hash(ByteString.EMPTY);
        }
    }
}
