package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.block.BlockRule;

public class ValidParentBlkRule extends BlockRule {
    private Blockchain blockchain;

    @Override
    public boolean isValid() {
        try {
            IndexedBlock indexedBlock = blockchain.getIndexedBlock(block.getPreviousBlockHash());
            if (indexedBlock == null) {
                return false;
            }
            return indexedBlock.getBlockInfo().isValid();
        } catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
