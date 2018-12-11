package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Correct target value", description = "Target value equals consensus target value.")
public class CorrectTargetValueBlkRule extends BlockRule {

    @Override
    public boolean isValid() {
        return block.getTargetValue().equals(consensus.getTargetValue());
    }
}
