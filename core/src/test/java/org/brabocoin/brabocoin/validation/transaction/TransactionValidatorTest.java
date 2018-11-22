package org.brabocoin.brabocoin.validation.transaction;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.*;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.processor.UTXOProcessor;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.testutil.TestState;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;
import org.brabocoin.brabocoin.validation.transaction.rules.MaxSizeTxRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TransactionValidatorTest {
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
    void ruleBookResult() throws DatabaseException {
        State state = new TestState(defaultConfig);

        Transaction transaction = new Transaction(
                Simulation.repeatedBuilder(Simulation::randomInput, 10000),
                Simulation.repeatedBuilder(Simulation::randomOutput, 10000), signatures
        );

        RuleBookResult result = state.getTransactionValidator().checkTransactionValid(transaction);

        assertFalse(result.isPassed());
        assertEquals(MaxSizeTxRule.class, result.getFailMarker().getFailedRule());
    }

    @Test
    void ruleBookResultFail() throws DatabaseException {
        State state = new TestState(defaultConfig);

        Transaction transaction = new Transaction(
                Simulation.repeatedBuilder(Simulation::randomInput, 10000),
                Simulation.repeatedBuilder(Simulation::randomOutput, 10000), signatures
        );

        assertThrows(IllegalStateException.class, () -> state.getTransactionValidator().checkTransactionValid(transaction));
    }
}
