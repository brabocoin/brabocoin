package org.brabocoin.brabocoin.validation;

import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.processor.ProcessedBlockStatus;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * @author Sten Wessel
 */
public class BlockValidator {

    private static final Logger LOGGER = Logger.getLogger(BlockValidator.class.getName());

    public ProcessedBlockStatus checkBlockValid(@NotNull Block block) {
        // TODO: implement
        return ProcessedBlockStatus.VALID;
    }

    public ProcessedBlockStatus checkOrphanBlockValid(@NotNull Block block) {
        // TODO: implement
        return ProcessedBlockStatus.VALID;
    }
}
