package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

import java.math.BigInteger;

@ValidationRule(name="Nonce smaller than max nonce", description = "The block nonce is smaller than the maximum nonce defined in consensus.")
public class MaxNonceBlkRule extends BlockRule {

    @DescriptionField
    private BigInteger blockNonce;
    @DescriptionField
    private BigInteger consensusMaxNonce;

    @Override
    public boolean isValid() {
        blockNonce = block.getNonce();
        consensusMaxNonce = consensus.getMaxNonce();
        return blockNonce.compareTo(consensusMaxNonce) < 0;
    }
}
