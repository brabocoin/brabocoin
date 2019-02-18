package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.crypto.MerkleTree;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

import java.util.stream.Collectors;

@ValidationRule(name="Valid merkle root", description = "The block's merkle root is equal to the merkle root of the block's transaction's merkle tree.")
public class ValidMerkleRootBlkRule extends BlockRule {

    @DescriptionField
    private Hash computedRoot;
    @DescriptionField
    private Hash blockRoot;

    @Override
    public boolean isValid() {
        computedRoot = new MerkleTree(
            consensus.getMerkleTreeHashFunction(),
            block.getTransactions().stream().map(Transaction::getHash).collect(Collectors.toList())
        ).getRoot();

        blockRoot = block.getMerkleRoot();

        return blockRoot.equals(computedRoot);
    }
}
