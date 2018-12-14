package org.brabocoin.brabocoin.util;

import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class DestructibleTest {

    @Test
    void destruct() throws DestructionException {
        Destructible<BigInteger> destructibleDummy = new Destructible<>(
                () -> BigInteger.valueOf(123)
        );

        assertEquals(BigInteger.valueOf(123), destructibleDummy.getReference().get());

        destructibleDummy.destruct();

        assertTrue(destructibleDummy.isDestroyed());
        assertNull(destructibleDummy.getReference().get());
    }

    @Test
    void destructTwice() throws DestructionException {
        Destructible<BigInteger> destructibleDummy = new Destructible<>(
                () -> BigInteger.valueOf(123)
        );

        assertEquals(BigInteger.valueOf(123), destructibleDummy.getReference().get());

        destructibleDummy.destruct();
        destructibleDummy.destruct();

        assertTrue(destructibleDummy.isDestroyed());
        assertNull(destructibleDummy.getReference().get());
    }

    @Test
    void destructHardReferencedObject() {
        Destructible<BigInteger> destructibleDummy = new Destructible<>(
                () -> BigInteger.valueOf(123)
        );

        BigInteger leaked = destructibleDummy.getReference().get();
        assertEquals(BigInteger.valueOf(123), leaked);

        assertThrows(DestructionException.class, destructibleDummy::destruct);
    }
}