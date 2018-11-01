package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

/**
 * Transaction rule
 */
@Rule(name = "Transaction size rule", description = "Size of transaction should be limited.")
public class MaxSizeTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction) {
        // TODO: where do we determine max block size? constant? config?
        int size = ProtoConverter.toProto(transaction, BrabocoinProtos.Transaction.class).getSerializedSize();
        if (size < 0 /* //TODO: implement */) {
            return false;
        }
        return true;
    }
}
