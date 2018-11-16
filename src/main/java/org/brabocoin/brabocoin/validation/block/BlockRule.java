package org.brabocoin.brabocoin.validation.block;

import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.rule.Rule;

public abstract class BlockRule implements Rule {
    protected Block block;
    protected Consensus consensus;

}
