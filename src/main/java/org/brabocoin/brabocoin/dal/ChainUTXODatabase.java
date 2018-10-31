package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.validation.Consensus;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

/**
 * Provides the functionality of storing the unspent transaction outputs (UTXO) set for the blockchain.
 */
public class ChainUTXODatabase extends UTXODatabase {

    private static final Logger LOGGER = Logger.getLogger(ChainUTXODatabase.class.getName());

    private static final ByteString KEY_BLOCK_MARKER = ByteString.copyFromUtf8("B");

    /**
     * Creates a new UTXO set database using the provided key-value store.
     *
     * @param storage
     *     The key-value store to use for the database.
     * @param consensus
     *     The consensus on which this UTXO needs to be initialized, if necessary.
     * @throws DatabaseException
     *     When the database could not be initialized.
     */
    public ChainUTXODatabase(@NotNull KeyValueStore storage, @NotNull Consensus consensus) throws DatabaseException {
        super(storage);
        initialize(consensus.getGenesisBlock());
    }

    private void initialize(@NotNull Block genesisBlock) throws DatabaseException {
        LOGGER.info("Initializing UTXO database.");
        ByteString key = getBlockMarkerKey();

        if (!storage.has(key)) {
            LOGGER.fine("Storage block marker key not found.");
            setLastProcessedBlockHash(genesisBlock.computeHash());
            LOGGER.fine("Storage block marker key created from consensus genesis block hash.");
        }
    }

    /**
     * Retrieve the hash of the last block up to which the UTXO set is up-to-date.
     *
     * @return The block hash of the last processed block.
     * @throws DatabaseException     When the data could not be retrieved.
     */
    public synchronized @NotNull Hash getLastProcessedBlockHash() throws DatabaseException {
        LOGGER.log(Level.FINE, "Getting last processed block hash.");
        ByteString key = getBlockMarkerKey();
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("key: {0}", toHexString(key)));
        ByteString value = retrieve(key);
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("value: {0}", toHexString(value)));

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
     * @param hash     The hash of the last processed block.
     * @throws DatabaseException
          When the data could not be stored.
     */
    public synchronized void setLastProcessedBlockHash(@NotNull Hash hash) throws DatabaseException {
        LOGGER.log(Level.FINE, "Sets the hash of the last block up to which the UTXO set is up-to-date.");
        ByteString key = getBlockMarkerKey();
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("key: {0}", toHexString(key)));
        ByteString value = getRawProtoValue(hash, BrabocoinProtos.Hash.class);
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("value: {0}", toHexString(value)));

        store(key, value);
    }

    private synchronized ByteString getBlockMarkerKey() {
        LOGGER.log(Level.FINE, "Block marker key value: {0}", toHexString(KEY_BLOCK_MARKER));
        return KEY_BLOCK_MARKER;
    }
}
