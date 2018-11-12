package org.brabocoin.brabocoin.validation.block;

import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.Rule;

public class BlockRule implements Rule {
    protected Block block;
    protected Consensus consensus;

    @Override
    public boolean valid() {
        return false;
    }
}
