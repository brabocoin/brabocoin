package org.brabocoin.brabocoin.validation.block;

import org.brabocoin.brabocoin.config.MutableBraboConfig;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.MerkleTree;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.testutil.LegacyBraboConfig;
import org.brabocoin.brabocoin.testutil.MockLegacyConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.testutil.TestState;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.block.rules.NonContextualTransactionCheckBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.ValidBlockHeightBlkRule;
import org.brabocoin.brabocoin.validation.transaction.rules.InputOutputNotEmptyTxRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockValidatorTest {

    static MockLegacyConfig defaultConfig =
        new MockLegacyConfig(new LegacyBraboConfig(new MutableBraboConfig()));
    private static Signer signer;

    private static final EllipticCurve CURVE = EllipticCurve.secp256k1();

    @BeforeAll
    static void setUp() {
        defaultConfig = new MockLegacyConfig(defaultConfig);

        signer = new Signer(CURVE);
    }

    @Test
    void checkBlockValidOrphan() throws DatabaseException {
        State state = new TestState(defaultConfig);

        List<Transaction> transactionList = Collections.singletonList(
            Transaction.coinbase(new Output(
                Simulation.randomHash(),
                state.getConsensus().getBlockReward()
            ), 1)
        );

        Hash merkleRoot = new MerkleTree(
            state.getConsensus().getMerkleTreeHashFunction(),
            transactionList.stream().map(Transaction::getHash).collect(Collectors.toList())
        ).getRoot();

        Block block = new Block(
            Simulation.randomHash(),
            merkleRoot,
            state.getConsensus().getTargetValue(),
            BigInteger.ZERO,
            1,
            transactionList,
            state.getConfig().getNetworkId()
        );

        BlockValidationResult result = state.getBlockValidator().validate(
            block, BlockValidator.INCOMING_BLOCK
        );

        assertEquals(ValidationStatus.ORPHAN, result.getStatus());
    }

    @Test
    void checkBlockValidInvalidBlockHeight() throws DatabaseException {
        State state = new TestState(defaultConfig);

        List<Transaction> transactionList = Collections.singletonList(
            Transaction.coinbase(new Output(
                Simulation.randomHash(),
                state.getConsensus().getBlockReward()
            ), 2)
        );

        Hash merkleRoot = new MerkleTree(
            state.getConsensus().getMerkleTreeHashFunction(),
            transactionList.stream().map(Transaction::getHash).collect(Collectors.toList())
        ).getRoot();

        Block block = new Block(
            state.getConsensus().getGenesisBlock().getHash(),
            merkleRoot,
            state.getConsensus().getTargetValue(),
            BigInteger.ZERO,
            2,
            transactionList,
            state.getConfig().getNetworkId()
        );

        BlockValidationResult result = state.getBlockValidator().validate(
            block, BlockValidator.INCOMING_BLOCK
        );

        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertEquals(ValidBlockHeightBlkRule.class, result.getFailMarker().getFailedRule());
    }

    @Test
    void checkBlockValidInvalidTransactionEmpty() throws DatabaseException {
        State state = new TestState(defaultConfig);

        List<Transaction> transactionList = Arrays.asList(
            Transaction.coinbase(new Output(
                Simulation.randomHash(),
                state.getConsensus().getBlockReward()
            ), 2),
            new Transaction(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            )
        );

        Hash merkleRoot = new MerkleTree(
            state.getConsensus().getMerkleTreeHashFunction(),
            transactionList.stream().map(Transaction::getHash).collect(Collectors.toList())
        ).getRoot();

        Block block = new Block(
            state.getConsensus().getGenesisBlock().getHash(),
            merkleRoot,
            state.getConsensus().getTargetValue(),
            BigInteger.ZERO,
            2,
            transactionList,
            state.getConfig().getNetworkId()
        );

        BlockValidationResult result = state.getBlockValidator().validate(
            block, BlockValidator.INCOMING_BLOCK
        );

        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertEquals(
            NonContextualTransactionCheckBlkRule.class,
            result.getFailMarker().getFailedRule()
        );
        assertTrue(result.getFailMarker().hasChild());
        assertEquals(
            InputOutputNotEmptyTxRule.class,
            result.getFailMarker().getChild().getFailedRule()
        );
    }

    @Test
    void checkBlockValidValid() throws DatabaseException {
        State state = new TestState(defaultConfig);

        List<Transaction> transactionList = Collections.singletonList(
            Transaction.coinbase(new Output(
                Simulation.randomHash(),
                state.getConsensus().getBlockReward()
            ), 1)
        );

        Hash merkleRoot = new MerkleTree(
            state.getConsensus().getMerkleTreeHashFunction(),
            transactionList.stream().map(Transaction::getHash).collect(Collectors.toList())
        ).getRoot();

        Block block = new Block(
            state.getConsensus().getGenesisBlock().getHash(),
            merkleRoot,
            state.getConsensus().getTargetValue(),
            BigInteger.ZERO,
            1,
            transactionList,
            state.getConfig().getNetworkId()
        );

        BlockValidationResult result = state.getBlockValidator().validate(
            block, BlockValidator.INCOMING_BLOCK
        );

        assertEquals(ValidationStatus.VALID, result.getStatus());

        result = state.getBlockValidator().validate(block, BlockValidator.CONNECT_TO_CHAIN);

        assertEquals(ValidationStatus.VALID, result.getStatus());
    }
}
