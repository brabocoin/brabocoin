package org.brabocoin.brabocoin.wallet.generation;

import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.util.Destructible;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Objects;

public class SecureRandomKeyGenerator implements KeyGenerator {

    private SecureRandom random = new SecureRandom();

    @Override
    public Destructible<BigInteger> generateKey(BigInteger maxValue) throws DestructionException {
        Destructible<byte[]> randomBytes =
            new Destructible<>(() -> new byte[maxValue.toByteArray().length]);
        random.nextBytes(randomBytes.getReference().get());

        Destructible<BigInteger> randomBigInteger = new Destructible<>(() ->
            new BigInteger(
                Objects.requireNonNull(randomBytes.getReference().get())
            ).mod(maxValue)
        );

        randomBytes.destruct();

        return randomBigInteger;
    }
}
