package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Correct target value", description = "Block target value equals consensus target value.")
public class CorrectTargetValueBlkRule extends BlockRule {

    private BraboConfig config;

    @Override
    public boolean isValid() {
        return block.getTargetValue().equals(config.targetValue());
    }
}
