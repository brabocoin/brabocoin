package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Parent block is valid", description = "The parent of the block is a valid block.")
public class ValidParentBlkRule extends BlockRule {

    private Blockchain blockchain;
    @DescriptionField
    private boolean parentValid;

    @Override
    public boolean isValid() {
        try {
            IndexedBlock indexedBlock = blockchain.getIndexedBlock(block.getPreviousBlockHash());
            if (indexedBlock == null) {
                return false;
            }

            parentValid = indexedBlock.getBlockInfo().isValid();
            return parentValid;
        }
        catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
