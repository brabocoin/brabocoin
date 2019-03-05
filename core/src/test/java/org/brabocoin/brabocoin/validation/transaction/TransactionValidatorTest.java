package org.brabocoin.brabocoin.validation.transaction;

import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.testutil.LegacyBraboConfig;
import org.brabocoin.brabocoin.testutil.MockLegacyConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.testutil.TestState;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;
import org.brabocoin.brabocoin.validation.transaction.rules.MaxSizeTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.ValidInputUTXOTxRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TransactionValidatorTest {

    static MockLegacyConfig defaultConfig =
        new MockLegacyConfig(new LegacyBraboConfig(new BraboConfig()));
    Consensus consensus = new Consensus();

    private static Signer signer;

    private static final EllipticCurve CURVE = EllipticCurve.secp256k1();

    @BeforeAll
    static void setUp() {
        defaultConfig = new MockLegacyConfig(defaultConfig);

        signer = new Signer(CURVE);
    }


    @Test
    void ruleBookResult() throws DatabaseException {
        State state = new TestState(defaultConfig);

        Transaction transaction = new Transaction(
            Simulation.repeatedBuilder(Simulation::randomInput, 10000),
            Simulation.repeatedBuilder(Simulation::randomOutput, 10000),
            Collections.emptyList()
        );

        RuleBookResult result = state.getTransactionValidator()
            .validate(transaction, TransactionValidator.ALL, true);

        assertFalse(result.isPassed());
        assertEquals(MaxSizeTxRule.class, result.getFailMarker().getFailedRule());
    }

    @Test
    void negativeOutputIndex() throws DatabaseException {
        State state = new TestState(defaultConfig);

        Transaction transaction = new Transaction(
            Collections.singletonList(
                new Input(Simulation.randomHash(), -1)
            ),
            Collections.singletonList(
                new Output(Simulation.randomHash(), 100)
            ),
            Collections.singletonList(
                Simulation.randomSignature()
            )
        );

        RuleBookResult result = state.getTransactionValidator()
            .validate(transaction, TransactionValidator.ALL, true);

        assertFalse(result.isPassed());
        assertEquals(ValidInputUTXOTxRule.class, result.getFailMarker().getFailedRule());
    }
}
