package org.brabocoin.brabocoin.config;

import com.github.drapostolos.typeparser.Parser;
import com.github.drapostolos.typeparser.ParserHelper;
import com.google.protobuf.ByteString;
import org.bouncycastle.util.encoders.Hex;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.util.ByteUtil;

import java.math.BigDecimal;

/**
 * Parses the target value from a decimal string or hexadecimal hash string.
 */
public class TargetValueParser implements Parser<Hash> {

    @Override
    public Hash parse(String input, ParserHelper helper) {
        if (input.startsWith("0")) {
            return parseHash(input);
        }

        return parseBigDecimal(input);
    }

    private Hash parseHash(String input) {
        return new Hash(ByteString.copyFrom(Hex.decode(input)));
    }

    private Hash parseBigDecimal(String input) {
        return new Hash(ByteUtil.toUnsigned(new BigDecimal(input).toBigInteger()));
    }
}
