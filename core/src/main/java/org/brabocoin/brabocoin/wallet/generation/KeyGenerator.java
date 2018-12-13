package org.brabocoin.brabocoin.wallet.generation;

import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.util.Destructible;

import java.math.BigInteger;

public interface KeyGenerator {

    /**
     * Generates a random {@link BigInteger}.
     *
     * @param maxValue
     *     Max value to be generated.
     * @return A destructible random {@link BigInteger}
     */
    Destructible<BigInteger> generateKey(BigInteger maxValue) throws DestructionException;
}
