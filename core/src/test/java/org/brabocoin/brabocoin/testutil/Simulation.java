package org.brabocoin.brabocoin.testutil;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.MerkleTree;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.mining.MiningBlock;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Signature;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.dal.BlockInfo;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.validation.Consensus;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class Simulation {
    private static Random RANDOM = new Random();
    private static EllipticCurve CURVE = EllipticCurve.secp256k1();

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
            previousHash = block.getHash();
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
                    block.getNonce(), block.getBlockHeight(),
                    block.getTransactions().size(),
                    false,
                    0,
                    0,
                    0,
                    0,
                    -1,
                    -1
            );

            previousHash = block.getHash();
            IndexedBlock indexedBlock = new IndexedBlock(previousHash, info);
            list.add(indexedBlock);
        }

        return list;
    }

    public static Block randomBlock(Hash previousHash, int blockHeight, int transactionInputBound, int transactionOutputBound, int transactionsBound) {
        return new Block(
                previousHash,
                randomHash(),
                randomHash(),
                randomBigInteger(), blockHeight,
                repeatedBuilder(() -> randomTransaction(transactionInputBound, transactionOutputBound), transactionsBound));
    }

    public static Block randomOrphanBlock(Consensus consensus, int blockHeight, int transactionInputBound, int transactionOutputBound, int transactionsBound) {
        List<Transaction> transactions =
                Collections.singletonList(
                        new Transaction(
                                Collections.emptyList(),
                                Collections.singletonList(
                                        new Output(randomHash(), consensus.getBlockReward())
                                )
                        )
                );

        return new MiningBlock(
                randomHash(),
                new MerkleTree(
                        consensus.getMerkleTreeHashFunction(),
                        transactions.stream().map(Transaction::getHash).collect(Collectors.toList())
                ).getRoot(),
                consensus.getTargetValue(),
                new BigInteger(consensus.getMaxNonceSize() * 8, RANDOM),
                blockHeight,
                transactions
        ).mine(consensus);
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
        return new Input(randomSignature(), randomHash(), RANDOM.nextInt(5));
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

    public static Signature randomSignature() {
        PublicKey publicKey = CURVE.getPublicKeyFromPrivateKey(new BigInteger(255, RANDOM));
        return new Signature(new BigInteger(255, RANDOM), new BigInteger(255, RANDOM), publicKey);
    }

//    public static Node generateNode(int port, BraboConfig config) throws DatabaseException {
//        return generateNode(port, config, new BlockDatabase(new HashMapDB(), config));
//    }
//
//    public static Node generateNode(int port, BraboConfig config, BlockDatabase blockDatabase) throws DatabaseException {
//        Consensus consensus = new Consensus();
//        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
//        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
//        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
//        PeerProcessor peerProcessor = new PeerProcessor(new HashSet<>(), config);
//        TransactionPool transactionPool = new TransactionPool(config, new Random());
//        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
//        Signer signer = new Signer(EllipticCurve.secp256k1());
//        TransactionValidator transactionValidator = new TransactionValidator(
//                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
//        );
//        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
//                transactionPool, chainUtxoDatabase, poolUtxo);
//        BlockValidator blockValidator = new BlockValidator(
//                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
//        );
//        BlockProcessor blockProcessor = new BlockProcessor(
//                blockchain,
//                utxoProcessor,
//                transactionProcessor,
//                consensus,
//                blockValidator);
//
//        return new Node(new NodeEnvironment(port,
//                blockchain,
//                chainUtxoDatabase,
//                blockProcessor,
//                peerProcessor,
//                transactionPool,
//                transactionProcessor,
//                config));
//    }

//    public static Node generateNodeWithBlocks(int port, BraboConfig config, Consensus consensus, List<Block> blocks) throws DatabaseException {
//        return generateNodeAndProcessorWithBlocks(port, config, consensus, blocks).getKey();
//    }
//
//    public static Map.Entry<Node, BlockProcessor> generateNodeAndProcessorWithBlocks(int port, BraboConfig config, Consensus consensus, Iterable<Block> blocks) throws DatabaseException {
//        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), config);
//        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
//        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
//        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
//        PeerProcessor peerProcessor = new PeerProcessor(new HashSet<>(), config);
//        TransactionPool transactionPool = new TransactionPool(config, new Random());
//        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
//        Signer signer = new Signer(EllipticCurve.secp256k1());
//        TransactionValidator transactionValidator = new TransactionValidator(
//                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
//        );
//        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
//                transactionPool, chainUtxoDatabase, poolUtxo);
//        BlockValidator blockValidator = new BlockValidator(
//                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
//        );
//        BlockProcessor blockProcessor = new BlockProcessor(
//                blockchain,
//                utxoProcessor,
//                transactionProcessor,
//                consensus,
//                blockValidator);
//
//        for (Block b : blocks) {
//            blockProcessor.processNewBlock(b);
//        }
//
//        return new AbstractMap.SimpleEntry<>(new Node(new NodeEnvironment(port,
//                blockchain,
//                chainUtxoDatabase,
//                blockProcessor,
//                peerProcessor,
//                transactionPool,
//                transactionProcessor,
//                config)), blockProcessor);
//    }

//    public static Node generateNodeWithTransactions(int port, BraboConfig config, Consensus consensus, Iterable<Transaction> transactions) throws DatabaseException {
//        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), config);
//        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
//        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
//        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
//        PeerProcessor peerProcessor = new PeerProcessor(new HashSet<>(), config);
//        TransactionPool transactionPool = new TransactionPool(config, new Random());
//        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
//        Signer signer = new Signer(EllipticCurve.secp256k1());
//        TransactionValidator transactionValidator = new TransactionValidator(
//                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
//        );
//        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
//                transactionPool, chainUtxoDatabase, poolUtxo);
//        BlockValidator blockValidator = new BlockValidator(
//                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
//        );
//        BlockProcessor blockProcessor = new BlockProcessor(
//                blockchain,
//                utxoProcessor,
//                transactionProcessor,
//                consensus,
//                blockValidator);
//
//        for (Transaction t : transactions) {
//            transactionProcessor.processNewTransaction(t);
//        }
//
//        return new Node(new NodeEnvironment(port,
//                blockchain,
//                chainUtxoDatabase,
//                blockProcessor,
//                peerProcessor,
//                transactionPool,
//                transactionProcessor,
//                config));
//    }

    public static BigInteger randomBigInteger() {
        return BigInteger.valueOf(RANDOM.nextInt());
    }
}