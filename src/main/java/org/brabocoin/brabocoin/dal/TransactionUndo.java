package org.brabocoin.brabocoin.dal;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.dal.utxo.UnspentOutputInfo;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transaction undo data to restore the UTXO set when the block containing this transaction is
 * disconnected.
 *
 * @see BlockDatabase
 * @see org.brabocoin.brabocoin.utxo.UTXOSet
 */
@ProtoClass(BrabocoinStorageProtos.TransactionUndo.class)
public class TransactionUndo implements ProtoModel<TransactionUndo> {

    @ProtoField
    private final @NotNull List<UnspentOutputInfo> outputInfoList;


    /**
     * Create a new transaction undo data record.
     *
     * @param outputInfoList
     *     List of unspent output info records from the UTXO database that are removed from the
     *     database when the transaction was added to the blockchain.
     */
    public TransactionUndo(@NotNull List<UnspentOutputInfo> outputInfoList) {
        this.outputInfoList = new ArrayList<>(outputInfoList);
    }

    public List<UnspentOutputInfo> getOutputInfoList() {
        return Collections.unmodifiableList(outputInfoList);
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinStorageProtos.TransactionUndo.class)
    public static class Builder implements ProtoBuilder<TransactionUndo> {

        @ProtoField
        private @NotNull List<UnspentOutputInfo.Builder> outputInfoList;

        public Builder setOutputInfoList(@NotNull List<UnspentOutputInfo.Builder> outputInfoList) {
            this.outputInfoList = outputInfoList;
            return this;
        }

        @Override
        public TransactionUndo build() {
            return new TransactionUndo(outputInfoList.stream()
                .map(UnspentOutputInfo.Builder::build)
                .collect(Collectors.toList()));
        }
    }
}
