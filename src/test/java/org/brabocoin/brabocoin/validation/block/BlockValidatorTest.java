package org.brabocoin.brabocoin.validation.block;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.MerkleTree;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.*;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.processor.UTXOProcessor;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.util.BigIntegerUtil;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.block.rules.NonContextualTransactionCheckBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.ValidBlockHeightBlkRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.brabocoin.brabocoin.validation.transaction.rules.OutputCountTxRule;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockValidatorTest {
    static BraboConfig defaultConfig = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);
    Consensus consensus = new Consensus();

    private static Signer signer;

    private static final EllipticCurve CURVE = EllipticCurve.secp256k1();

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
        };

        signer = new Signer(CURVE);
    }

    @Test
    void checkBlockValidOrphan() throws DatabaseException {
        Consensus consensus = new Consensus() {
            @Override
            public @NotNull Hash getTargetValue() {
                return new Hash(ByteString.copyFrom(BigIntegerUtil.getMaxBigInteger(33).toByteArray()));
            }
        };
        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );

        List<Transaction> transactionList = Collections.singletonList(
                new Transaction(Collections.emptyList(),
                        Collections.singletonList(
                                new Output(Simulation.randomHash(), consensus.getBlockReward())
                        ))
        );

        Hash merkleRoot = new MerkleTree(consensus.getMerkleTreeHashFunction(),
                transactionList.stream().map(Transaction::getHash).collect(Collectors.toList())).getRoot();

        Block block = new Block(
                Simulation.randomHash(),
                merkleRoot,
                consensus.getTargetValue(),
                BigInteger.ZERO,
                1,
                transactionList
        );

        BlockValidationResult result = blockValidator.checkIncomingBlockValid(
                block
        );

        assertEquals(ValidationStatus.ORPHAN, result.getStatus());
    }

    @Test
    void checkBlockValidInvalidBlockHeight() throws DatabaseException {
        Consensus consensus = new Consensus() {
            @Override
            public @NotNull Hash getTargetValue() {
                return new Hash(ByteString.copyFrom(BigIntegerUtil.getMaxBigInteger(33).toByteArray()));
            }
        };
        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );

        List<Transaction> transactionList = Collections.singletonList(
                new Transaction(Collections.emptyList(),
                        Collections.singletonList(
                                new Output(Simulation.randomHash(), consensus.getBlockReward())
                        ))
        );

        Hash merkleRoot = new MerkleTree(consensus.getMerkleTreeHashFunction(),
                transactionList.stream().map(Transaction::getHash).collect(Collectors.toList())).getRoot();

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                merkleRoot,
                consensus.getTargetValue(),
                BigInteger.ZERO,
                2,
                transactionList
        );

        BlockValidationResult result = blockValidator.checkIncomingBlockValid(
                block
        );

        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertEquals(ValidBlockHeightBlkRule.class, result.getFailMarker().getFailedRule());
    }

    @Test
    void checkBlockValidInvalidTransactionEmpty() throws DatabaseException {
        Consensus consensus = new Consensus() {
            @Override
            public @NotNull Hash getTargetValue() {
                return new Hash(ByteString.copyFrom(BigIntegerUtil.getMaxBigInteger(33).toByteArray()));
            }
        };
        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );

        List<Transaction> transactionList = Arrays.asList(
                new Transaction(Collections.emptyList(),
                        Collections.singletonList(
                                new Output(Simulation.randomHash(), consensus.getBlockReward())
                        )),
                new Transaction(Collections.emptyList(), Collections.emptyList())
        );

        Hash merkleRoot = new MerkleTree(consensus.getMerkleTreeHashFunction(),
                transactionList.stream().map(Transaction::getHash).collect(Collectors.toList())).getRoot();

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                merkleRoot,
                consensus.getTargetValue(),
                BigInteger.ZERO,
                2,
                transactionList
        );

        BlockValidationResult result = blockValidator.checkIncomingBlockValid(
                block
        );

        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertEquals(NonContextualTransactionCheckBlkRule.class, result.getFailMarker().getFailedRule());
        assertTrue(result.getFailMarker().hasChild());
        assertEquals(OutputCountTxRule.class, result.getFailMarker().getChild().getFailedRule());
    }

    @Test
    void checkBlockValidValid() throws DatabaseException {
        Consensus consensus = new Consensus() {
            @Override
            public @NotNull Hash getTargetValue() {
                return new Hash(ByteString.copyFrom(BigIntegerUtil.getMaxBigInteger(33).toByteArray()));
            }
        };
        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );

        List<Transaction> transactionList = Collections.singletonList(
                new Transaction(Collections.emptyList(),
                        Collections.singletonList(
                                new Output(Simulation.randomHash(), consensus.getBlockReward())
                        ))
        );

        Hash merkleRoot = new MerkleTree(consensus.getMerkleTreeHashFunction(),
                transactionList.stream().map(Transaction::getHash).collect(Collectors.toList())).getRoot();

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                merkleRoot,
                consensus.getTargetValue(),
                BigInteger.ZERO,
                1,
                transactionList
        );

        BlockValidationResult result = blockValidator.checkIncomingBlockValid(
                block
        );

        assertEquals(ValidationStatus.VALID, result.getStatus());

        result = blockValidator.checkConnectBlockValid(
                block
        );

        assertEquals(ValidationStatus.VALID, result.getStatus());
    }
}