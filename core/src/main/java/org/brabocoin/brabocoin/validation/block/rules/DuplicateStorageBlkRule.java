package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Block not already stored", description = "The block should not already be stored in the blockchain.")
public class DuplicateStorageBlkRule extends BlockRule {

    private Blockchain blockchain;

    @Override
    public boolean isValid() {
        try {
            return !blockchain.isBlockStored(block.getHash());
        }
        catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
