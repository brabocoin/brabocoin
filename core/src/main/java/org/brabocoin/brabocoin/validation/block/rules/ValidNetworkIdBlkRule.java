package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Valid network id", failedName = "Invalid network id", description = "The network id of the block is equal to the network id defined in configuration.")
public class ValidNetworkIdBlkRule extends BlockRule {
    private BraboConfig config;

    @Override
    public boolean isValid() {
        return block.getNetworkId() == config.networkId();
    }
}
