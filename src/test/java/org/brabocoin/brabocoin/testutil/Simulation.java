package org.brabocoin.brabocoin.testutil;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.dal.*;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.*;
import org.brabocoin.brabocoin.model.dal.BlockInfo;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.processor.BlockProcessor;
import org.brabocoin.brabocoin.processor.PeerProcessor;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.processor.UTXOProcessor;
import org.brabocoin.brabocoin.services.Node;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.validation.BlockValidator;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.TransactionValidator;

import java.util.*;
import java.util.concurrent.Callable;

public class Simulation {
    private static Random RANDOM = new Random();

    public static List<Block> randomBlockChainGenerator(int length) {
        return randomBlockChainGenerator(length, new Hash(ByteUtil.toByteString(0)), 0);
    }

    public static List<Block> randomBlockChainGenerator(int length, Hash previousHash, int startBlockHeight) {
        return randomBlockChainGenerator(length, previousHash, startBlockHeight, 5, 5);
    }

    public static List<Block> randomBlockChainGenerator(int length, Hash previousHash, int startBlockHeight, int inputs, int outputs) {
        List<Block> list = new ArrayList<>();

        for (int i = startBlockHeight; i < length + startBlockHeight; i++) {
            Block block = randomBlock(previousHash, i, inputs, outputs, 30);
            previousHash = block.computeHash();
            list.add(block);
        }

        return list;
    }

    public static List<IndexedBlock> randomIndexedBlockChainGenerator(int length) {
        return randomIndexedBlockChainGenerator(length, new Hash(ByteUtil.toByteString(0)), 0);
    }

    public static List<IndexedBlock> randomIndexedBlockChainGenerator(int length, Hash previousHash, int startBlockHeight) {
        List<IndexedBlock> list = new ArrayList<>();
        long creationTime = new Date().getTime();

        for (int i = startBlockHeight; i < startBlockHeight + length; i++) {
            Block block = randomBlock(
                previousHash,

                i,
                5, 5, 30);

            BlockInfo info = new BlockInfo(
                block.getPreviousBlockHash(),
                block.getMerkleRoot(),
                block.getTargetValue(),
                block.getNonce(),
                block.getTimestamp(),
                block.getBlockHeight(),
                block.getTransactions().size(),
                false,
                0,
                0,
                0,
                -1,
                -1
            );

            previousHash = block.computeHash();
            IndexedBlock indexedBlock = new IndexedBlock(previousHash, info);
            list.add(indexedBlock);
        }

        return list;
    }

    public static Block randomBlock(Hash previousHash, int blockHeight, int transactionInputBound, int transactionOutputBound, int transactionsBound) {
        long creationTime = new Date().getTime();

        return new Block(
                previousHash,
                randomHash(),
                randomHash(),
                randomByteString(),
                creationTime,
                blockHeight,
                repeatedBuilder(() -> randomTransaction(transactionInputBound,transactionOutputBound), transactionsBound));
    }

    public static Transaction randomTransaction(int inputBound, int outputBound) {
        List<Input> inputs = repeatedBuilder(Simulation::randomInput, inputBound);
        List<Output> outputs = repeatedBuilder(Simulation::randomOutput, outputBound);
        return new Transaction(inputs, outputs);
    }

    public static <U> List<U> repeatedBuilder(Callable<U> builder, int bound) {
        List<U> list = new ArrayList<>();
        for (int i = 0; i < bound; i++) {
            try {
                list.add(builder.call());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static Input randomInput() {
        return new Input(new Signature(), randomHash(), RANDOM.nextInt(5));
    }

    public static Output randomOutput() {
        return new Output(randomHash(), RANDOM.nextInt(1000));
    }

    public static Hash randomHash() {
        return new Hash(randomByteString());
    }

    public static ByteString randomByteString() {
        byte[] randomBytes = new byte[64];
        RANDOM.nextBytes(randomBytes);
        return ByteString.copyFrom(randomBytes);
    }

    public static Node generateNode(int port, BraboConfig config) throws DatabaseException {
        return generateNode(port, config, new BlockDatabase(new HashMapDB(), config));
    }

    public static Node generateNode(int port, BraboConfig config, BlockDatabase blockDatabase) throws DatabaseException {
        Consensus consensus = new Consensus();
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase ChainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(ChainUtxoDatabase);
        BlockValidator blockValidator = new BlockValidator();
        PeerProcessor peerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionPool transactionPool = new TransactionPool(config, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, ChainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        return new Node(port, new NodeEnvironment(port,
                blockchain,
                blockProcessor,
                peerProcessor,
                transactionPool,
                transactionProcessor,
                config));
    }

    public static Node generateNodeWithBlocks(int port, BraboConfig config, Consensus consensus, List<Block> blocks) throws DatabaseException {
        return generateNodeAndProcessorWithBlocks(port, config, consensus, blocks).getKey();
    }

    public static Map.Entry<Node, BlockProcessor> generateNodeAndProcessorWithBlocks(int port, BraboConfig config, Consensus consensus, List<Block> blocks) throws DatabaseException {
        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), config);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase ChainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(ChainUtxoDatabase);
        BlockValidator blockValidator = new BlockValidator();
        PeerProcessor peerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionPool transactionPool = new TransactionPool(config, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, ChainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        for (Block b : blocks) {
            blockProcessor.processNewBlock(b);
        }

        return new AbstractMap.SimpleEntry<>(new Node(port, new NodeEnvironment(port,
                blockchain,
                blockProcessor,
                peerProcessor,
                transactionPool,
                transactionProcessor,
                config)), blockProcessor);
    }

    public static Node generateNodeWithTransactions(int port, BraboConfig config, Consensus consensus, List<Transaction> transactions) throws DatabaseException {
        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), config);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase ChainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(ChainUtxoDatabase);
        BlockValidator blockValidator = new BlockValidator();
        PeerProcessor peerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionPool transactionPool = new TransactionPool(config, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, ChainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        for (Transaction t : transactions) {
            transactionProcessor.processNewTransaction(t);
        }

        return new Node(port, new NodeEnvironment(port,
                blockchain,
                blockProcessor,
                peerProcessor,
                transactionPool,
                transactionProcessor,
                config));
    }
}
