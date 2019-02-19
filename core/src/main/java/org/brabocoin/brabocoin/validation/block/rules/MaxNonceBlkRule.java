package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Nonce smaller than max nonce", failedName = "Nonce is larger than max nonce", description = "The block nonce is smaller than the maximum nonce defined in consensus.")
public class MaxNonceBlkRule extends BlockRule {

    @Override
    public boolean isValid() {
        return block.getNonce().compareTo(consensus.getMaxNonce()) < 0;
    }
}
