package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

import java.util.HashSet;
import java.util.Set;

@ValidationRule(name="No duplicate inputs", failedName = "Block contains duplicate inputs", description = "The block does not contain duplicate inputs.")
public class DuplicateInputBlkRule extends BlockRule {

    @Override
    public boolean isValid() {
        // Check for no duplicates inputs
        Set<Input> seen = new HashSet<>();
        return block.getTransactions()
            .stream()
            .allMatch(t -> t.getInputs().stream().allMatch(seen::add));
    }
}
