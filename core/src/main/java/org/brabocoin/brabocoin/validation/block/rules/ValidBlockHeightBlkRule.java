package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.block.BlockRule;

public class ValidBlockHeightBlkRule extends BlockRule {

    private Blockchain blockchain;

    @Override
    public boolean isValid() {
        try {
            int heightFromParent = blockchain.getIndexedBlock(block.getPreviousBlockHash())
                .getBlockInfo()
                .getBlockHeight() + 1;

            return heightFromParent == block.getBlockHeight();
        }
        catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
