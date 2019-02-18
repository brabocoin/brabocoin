package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Parent block is valid", failedName = "Parent block is invalid", description = "The parent of the block is a valid block.")
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
        }
        catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
