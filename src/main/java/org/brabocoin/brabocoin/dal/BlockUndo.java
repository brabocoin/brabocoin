package org.brabocoin.brabocoin.dal;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Block undo data to restore the UTXO set when the block is disconnected.
 *
 * @see BlockDatabase
 * @see org.brabocoin.brabocoin.utxo.UTXOSet
 */
@ProtoClass(BrabocoinStorageProtos.BlockUndo.class)
public class BlockUndo implements ProtoModel<BlockUndo> {

    @ProtoField
    private final @NotNull List<TransactionUndo> transactionUndos;

    /**
     * Create a new block undo data record.
     *
     * @param transactionUndos
     *     List of the undo records of the transactions in the block.
     */
    public BlockUndo(List<TransactionUndo> transactionUndos) {
        this.transactionUndos = new ArrayList<>(transactionUndos);
    }

    public List<TransactionUndo> getTransactionUndos() {
        return Collections.unmodifiableList(transactionUndos);
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    public static class Builder implements ProtoBuilder<BlockUndo> {

        private List<TransactionUndo.Builder> transactionUndos;

        public Builder setTransactionUndos(List<TransactionUndo.Builder> transactionUndos) {
            this.transactionUndos = transactionUndos;
            return this;
        }

        @Override
        public BlockUndo build() {
            return new BlockUndo(transactionUndos.stream()
                .map(TransactionUndo.Builder::build)
                .collect(Collectors.toList()));
        }
    }
}
