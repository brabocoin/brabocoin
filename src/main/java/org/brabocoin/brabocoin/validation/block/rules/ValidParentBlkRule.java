package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.validation.block.BlockRule;

public class ValidParentBlkRule extends BlockRule {
    private Blockchain blockchain;

    @Override
    public boolean valid() {
//        try {
            return false;//blockchain.getIndexedBlock(block.getPreviousBlockHash()).getBlockInfo();
//        } catch (DatabaseException e) {
//            e.printStackTrace();
//            return false;
//        }
    }
}
