package org.brabocoin.brabocoin.testutil;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.model.dal.BlockInfo;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.services.Node;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.validation.consensus.Consensus;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class Simulation {

    private static Random RANDOM = new Random();
    private static EllipticCurve CURVE = EllipticCurve.secp256k1();

    public static List<Block> randomBlockChainGenerator(int length) {
        return randomBlockChainGenerator(length, new Hash(ByteUtil.toByteString(0)), 0);
    }

    public static List<Block> randomBlockChainGenerator(int length, Hash previousHash,
                                                        int startBlockHeight) {
        return randomBlockChainGenerator(length, previousHash, startBlockHeight, 5, 5, false);
    }

    public static List<Block> randomBlockChainGenerator(int length, Hash previousHash,
                                                        int startBlockHeight, boolean withCoinbases) {
        return randomBlockChainGenerator(length, previousHash, startBlockHeight, 5, 5, withCoinbases);
    }

    public static List<Block> randomBlockChainGenerator(int length, Hash previousHash,
                                                        int startBlockHeight, int inputs,
                                                        int outputs, boolean withCoinbases) {
        List<Block> list = new ArrayList<>();

        for (int i = startBlockHeight; i < length + startBlockHeight; i++) {
            Block block = randomBlock(previousHash, i, inputs, outputs, 30, withCoinbases);
            previousHash = block.getHash();
            list.add(block);
        }

        return list;
    }

    public static List<IndexedBlock> randomIndexedBlockChainGenerator(int length) {
        return randomIndexedBlockChainGenerator(length, new Hash(ByteUtil.toByteString(0)), 0);
    }

    public static List<IndexedBlock> randomIndexedBlockChainGenerator(int length, Hash previousHash,
                                                                      int startBlockHeight) {
        List<IndexedBlock> list = new ArrayList<>();
        long creationTime = new Date().getTime();

        for (int i = startBlockHeight; i < startBlockHeight + length; i++) {
            Block block = randomBlock(
                previousHash,

                i,
                5, 5, 30
            );

            BlockInfo info = new BlockInfo(
                block.getPreviousBlockHash(),
                block.getMerkleRoot(),
                block.getTargetValue(),
                block.getNonce(), block.getBlockHeight(),
                block.getTransactions().size(), 1, true,
                0,
                0,
                0,
                0,
                -1,
                -1,
                false
            );

            previousHash = block.getHash();
            IndexedBlock indexedBlock = new IndexedBlock(previousHash, info);
            list.add(indexedBlock);
        }

        return list;
    }

    public static Block randomBlock(Hash previousHash, int blockHeight, int transactionInputBound,
                                    int transactionOutputBound, int transactionsBound) {
        return randomBlock(previousHash, blockHeight, transactionInputBound, transactionOutputBound, transactionsBound, false);
    }

    public static Block randomBlock(Hash previousHash, int blockHeight, int transactionInputBound,
                                    int transactionOutputBound, int transactionsBound, boolean withExtraCoinbase) {
        List<Transaction> transactions = new ArrayList<>();

        if (withExtraCoinbase) {
            transactions.add(Transaction.coinbase(Simulation.randomOutput(), blockHeight));
        }

        transactions.addAll(
            repeatedBuilder(
                () -> randomTransaction(transactionInputBound, transactionOutputBound),
                transactionsBound
            )
        );

        return new Block(
            previousHash,
            randomHash(),
            randomHash(),
            randomBigInteger(),
            blockHeight,
            transactions,
            0
        );
    }

    public static Transaction randomTransaction(int inputBound, int outputBound) {
        List<Input> inputs = repeatedBuilder(Simulation::randomInput, inputBound);
        List<Output> outputs = repeatedBuilder(Simulation::randomOutput, outputBound);
        List<Signature> signatures = repeatedBuilder(Simulation::randomSignature, inputBound);
        return new Transaction(inputs, outputs, signatures);
    }

    public static <U> List<U> repeatedBuilder(Callable<U> builder, int bound) {
        List<U> list = new ArrayList<>();
        for (int i = 0; i < bound; i++) {
            try {
                list.add(builder.call());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static Input randomInput() {
        return new Input(randomHash(), RANDOM.nextInt(5));
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

    public static Node generateNode(int port, MockLegacyConfig config) throws DatabaseException {
        return generateNode(
            port,
            config,
            new BlockDatabase(new HashMapDB(),
                new File(config.blockStoreDirectory()),
                config.maxBlockFileSize()
            )
        );
    }

    public static Node generateNode(int port, MockLegacyConfig config,
                                    BlockDatabase providedBlockDatabase) throws DatabaseException {
        MockLegacyConfig mockConfig = new MockLegacyConfig(config) {
            @Override
            public Integer servicePort() {
                return port;
            }
        };

        State state = new TestState(mockConfig) {
            @Override
            protected BlockDatabase createBlockDatabase() {
                return providedBlockDatabase;
            }
        };

        return state.getNode();
    }

    public static BigInteger randomBigInteger() {
        return BigInteger.valueOf(RANDOM.nextInt());
    }

    public static BigInteger randomPrivateKey() {
        return BigInteger.valueOf(RANDOM.nextInt()).mod(new Consensus().getCurve().getDomain().getN());
    }
}
