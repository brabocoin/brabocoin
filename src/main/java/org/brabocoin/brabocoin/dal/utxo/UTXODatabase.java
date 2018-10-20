package org.brabocoin.brabocoin.dal.utxo;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Provides the functionality of storing the unspent transaction outputs (UTXO) set.
 */
public class UTXODatabase {

    private static final ByteString KEY_PREFIX_OUTPUT = ByteString.copyFromUtf8("c");
    private static final ByteString KEY_BLOCK_MARKER = ByteString.copyFromUtf8("B");

    private final @NotNull KeyValueStore storage;

    /**
     * Creates a new UTXO set database using the provided key-value store.
     *
     * @param storage
     *     The key-value store to use for the database.
     * @throws DatabaseException
     *     When the database could not be initialized.
     */
    public UTXODatabase(@NotNull KeyValueStore storage) throws DatabaseException {
        this.storage = storage;
        initialize();
    }

    private void initialize() throws DatabaseException {
        ByteString key = getBlockMarkerKey();

        if (!storage.has(key)) {
            storage.put(key, ByteUtil.toByteString(0));
        }
    }

    private ByteString getBlockMarkerKey() {
        return KEY_BLOCK_MARKER;
    }

    /**
     * Checks whether the output referenced by the given transaction input is unspent.
     *
     * @param input
     *     The input to check.
     * @return Whether the input is unspent.
     * @throws DatabaseException
     *     When the output data could not be read.
     */
    public boolean isUnspent(@NotNull Input input) throws DatabaseException {
        return this.isUnspent(input.getReferencedTransaction(), input.getReferencedOutputIndex());
    }

    /**
     * Checks whether the indicated transaction output is unspent.
     *
     * @param transactionHash
     *     The hash of the transaction of the output to check.
     * @param outputIndex
     *     The index of the output in the transaction.
     * @return Whether the output is unspent.
     * @throws DatabaseException
     *     When the output data could not be read.
     */
    public boolean isUnspent(@NotNull Hash transactionHash, int outputIndex) throws DatabaseException {
        ByteString key = getOutputKey(transactionHash, outputIndex);
        return storage.has(key);
    }

    private ByteString getOutputKey(@NotNull Hash transactionHash, int outputIndex) {
        return KEY_PREFIX_OUTPUT.concat(transactionHash.getValue())
            .concat(ByteUtil.toByteString(outputIndex));
    }

    /**
     * Find the unspent output information for the output referenced by the given input.
     *
     * @param input
     *     The input to get the unspent information from.
     * @return The unspent output information, or {@code null} when the output is already spent.
     * @throws DatabaseException
     *     When the output data could not be read.
     */
    public @Nullable UnspentOutputInfo findUnspentOutputInfo(@NotNull Input input) throws DatabaseException {
        return this.findUnspentOutputInfo(input.getReferencedTransaction(),
            input.getReferencedOutputIndex()
        );
    }

    /**
     * Find the unspent output information for the indicated output.
     *
     * @param transactionHash
     *     The hash of the transaction of the output
     * @param outputIndex
     *     The index of the output in the transaction.
     * @return The unspent output information, or {@code null} when the output is already spent.
     * @throws DatabaseException
     *     When the output data could not be read.
     */
    public @Nullable UnspentOutputInfo findUnspentOutputInfo(@NotNull Hash transactionHash,
                                                             int outputIndex) throws DatabaseException {
        ByteString key = getOutputKey(transactionHash, outputIndex);
        ByteString value = retrieve(key);

        return parseProtoValue(value,
            UnspentOutputInfo.Builder.class,
            BrabocoinStorageProtos.UnspentOutputInfo.parser()
        );
    }

    private @Nullable ByteString retrieve(ByteString key) throws DatabaseException {
        return storage.get(key);
    }

    private <D extends ProtoModel<D>, B extends ProtoBuilder<D>, P extends Message> @Nullable D parseProtoValue(@Nullable ByteString value, @NotNull Class<B> domainClassBuilder, @NotNull Parser<P> parser) throws DatabaseException {
        try {
            return ProtoConverter.parseProtoValue(value, domainClassBuilder, parser);
        }
        catch (InvalidProtocolBufferException e) {
            throw new DatabaseException("Data could not be parsed", e);
        }
    }

