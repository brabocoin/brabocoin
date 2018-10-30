package org.brabocoin.brabocoin.services;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.validation.Consensus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.testutil.Simulation.generateNode;
import static org.junit.jupiter.api.Assertions.*;

public class NodeTest {
    private Random random = new Random();
    static BraboConfig defaultConfig = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);

    @BeforeAll
    static void setUp() {
        defaultConfig = new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return "src/test/resources/" + super.blockStoreDirectory();
            }

            @Override
            public String utxoStoreDirectory() {
                return "src/test/resources/" + super.utxoStoreDirectory();
            }

            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        };

        Logger log = Logger.getLogger("org.brabocoin");
        log.setLevel(Level.ALL);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);

        log.addHandler(handler);
    }

    /**
     * <em>Setup:</em>
     * Two nodes: A and B.
     * A has a block X unknown to B.
     * X is a block mined on top of the genesis block.
     * B's blockchain is empty and consists only of the genesis block.
     *
     * <em>Expected result:</em>
     * After announcing the block to B, B should eventually fetch the block from A and put it on the blockchain.
     */
    @Test
    void announceBlockToPeer() throws DatabaseException, IOException, InterruptedException {
        // Create default node A
        Node nodeA = generateNode(8090, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }
        });

        // Create a blockchain with a block mined on top of genesis block.
        Consensus consensus = new Consensus();

        Block newBlock = new Block(consensus.getGenesisBlock().computeHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                ByteString.copyFromUtf8("randomNonce"),
                new Date().getTime(),
                1,
                Simulation.repeatedBuilder(() -> Simulation.randomTransaction(0, 5), 20));
        Hash newBlockHash = newBlock.computeHash();

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        }, consensus, new ArrayList<Block>() {{ add(newBlock); }});

        // Start nodes
        nodeA.start();
        nodeB.start();

        nodeB.environment.announceBlockRequest(nodeB.environment.getBlock(newBlockHash));

        boolean blockFound = false;
        int remainingWaitingTime = 20000;
        int loopWaitintTime = 100;

        while (!blockFound) {
            if (remainingWaitingTime <= 0) {
                break;
            }
            blockFound = nodeA.environment.getTopBlockHeight() >= 1;
            Thread.sleep(loopWaitintTime);
            remainingWaitingTime -= loopWaitintTime;
        }

        assertNotNull(nodeA.environment.getBlock(newBlockHash));
        assertEquals(1, nodeA.environment.getTopBlockHeight());
    }
}
