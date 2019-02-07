package org.brabocoin.brabocoin.mining;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.Hashing;
import org.brabocoin.brabocoin.dal.CompositeReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.dal.BlockInfo;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.validation.Consensus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Test {@link Miner}.
 */
class MinerTest {

    private Consensus consensus;
    private TransactionPool transactionPool;
    private Miner miner;

    @BeforeEach
    void setUp() {
        Random random = new Random();
        BraboConfig defaultConfig = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);

        transactionPool = new TransactionPool(defaultConfig.maxTransactionPoolSize(),defaultConfig.maxOrphanTransactions(), random);
        consensus = new Consensus() {
            @Override
            public @NotNull Hash getTargetValue() {
                return Hashing.digestSHA256(ByteString.copyFromUtf8("easy"));
            }
        };
        miner = new Miner(transactionPool, consensus, random, new CompositeReadonlyUTXOSet(), defaultConfig.networkId());
    }

    @Test
    void mineNewEmptyBlock() {
        IndexedBlock genesis = new IndexedBlock(
            consensus.getGenesisBlock().getHash(),
            new BlockInfo(
                consensus.getGenesisBlock().getPreviousBlockHash(),
                consensus.getGenesisBlock().getMerkleRoot(),
                consensus.getGenesisBlock().getTargetValue(),
                consensus.getGenesisBlock().getNonce(),
                consensus.getGenesisBlock().getBlockHeight(),
                consensus.getGenesisBlock().getTransactions().size(), consensus.getGenesisBlock().getNetworkId(), true,
                0,
                0,
                0,
                0,
                0,
                0,
                false
            )
        );

        Hash address = Hashing.digestRIPEMD160(ByteString.copyFromUtf8("Neerkant"));
        Block block = miner.mineNewBlock(genesis, address);

        assertNotNull(block);
        assertEquals(1, block.getTransactions().size());
        assertEquals(genesis.getHash(), block.getPreviousBlockHash());
        assertEquals(genesis.getBlockInfo().getBlockHeight() + 1, block.getBlockHeight());
        assertTrue(block.getHash().compareTo(block.getTargetValue()) <= 0);
        assertEquals(consensus.getTargetValue(), block.getTargetValue());

        ByteString serialized = ProtoConverter.toProtoBytes(block, BrabocoinProtos.Block.class);

        assertNotNull(serialized);
        assertTrue(serialized.size() <= consensus.getMaxBlockSize());

        // Check coinbase
        assertTrue(block.getTransactions().get(0).isCoinbase());
        assertEquals(address, block.getTransactions().get(0).getOutputs().get(0).getAddress());
        assertEquals(consensus.getBlockReward(), block.getTransactions().get(0).getOutputs().get(0).getAmount());
    }

    @Test
    void mineNewBlockSingleTransaction() {
        IndexedBlock genesis = new IndexedBlock(
            consensus.getGenesisBlock().getHash(),
            new BlockInfo(
                consensus.getGenesisBlock().getPreviousBlockHash(),
                consensus.getGenesisBlock().getMerkleRoot(),
                consensus.getGenesisBlock().getTargetValue(),
                consensus.getGenesisBlock().getNonce(),
                consensus.getGenesisBlock().getBlockHeight(),
                consensus.getGenesisBlock().getTransactions().size(), consensus.getGenesisBlock().getNetworkId(), true,
                0,
                0,
                0,
                0,
                0,
                0,
                false
            )
        );

        Hash address = Hashing.digestRIPEMD160(ByteString.copyFromUtf8("Neerkant"));

        Transaction transaction = Simulation.randomTransaction(5, 5);
        transactionPool.addIndependentTransaction(transaction);

        Block block = miner.mineNewBlock(genesis, address);

        assertNotNull(block);
        assertEquals(2, block.getTransactions().size());
        assertEquals(genesis.getHash(), block.getPreviousBlockHash());
        assertEquals(genesis.getBlockInfo().getBlockHeight() + 1, block.getBlockHeight());
        assertTrue(block.getHash().compareTo(block.getTargetValue()) <= 0);
        assertEquals(consensus.getTargetValue(), block.getTargetValue());

        ByteString serialized = ProtoConverter.toProtoBytes(block, BrabocoinProtos.Block.class);

        assertNotNull(serialized);
        assertTrue(serialized.size() <= consensus.getMaxBlockSize());

        // Check coinbase
        assertTrue(block.getTransactions().get(0).isCoinbase());
        assertEquals(address, block.getTransactions().get(0).getOutputs().get(0).getAddress());
        assertEquals(consensus.getBlockReward(), block.getTransactions().get(0).getOutputs().get(0).getAmount());

        // Check transaction
        assertEquals(transaction.getHash(), block.getTransactions().get(1).getHash());
    }

    @Test
    void mineNewBlockTransactionTooLarge() {
        IndexedBlock genesis = new IndexedBlock(
            consensus.getGenesisBlock().getHash(),
            new BlockInfo(
                consensus.getGenesisBlock().getPreviousBlockHash(),
                consensus.getGenesisBlock().getMerkleRoot(),
                consensus.getGenesisBlock().getTargetValue(),
                consensus.getGenesisBlock().getNonce(),
                consensus.getGenesisBlock().getBlockHeight(),
                consensus.getGenesisBlock().getTransactions().size(), consensus.getGenesisBlock().getNetworkId(), true,
                0,
                0,
                0,
                0,
                0,
                0,
                false
            )
        );

        Hash address = Hashing.digestRIPEMD160(ByteString.copyFromUtf8("Neerkant"));

        Transaction transaction = Simulation.randomTransaction(1, 20000);
        transactionPool.addIndependentTransaction(transaction);

        Block block = miner.mineNewBlock(genesis, address);

        assertNotNull(block);
        assertEquals(1, block.getTransactions().size());
        assertEquals(genesis.getHash(), block.getPreviousBlockHash());
        assertEquals(genesis.getBlockInfo().getBlockHeight() + 1, block.getBlockHeight());
        assertTrue(block.getHash().compareTo(block.getTargetValue()) <= 0);
        assertEquals(consensus.getTargetValue(), block.getTargetValue());

        ByteString serialized = ProtoConverter.toProtoBytes(block, BrabocoinProtos.Block.class);

        assertNotNull(serialized);
        assertTrue(serialized.size() <= consensus.getMaxBlockSize());

        // Check coinbase
        assertTrue(block.getTransactions().get(0).isCoinbase());
        assertEquals(address, block.getTransactions().get(0).getOutputs().get(0).getAddress());
        assertEquals(consensus.getBlockReward(), block.getTransactions().get(0).getOutputs().get(0).getAmount());
    }

    @Test
    void mineNewBlockMaxTransactionsReached() {
        IndexedBlock genesis = new IndexedBlock(
            consensus.getGenesisBlock().getHash(),
            new BlockInfo(
                consensus.getGenesisBlock().getPreviousBlockHash(),
                consensus.getGenesisBlock().getMerkleRoot(),
                consensus.getGenesisBlock().getTargetValue(),
                consensus.getGenesisBlock().getNonce(),
                consensus.getGenesisBlock().getBlockHeight(),
                consensus.getGenesisBlock().getTransactions().size(), consensus.getGenesisBlock().getNetworkId(), true,
                0,
                0,
                0,
                0,
                0,
                0,
                false
            )
        );

        Hash address = Hashing.digestRIPEMD160(ByteString.copyFromUtf8("Neerkant"));

        List<Transaction> transactions = Simulation.repeatedBuilder(() -> Simulation.randomTransaction(1, 50), 20);

        for (Transaction transaction : transactions) {
            transactionPool.addIndependentTransaction(transaction);
        }

        Block block = miner.mineNewBlock(genesis, address);

        assertNotNull(block);
        assertTrue(block.getTransactions().size() < transactions.size());
        assertTrue(block.getTransactions().size() > 2);
        assertEquals(genesis.getHash(), block.getPreviousBlockHash());
        assertEquals(genesis.getBlockInfo().getBlockHeight() + 1, block.getBlockHeight());
        assertTrue(block.getHash().compareTo(block.getTargetValue()) <= 0);
        assertEquals(consensus.getTargetValue(), block.getTargetValue());

        ByteString serialized = ProtoConverter.toProtoBytes(block, BrabocoinProtos.Block.class);

        assertNotNull(serialized);
        assertTrue(serialized.size() <= consensus.getMaxBlockSize());

        // Check coinbase
        assertTrue(block.getTransactions().get(0).isCoinbase());
        assertEquals(address, block.getTransactions().get(0).getOutputs().get(0).getAddress());
        assertEquals(consensus.getBlockReward(), block.getTransactions().get(0).getOutputs().get(0).getAmount());
    }

    @Test
    void stop() {
        consensus = new Consensus() {
            @Override
            public Hash getTargetValue() {
                return new Hash(ByteString.copyFrom(new byte[] { 0x01 }));
            }
        };

        miner = new Miner(transactionPool, consensus, new Random(), new CompositeReadonlyUTXOSet(), 0);

        IndexedBlock genesis = new IndexedBlock(
            consensus.getGenesisBlock().getHash(),
            new BlockInfo(
                consensus.getGenesisBlock().getPreviousBlockHash(),
                consensus.getGenesisBlock().getMerkleRoot(),
                consensus.getGenesisBlock().getTargetValue(),
                consensus.getGenesisBlock().getNonce(),
                consensus.getGenesisBlock().getBlockHeight(),
                consensus.getGenesisBlock().getTransactions().size(), consensus.getGenesisBlock().getNetworkId(), true,
                0,
                0,
                0,
                0,
                0,
                0,
                false
            )
        );

        Hash address = Hashing.digestRIPEMD160(ByteString.copyFromUtf8("Neerkant"));

        AtomicReference<Object> minedBlock = new AtomicReference<>(new Object());
        Thread thread = new Thread(() -> minedBlock.set(miner.mineNewBlock(genesis, address)));

        thread.start();

        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException e) {
            fail("Sleep failed!");
        }

        miner.stop();

        try {
            thread.join();
        }
        catch (InterruptedException e) {
            fail("Thread was interrupted!");
        }

        assertNull(minedBlock.get());
    }

    @Test
    void stopImmediately() {
        consensus = new Consensus() {
            @Override
            public Hash getTargetValue() {
                return new Hash(ByteString.copyFrom(new byte[] { 0x01 }));
            }
        };

        miner = new Miner(transactionPool, consensus, new Random(), new CompositeReadonlyUTXOSet(), 0);

        IndexedBlock genesis = new IndexedBlock(
            consensus.getGenesisBlock().getHash(),
            new BlockInfo(
                consensus.getGenesisBlock().getPreviousBlockHash(),
                consensus.getGenesisBlock().getMerkleRoot(),
                consensus.getGenesisBlock().getTargetValue(),
                consensus.getGenesisBlock().getNonce(),
                consensus.getGenesisBlock().getBlockHeight(),
                consensus.getGenesisBlock().getTransactions().size(), consensus.getGenesisBlock().getNetworkId(), true,
                0,
                0,
                0,
                0,
                0,
                0,
                false
            )
        );

        Hash address = Hashing.digestRIPEMD160(ByteString.copyFromUtf8("Neerkant"));

        AtomicReference<Object> minedBlock = new AtomicReference<>(new Object());
        Thread thread = new Thread(() -> minedBlock.set(miner.mineNewBlock(genesis, address)));

        thread.start();

        await().until(() -> miner.getMiningBlock() != null);
        miner.stop();

        try {
            thread.join();
        }
        catch (InterruptedException e) {
            fail("Thread was interrupted!");
        }

        assertNull(minedBlock.get());
    }
}
