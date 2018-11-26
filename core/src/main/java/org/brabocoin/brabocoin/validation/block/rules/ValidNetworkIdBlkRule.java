package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.validation.block.BlockRule;

public class ValidNetworkIdBlkRule extends BlockRule {
    private BraboConfig config;

    @Override
    public boolean isValid() {
        return block.getNetworkId() == config.networkId();
    }
}
