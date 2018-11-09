package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * Size of transaction should be limited.
 */
@Rule(name = "Transaction size rule")
public class MaxSizeTxRule extends TransactionRule {
    @When
    public boolean valid() {
        int size = ProtoConverter.toProto(transaction, BrabocoinProtos.Transaction.class).getSerializedSize();
        return size < consensus.getMaxBlockSize() + consensus.getMaxNonceSize();
    }
}
