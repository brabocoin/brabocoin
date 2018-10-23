package org.brabocoin.brabocoin.dal.utxo;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import org.brabocoin.brabocoin.crypto.Hashing;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

/**
 * Provides the functionality of storing the unspent transaction outputs (UTXO) set.
 */
public class UTXODatabase {
    private static final Logger LOGGER = Logger.getLogger(Hashing.class.getName());

    private static final ByteString KEY_PREFIX_OUTPUT = ByteString.copyFromUtf8("c");
    private static final ByteString KEY_BLOCK_MARKER = ByteString.copyFromUtf8("B");

    private final @NotNull KeyValueStore storage;

    /**
     * Creates a new UTXO set database using the provided key-value store.
     *
     * @param storage The key-value store to use for the database.
     */
    public UTXODatabase(@NotNull KeyValueStore storage) throws DatabaseException {
        this.storage = storage;
        initialize();
    }

    private void initialize() throws DatabaseException {
        LOGGER.info("Initializing UTXO database.");
        ByteString key = getBlockMarkerKey();

        if (!storage.has(key)) {
            LOGGER.fine("Storage block marker key not found");
            storage.put(key, ByteUtil.toByteString(0));
            LOGGER.fine("Storage block marker key created");
        }
    }

    /**
     * Checks whether the output referenced by the given transaction input is unspent.
     *
     * @param input The input to check.
     * @return Whether the input is unspent.
     * @throws DatabaseException When the output data could not be read.
     */
    public boolean isUnspent(@NotNull Input input) throws DatabaseException {
        LOGGER.fine("Checking whether input is unspent.");
        return this.isUnspent(input.getReferencedTransaction(), input.getReferencedOutputIndex());
    }

    /**
     * Checks whether the indicated transaction output is unspent.
     *
     * @param transactionHash The hash of the transaction of the output to check.
     * @param outputIndex     The index of the output in the transaction.
     * @return Whether the output is unspent.
     * @throws DatabaseException When the output data could not be read.
     */
    public boolean isUnspent(@NotNull Hash transactionHash, int outputIndex) throws DatabaseException {
        LOGGER.fine("Checking whether a transaction hash with given output index is unspent.");
        ByteString key = getOutputKey(transactionHash, outputIndex);
        boolean has = storage.has(key);
        LOGGER.log(Level.FINEST, "Hash: {0}, output index: {1}, unspent: {2}", new Object[]{toHexString(transactionHash.getValue()), outputIndex, has});
        return has;
    }

    private ByteString getOutputKey(@NotNull Hash transactionHash, int outputIndex) {
        ByteString outputKey = KEY_PREFIX_OUTPUT.concat(transactionHash.getValue())
                .concat(ByteUtil.toByteString(outputIndex));
        LOGGER.log(Level.FINEST, "Output key: {0}", toHexString(outputKey));
        return outputKey;
    }

    /**
     * Find the unspent output information for the output referenced by the given input.
     *
     * @param input The input to get the unspent information from.
     * @return The unspent output information, or {@code null} when the output is already spent.
     * @throws DatabaseException When the output data could not be read.
     */
    public @Nullable UnspentOutputInfo findUnspentOutputInfo(@NotNull Input input) throws DatabaseException {
        LOGGER.fine("Getting unspent output info for the output referenced by the given input.");
        return this.findUnspentOutputInfo(input.getReferencedTransaction(),
                input.getReferencedOutputIndex()
        );
    }

    /**
     * Find the unspent output information for the indicated output.
     *
     * @param transactionHash The hash of the transaction of the output
     * @param outputIndex     The index of the output in the transaction.
     * @return The unspent output information, or {@code null} when the output is already spent.
     * @throws DatabaseException When the output data could not be read.
     */
    public @Nullable UnspentOutputInfo findUnspentOutputInfo(@NotNull Hash transactionHash,
                                                             int outputIndex) throws DatabaseException {
        LOGGER.fine("Getting unspent output info for a given transaction hash and output index.");
        ByteString key = getOutputKey(transactionHash, outputIndex);
        LOGGER.log(Level.FINEST, "key: {0}", key);
        ByteString value = retrieve(key);
        LOGGER.log(Level.FINEST, "value: {0}", value);

        return parseProtoValue(value,
                UnspentOutputInfo.Builder.class,
                BrabocoinStorageProtos.UnspentOutputInfo.parser()
        );
    }

    private @Nullable ByteString retrieve(ByteString key) throws DatabaseException {
        LOGGER.log(Level.FINEST, "Retrieving ByteString from key: {0}", toHexString(key));
        ByteString bytes = storage.get(key);
        LOGGER.log(Level.FINEST, "Got ByteString: {0}", toHexString(bytes));
        return bytes;
    }

    private <D extends ProtoModel<D>, B extends ProtoBuilder<D>, P extends Message> @Nullable D parseProtoValue(@Nullable ByteString value, @NotNull Class<B> domainClassBuilder, @NotNull Parser<P> parser) throws DatabaseException {
        LOGGER.log(Level.FINEST, "Parsing proto value from byte array: {0}", value);
        try {
            return ProtoConverter.parseProtoValue(value, domainClassBuilder, parser);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.log(Level.SEVERE, "Data parsing failed: {0}", e.getMessage());
            throw new DatabaseException("Data could not be parsed", e);
        }
    }