    /**
     * Mark all outputs of the given transaction as unspent.
     *
     * @param transaction
     *     The transaction that provides the outputs.
     * @param blockHeight
     *     The height of the block in which the transaction is validated.
     * @throws DatabaseException
     *     When the data could not be stored.
     */
    public void setOutputsUnspent(@NotNull Transaction transaction, int blockHeight) throws DatabaseException {
        List<Integer> allOutputs = IntStream.range(0, transaction.getOutputs().size())
            .boxed()
            .collect(Collectors.toList());
        this.setOutputsUnspent(transaction, allOutputs, blockHeight);
    }

    /**
     * Mark outputs of the given transaction as unspent.
     *
     * @param transaction
     *     The transaction that provides the outputs.
     * @param outputIndices
     *     The indices of the outputs to mark as unspent.
     * @param blockHeight
     *     The height of the block in which the transaction is validated.
     * @throws DatabaseException
     *     When the data could not be stored.
     */
    public void setOutputsUnspent(@NotNull Transaction transaction,
                                  @NotNull List<Integer> outputIndices, int blockHeight) throws DatabaseException {

        Hash transactionHash = transaction.computeHash();
        boolean coinbase = transaction.isCoinbase();

        for (int outputIndex : outputIndices) {
            Output output = transaction.getOutputs().get(outputIndex);
            UnspentOutputInfo info = new UnspentOutputInfo(coinbase,
                blockHeight,
                output.getAmount(),
                output.getAddress()
            );

            addUnspentOutputInfo(transactionHash, outputIndex, info);
        }
    }

    /**
     * Add an unspent output information record to the database.
     *
     * @param transactionHash
     *     The hash of the transaction of the referenced output.
     * @param outputIndex
     *     The index of the referenced output.
     * @param info
     *     The unspent information.
     * @throws DatabaseException
     *     When the data could not be stored.
     */
    public void addUnspentOutputInfo(@NotNull Hash transactionHash, int outputIndex,
                                     @NotNull UnspentOutputInfo info) throws DatabaseException {
        ByteString key = getOutputKey(transactionHash, outputIndex);
        ByteString value = getRawProtoValue(info, BrabocoinStorageProtos.UnspentOutputInfo.class);

        store(key, value);
    }

    private <D extends ProtoModel<D>, P extends Message> ByteString getRawProtoValue(D domainObject, Class<P> protoClass) {
        return ProtoConverter.toProto(domainObject, protoClass).toByteString();
    }

    private void store(ByteString key, ByteString value) throws DatabaseException {
        storage.put(key, value);
    }

    /**
     * Mark an output of a given transaction as spent.
     *
     * @param transactionHash
     *     The transaction that provides the output.
     * @param outputIndex
     *     The index of the outputs to mark as spent.
     * @throws DatabaseException
     *     When the data could not be stored.
     */
    public void setOutputSpent(@NotNull Hash transactionHash, int outputIndex) throws DatabaseException {
        ByteString key = getOutputKey(transactionHash, outputIndex);
        storage.delete(key);
    }

    /**
     * Retrieve the hash of the last block up to which the UTXO set is up-to-date.
     *
     * @return The block hash of the last processed block.
     * @throws DatabaseException
     *     When the data could not be retrieved.
     */
    public @NotNull Hash getLastProcessedBlockHash() throws DatabaseException {
        ByteString key = getBlockMarkerKey();
        ByteString value = retrieve(key);

        Hash hash = parseProtoValue(value, Hash.Builder.class, BrabocoinProtos.Hash.parser());

        if (hash == null) {
            throw new DatabaseException("Last processed block hash could not be found.");
        }

        return hash;
    }

    /**
     * Sets the hash of the last block up to which the UTZO set is up-to-date.
     *
     * @param hash
     *     The hash of the last processed block.
     * @throws DatabaseException
     *     When the data could not be stored.
     */
    public void setLastProcessedBlockHash(@NotNull Hash hash) throws DatabaseException {
        ByteString key = getBlockMarkerKey();
        ByteString value = getRawProtoValue(hash, BrabocoinProtos.Hash.class);

        store(key, value);
    }
}
