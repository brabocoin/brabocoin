package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Correct target value", description = "Block target value equals consensus target value.")
public class CorrectTargetValueBlkRule extends BlockRule {

    private BraboConfig config;
    @DescriptionField
    private Hash blockTargetValue;
    @DescriptionField
    private Hash configTargetValue;

    @Override
    public boolean isValid() {
        blockTargetValue = block.getTargetValue();
        configTargetValue = config.targetValue();

        return blockTargetValue.equals(configTargetValue);
    }
}
