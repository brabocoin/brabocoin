package org.brabocoin.brabocoin.mining;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.validation.Consensus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Sten Wessel
 */
class MiningBlockTest {

    private MiningBlock block;

    @BeforeEach
    void setUp() {
        block = new MiningBlock(Simulation.randomHash(),
            Simulation.randomHash(),
            new Hash(ByteString.copyFrom(new BigInteger(
                "100000000000000000000000000000000000000000000000000000000000000",
                16
            ).toByteArray())),
            BigInteger.ZERO,
            0,
            Collections.emptyList()
        );
    }

    @Test
    void mine() {
        MiningBlock block = new MiningBlock(Simulation.randomHash(),
            Simulation.randomHash(),
            new Hash(ByteString.copyFrom(new BigInteger(
                "100000000000000000000000000000000000000000000000000000000000000",
                16
            ).toByteArray())),
            BigInteger.ZERO,
            0,
            Collections.emptyList()
        );

        Block minedBlock = block.mine(new Consensus());

        assertNotNull(minedBlock);
        assertTrue(minedBlock.getHash().compareTo(minedBlock.getTargetValue()) <= 0);
    }

    @Test
    void stop() {
        MiningBlock block = new MiningBlock(Simulation.randomHash(),
            Simulation.randomHash(),
            new Hash(ByteString.copyFrom(new byte[1])),
            BigInteger.ZERO,
            0,
            Collections.emptyList()
        );

        AtomicReference<Block> minedBlock = new AtomicReference<>(block);
        Thread thread = new Thread(() -> {
            minedBlock.set(block.mine(new Consensus()));
        });

        thread.start();
        block.stop();

        try {
            thread.join();
        }
        catch (InterruptedException e) {
            fail("Thread was interrupted!");
        }

        assertNull(minedBlock.get());
        assertTrue(block.isStopped());
    }
}
