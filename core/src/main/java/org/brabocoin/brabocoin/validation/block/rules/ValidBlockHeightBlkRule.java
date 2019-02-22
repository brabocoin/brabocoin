package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Valid block height", failedName = "Invalid block height", description = "The block height is one larger than the parent's block height.")
public class ValidBlockHeightBlkRule extends BlockRule {

    private Blockchain blockchain;
    @DescriptionField
    private int heightFromParent;
    @DescriptionField
    private int blockHeight;

    @Override
    public boolean isValid() {
        try {
            heightFromParent = blockchain.getIndexedBlock(block.getPreviousBlockHash())
                .getBlockInfo()
                .getBlockHeight();

            blockHeight = block.getBlockHeight();

            return heightFromParent + 1 == blockHeight ;
        }
        catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
