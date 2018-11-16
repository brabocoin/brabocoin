package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

/**
 * Provides the functionality of storing the unspent transaction outputs (UTXO) set.
 */
public class UTXODatabase implements ReadonlyUTXOSet {

    private static final Logger LOGGER = Logger.getLogger(UTXODatabase.class.getName());
    private static final ByteString KEY_PREFIX_OUTPUT = ByteString.copyFromUtf8("c");
    protected final @NotNull KeyValueStore storage;

    /**
     * Creates a new UTXO set database using the provided key-value store.
     *
     * @param storage
     *     The key-value store to use for the database.
     */
    public UTXODatabase(@NotNull KeyValueStore storage) {
        this.storage = storage;
    }

    @Override
    public synchronized boolean isUnspent(@NotNull Input input) throws DatabaseException {
        LOGGER.fine("Checking whether input is unspent.");
        return this.isUnspent(input.getReferencedTransaction(), input.getReferencedOutputIndex());
    }

    @Override
    public synchronized boolean isUnspent(@NotNull Hash transactionHash, int outputIndex) throws DatabaseException {
        LOGGER.fine("Checking whether a transaction hash with given output index is unspent.");
        ByteString key = getOutputKey(transactionHash, outputIndex);
        boolean has = storage.has(key);
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Hash: {0}, output index: {1}, unspent: {2}",
            toHexString(transactionHash.getValue()),
            outputIndex,
            has
        ));
        return has;
    }

    private synchronized ByteString getOutputKey(@NotNull Hash transactionHash, int outputIndex) {
        ByteString outputKey = KEY_PREFIX_OUTPUT.concat(transactionHash.getValue())
            .concat(ByteUtil.toByteString(outputIndex));
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Output key: {0}", toHexString(outputKey)));
        return outputKey;
    }

    @Override
    public synchronized @Nullable UnspentOutputInfo findUnspentOutputInfo(@NotNull Input input) throws DatabaseException {
        LOGGER.fine("Getting unspent output info for the output referenced by the given input.");
        return this.findUnspentOutputInfo(input.getReferencedTransaction(),
            input.getReferencedOutputIndex()
        );
    }

    @Override
    public synchronized @Nullable UnspentOutputInfo findUnspentOutputInfo(@NotNull Hash transactionHash, int outputIndex) throws DatabaseException {
        LOGGER.fine("Getting unspent output info for a given transaction hash and output index.");
        ByteString key = getOutputKey(transactionHash, outputIndex);
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("key: {0}", key));
        ByteString value = retrieve(key);
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("value: {0}", value));

        return parseProtoValue(value,
            UnspentOutputInfo.Builder.class,
            BrabocoinStorageProtos.UnspentOutputInfo.parser()
        );
    }

    @Nullable
    protected synchronized ByteString retrieve(ByteString key) throws DatabaseException {
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Retrieving ByteString from key: {0}", toHexString(key)));
        ByteString bytes = storage.get(key);
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Got ByteString: {0}", toHexString(bytes)));
        return bytes;
    }

    protected synchronized <D extends ProtoModel<D>, B extends ProtoBuilder<D>, P extends Message> @Nullable D parseProtoValue(@Nullable ByteString value, @NotNull Class<B> domainClassBuilder, @NotNull Parser<P> parser) throws DatabaseException {
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Parsing proto value from byte array: {0}", value));
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
         * @param transaction     The transaction that provides the outputs.
         * @param blockHeight
              The height of the block in which the transaction is validated.
         * @throws DatabaseException
              When the data could not be stored.
         */
    public synchronized void setOutputsUnspent(@NotNull Transaction transaction, int blockHeight) throws DatabaseException {
        LOGGER.fine("Mark all outputs of the given transaction as unspent.");
        List<Integer> allOutputs = IntStream.range(0, transaction.getOutputs().size())
            .boxed()
            .collect(Collectors.toList());
        this.setOutputsUnspent(transaction, allOutputs, blockHeight);
    }

    /**
         * Mark outputs of the given transaction as unspent.
         *
         * @param transaction     The transaction that provides the outputs.
         * @param outputIndices
              The indices of the outputs to mark as unspent.
         * @param blockHeight
              The height of the block in which the transaction is validated.
         * @throws DatabaseException
              When the data could not be stored.
         */
    public synchronized void setOutputsUnspent(@NotNull Transaction transaction, @NotNull List<Integer> outputIndices, int blockHeight) throws DatabaseException {
        LOGGER.fine("Mark outputs of the given transaction as unspent.");
        Hash transactionHash = transaction.getHash();
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Transaction hash: {0}", toHexString(transactionHash.getValue())));
        boolean coinbase = transaction.isCoinbase();
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Coinbase: {0}", coinbase));

        for (int outputIndex : outputIndices) {
            Output output = transaction.getOutputs().get(outputIndex);
            UnspentOutputInfo info = new UnspentOutputInfo(coinbase,
                blockHeight,
                output.getAmount(),
                output.getAddress()
            );
            LOGGER.log(Level.FINEST, () -> MessageFormat.format("Setting output to unspent, index: {0}, amount: {1}, blockheight: {2}, address: {3}",
                outputIndex,
                output.getAmount(),
                blockHeight,
                output.getAddress()
            ));

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
    public synchronized void addUnspentOutputInfo(@NotNull Hash transactionHash, int outputIndex, @NotNull UnspentOutputInfo info) throws DatabaseException {
        ByteString key = getOutputKey(transactionHash, outputIndex);
        ByteString value = getRawProtoValue(info, BrabocoinStorageProtos.UnspentOutputInfo.class);

        LOGGER.log(Level.FINE, "Storing key and value for outputIndex: {0}", outputIndex);
        store(key, value);
    }

    protected synchronized <D extends ProtoModel<D>, P extends Message> ByteString getRawProtoValue(D domainObject, Class<P> protoClass) {
        return ProtoConverter.toProto(domainObject, protoClass).toByteString();
    }

    protected synchronized void store(ByteString key, ByteString value) throws DatabaseException {
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Storing key: {0}, value: {1}",
            toHexString(key),
            toHexString(value)
        ));
        storage.put(key, value);
    }

    /**
         * Mark an output of a given transaction as spent.
         *
         * @param transactionHash     The transaction that provides the output.
         * @param outputIndex
              The index of the outputs to mark as spent.
         * @throws DatabaseException
              When the data could not be stored.
         */
    public synchronized void setOutputSpent(@NotNull Hash transactionHash, int outputIndex) throws DatabaseException {
        LOGGER.log(Level.FINE, "Marking output as spent");
        ByteString key = getOutputKey(transactionHash, outputIndex);
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("key: {0}", toHexString(key)));
        storage.delete(key);
    }
}
