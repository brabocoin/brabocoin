package org.brabocoin.brabocoin.model;

import org.brabocoin.brabocoin.validation.block.BlockValidationResult;
import org.jetbrains.annotations.NotNull;

/**
 * A block that has been rejected.
 */
public class RejectedBlock {

    private final @NotNull Block block;

    private final @NotNull BlockValidationResult validationResult;

    public RejectedBlock(@NotNull Block block,
                         @NotNull BlockValidationResult validationResult) {
        this.block = block;
        this.validationResult = validationResult;
    }

    public @NotNull Block getBlock() {
        return block;
    }

    public @NotNull BlockValidationResult getValidationResult() {
        return validationResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RejectedBlock that = (RejectedBlock)o;

        return block.getHash().equals(that.block.getHash());
    }

    @Override
    public int hashCode() {
        return block.getHash().hashCode();
    }
}
