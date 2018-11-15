package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.validation.block.BlockRule;

public class MaxSizeBlkRule extends BlockRule {
    @Override
    public boolean valid() {
        return ProtoConverter.toProto(block, BrabocoinProtos.Block.class).getSerializedSize() <= consensus.getMaxBlockSize();
    }
}
