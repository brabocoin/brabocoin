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
}
