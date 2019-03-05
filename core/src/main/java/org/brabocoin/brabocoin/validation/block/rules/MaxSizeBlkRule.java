package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Block size smaller than max block size", failedName = "Block size larger than max block size", description = "The block size is smaller than the maximum block size defined in consensus.")
public class MaxSizeBlkRule extends BlockRule {
    @DescriptionField
    private int blockSize;
    @DescriptionField
    private int consensusBlockSize;

    @Override
    public boolean isValid() {
        blockSize = ProtoConverter.toProto(block, BrabocoinProtos.Block.class)
            .getSerializedSize();
        consensusBlockSize = consensus.getMaxBlockSize();
        return blockSize <= consensusBlockSize;
    }
}
