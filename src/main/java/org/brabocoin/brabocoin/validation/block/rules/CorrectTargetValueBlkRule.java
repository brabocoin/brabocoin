package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.block.BlockRule;

public class CorrectTargetValueBlkRule extends BlockRule {
    @Override
    public boolean valid() {
        return block.getTargetValue().equals(consensus.getTargetValue());
    }
}
