package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.block.BlockRule;

import java.math.BigInteger;

public class MaxNonceSizeBlkRule extends BlockRule {
    @Override
    public boolean valid() {
        return new BigInteger(
                block.getNonce().toByteArray()
        ).compareTo(
                new BigInteger(consensus.getMaxNonce().toByteArray())
        ) <= 0;
    }
}
