package org.brabocoin.brabocoin.wallet.generation;

import org.brabocoin.brabocoin.util.BigIntegerUtil;
import org.brabocoin.brabocoin.util.Destructible;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Objects;

public class SecureRandomKeyGenerator implements KeyGenerator {

    private SecureRandom random = new SecureRandom();

    @Override
    public Destructible<BigInteger> generateKey(BigInteger maxValue) {
        Destructible<BigInteger> generatedKey;
        do {
            generatedKey = new Destructible<>(() -> new BigInteger(maxValue.bitLength(), random));
        }
        while (!BigIntegerUtil.isInRangeExclusive(
            Objects.requireNonNull(generatedKey.getReference().get()),
            BigInteger.ZERO,
            maxValue)
        );

        return generatedKey;
    }
}
