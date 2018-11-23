package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.crypto.MerkleTree;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.block.BlockRule;

import java.util.stream.Collectors;

public class ValidMerkleRootBlkRule extends BlockRule {

    @Override
    public boolean isValid() {
        Hash computedRoot = new MerkleTree(
            consensus.getMerkleTreeHashFunction(),
            block.getTransactions().stream().map(Transaction::getHash).collect(Collectors.toList())
        ).getRoot();

        return block.getMerkleRoot().equals(computedRoot);
    }
}
