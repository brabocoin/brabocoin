package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Valid network ID", failedName = "Invalid network ID", description = "The network ID of the block is equal to the network ID defined in configuration.")
public class ValidNetworkIdBlkRule extends BlockRule {
    private BraboConfig config;

    @DescriptionField
    private int networkID;
    @DescriptionField
    private int configID;

    @Override
    public boolean isValid() {
        networkID = block.getNetworkId();
        configID = config.networkId();

        return networkID == configID;
    }
}
