package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Parent block is known", description = "The parent of the block is a known block.")
public class KnownParentBlkRule extends BlockRule {

    private Blockchain blockchain;
    @DescriptionField
    private boolean parentStored;

    @Override
    public boolean isValid() {
        try {
            parentStored = blockchain.isBlockStored(block.getPreviousBlockHash());
            return parentStored;
        }
        catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
