package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.block.BlockRule;

public class MaxNonceSizeBlkRule extends BlockRule {
    @Override
    public boolean valid() {
        return block.getNonce().compareTo(consensus.getMaxNonce()) <= 0;
    }
}