    /**
     * Mark all outputs of the given transaction as unspent.
     *
     * @param transaction The transaction that provides the outputs.
     * @param blockHeight The height of the block in which the transaction is validated.
     * @throws DatabaseException When the data could not be stored.
     */
    public void setOutputsUnspent(@NotNull Transaction transaction, int blockHeight) throws DatabaseException {
        LOGGER.fine("Mark all outputs of the given transaction as unspent.");
        List<Integer> allOutputs = IntStream.range(0, transaction.getOutputs().size())
                .boxed()
                .collect(Collectors.toList());
        this.setOutputsUnspent(transaction, allOutputs, blockHeight);
    }

    /**
     * Mark outputs of the given transaction as unspent.
     *
     * @param transaction   The transaction that provides the outputs.
     * @param outputIndices The indices of the outputs to mark as unspent.
     * @param blockHeight   The height of the block in which the transaction is validated.
     * @throws DatabaseException When the data could not be stored.
     */
    public void setOutputsUnspent(@NotNull Transaction transaction,
                                  @NotNull List<Integer> outputIndices, int blockHeight) throws DatabaseException {
        LOGGER.fine("Mark outputs of the given transaction as unspent.");
        Hash transactionHash = transaction.computeHash();
        LOGGER.log(Level.FINEST, "Transaction hash: {0}", toHexString(transactionHash.getValue()));
        boolean coinbase = transaction.isCoinbase();
        LOGGER.log(Level.FINEST, "Coinbase: {0}", coinbase);

        for (int outputIndex : outputIndices) {
            Output output = transaction.getOutputs().get(outputIndex);
            UnspentOutputInfo info = new UnspentOutputInfo(coinbase,
                    blockHeight,
                    output.getAmount(),
                    output.getAddress()
            );
            LOGGER.log(Level.FINEST, "Setting output to unspent, index: {0}, amount: {1}, blockheight: {2}, address: {3}", new Object[]{outputIndex, output.getAmount(), blockHeight, output.getAddress()});

            ByteString key = getOutputKey(transactionHash, outputIndex);
            ByteString value = getRawProtoValue(
                    info,
                    BrabocoinStorageProtos.UnspentOutputInfo.class
            );

            LOGGER.log(Level.FINE, "Storing key and value for outputIndex: {0}", outputIndex);
            store(key, value);
        }
    }

    private <D extends ProtoModel<D>, P extends Message> ByteString getRawProtoValue(D domainObject, Class<P> protoClass) {
        return ProtoConverter.toProto(domainObject, protoClass).toByteString();
    }

    private void store(ByteString key, ByteString value) throws DatabaseException {
        LOGGER.log(Level.FINEST, "Storing key: {0}, value: {1}", new Object[]{toHexString(key), toHexString(value)});
        storage.put(key, value);
    }

    /**
     * Mark an output of a given transaction as spent.
     *
     * @param transactionHash The transaction that provides the output.
     * @param outputIndex     The index of the outputs to mark as spent.
     * @throws DatabaseException When the data could not be stored.
     */
    public void setOutputSpent(@NotNull Hash transactionHash, int outputIndex) throws DatabaseException {
        LOGGER.log(Level.FINE, "Marking output as spent");
        ByteString key = getOutputKey(transactionHash, outputIndex);
        LOGGER.log(Level.FINEST, "key: {0}", toHexString(key));
        storage.delete(key);
    }

    /**
     * Retrieve the hash of the last block up to which the UTXO set is up-to-date.
     *
     * @return The block hash of the last processed block.
     * @throws DatabaseException When the data could not be retrieved.
     */
    public @NotNull Hash getLastProcessedBlockHash() throws DatabaseException {
        LOGGER.log(Level.FINE, "Getting last processed block hash.");
        ByteString key = getBlockMarkerKey();
        LOGGER.log(Level.FINEST, "key: {0}", toHexString(key));
        ByteString value = retrieve(key);
        LOGGER.log(Level.FINEST, "value: {0}", toHexString(value));

        Hash hash = parseProtoValue(value, Hash.Builder.class, BrabocoinProtos.Hash.parser());

        if (hash == null) {
            LOGGER.severe("Could not find last processed block");
            throw new DatabaseException("Last processed block hash could not be found.");
        }

        return hash;
    }

    /**
     * Sets the hash of the last block up to which the UTXO set is up-to-date.
     *
     * @param hash The hash of the last processed block.
     * @throws DatabaseException When the data could not be stored.
     */
    public void setLastProcessedBlockHash(@NotNull Hash hash) throws DatabaseException {
        LOGGER.log(Level.FINE, "Sets the hash of the last block up to which the UTXO set is up-to-date.");
        ByteString key = getBlockMarkerKey();
        LOGGER.log(Level.FINEST, "key: {0}", toHexString(key));
        ByteString value = getRawProtoValue(hash, BrabocoinProtos.Hash.class);
        LOGGER.log(Level.FINEST, "value: {0}", toHexString(value));

        store(key, value);
    }

    private ByteString getBlockMarkerKey() {
        LOGGER.log(Level.FINE, "Block marker key value: {0}", toHexString(KEY_BLOCK_MARKER));
        return KEY_BLOCK_MARKER;
    }
}
