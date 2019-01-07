package org.brabocoin.brabocoin.gui.converter;

import javafx.util.StringConverter;

import java.math.BigInteger;

public class HexBigIntegerStringConverter extends StringConverter<BigInteger> {

    public static final int RADIX = 16;

    @Override
    public String toString(BigInteger value) {
        return value.toString(RADIX).toUpperCase();
    }

    @Override
    public BigInteger fromString(String value) {
        return new BigInteger(value, RADIX);
    }
}
