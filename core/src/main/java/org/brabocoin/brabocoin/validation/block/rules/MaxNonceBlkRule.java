package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

import java.math.BigInteger;

@ValidationRule(name="Nonce smaller than max nonce", failedName = "Nonce is larger than max nonce", description = "The block nonce is smaller than the maximum nonce defined in consensus.")
public class MaxNonceBlkRule extends BlockRule {

    @DescriptionField
    private String blockNonce;
    @DescriptionField
    private String consensusMaxNonce;

    @Override
    public boolean isValid() {
        blockNonce = block.getNonce().toString(16);
        consensusMaxNonce = consensus.getMaxNonce().toString(16);
        return block.getNonce().compareTo(consensus.getMaxNonce()) < 0;
    }
}
