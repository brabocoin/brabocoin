package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.crypto.MerkleTree;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.block.BlockRule;

import java.util.stream.Collectors;

public class ValidMerkleRootBlkRule extends BlockRule {
    @Override
    public boolean valid() {
        return block.getMerkleRoot().equals(new MerkleTree(consensus.getMerkleTreeHashFunction(),
                block.getTransactions().stream().map(Transaction::computeHash).collect(Collectors.toList())
        ).getRoot());
    }
}
