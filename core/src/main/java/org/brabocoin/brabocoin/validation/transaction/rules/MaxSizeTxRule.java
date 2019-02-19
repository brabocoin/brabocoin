package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 * <p>
 * Size of transaction should be limited.
 */
@ValidationRule(name="Transaction size smaller than max transaction size", description = "The transaction size is smaller than the maximum transaction size defined in consensus.")
public class MaxSizeTxRule extends TransactionRule {

    @DescriptionField
    private int size;
    @DescriptionField
    private long consensusSize;

    public boolean isValid() {
        size = ProtoConverter.toProto(transaction, BrabocoinProtos.Transaction.class)
            .getSerializedSize();
        consensusSize = consensus.getMaxTransactionSize();

        return size < consensusSize;
    }
}
