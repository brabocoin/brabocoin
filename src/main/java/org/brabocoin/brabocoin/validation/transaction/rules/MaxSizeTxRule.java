package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.validation.Consensus;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

/**
 * Transaction rule
 */
@Rule(name = "Transaction size rule", description = "Size of transaction should be limited.")
public class MaxSizeTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction, @Fact("consensus") Consensus consensus) {
        int size = ProtoConverter.toProto(transaction, BrabocoinProtos.Transaction.class).getSerializedSize();
        return size >= consensus.getMaxBlockSize() + consensus.getMaxNonceSize();
    }
}
