package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;
import static org.fusesource.leveldbjni.JniDBFactory.factory;

/**
 * LevelDB Data Access Layer for the LevelDB Java Native Interface provided by fusesource.
 */
public class LevelDB implements KeyValueStore {
    private static final java.util.logging.Logger LOGGER = Logger.getLogger(LevelDB.class.getName());
    private DB database;
    private File databasePath;
    private Options options;

    /**
     * Construct a LevelDB Data Access Layer.
     *
     * @param databasePath The path to store the database in.
     */
    public LevelDB(final File databasePath) {
        options = new Options();
        options.createIfMissing(true);

        this.databasePath = databasePath;
        if (!databasePath.exists()) {
            databasePath.mkdirs();
        }
    }

    @Override
    public void open() throws IOException {
        LOGGER.fine("LevelDB open called.");
        database = factory.open(databasePath, options);
    }

    @Override
    public void close() throws IOException {
        LOGGER.fine("LevelDB close called.");
        database.close();
    }

    @Override
    public void put(final ByteString key, final ByteString value) throws DatabaseException {
        LOGGER.fine("Putting key-value pair.");
        LOGGER.log(Level.FINEST, "key: {0}", toHexString(key));
        LOGGER.log(Level.FINEST, "value: {0}", toHexString(value));
        try {
            database.put(key.toByteArray(), value.toByteArray());
        } catch (final DBException e) {
            LOGGER.log(Level.SEVERE, "Exception when putting key-value pair: {0}", e.getMessage());
            throw new DatabaseException(e.getMessage());
        }
    }

    @Override
    public ByteString get(final ByteString key) throws DatabaseException {
        LOGGER.fine("Getting value using key.");
        LOGGER.log(Level.FINEST, "key: {0}", toHexString(key));
        try {
            byte[] data = database.get(key.toByteArray());
            if (data == null) {
                LOGGER.finest("Key not found in store.");
                return null;
            }

            ByteString byteString = ByteString.copyFrom(data);
            LOGGER.log(Level.FINEST, "Value found: {0}", toHexString(byteString));
            return byteString;
        } catch (final DBException e) {
            LOGGER.log(Level.SEVERE, "Exception while getting value: {0}", e.getMessage());
            throw new DatabaseException(e.getMessage());
        }
    }

    @Override
    public void delete(final ByteString key) throws DatabaseException {
        LOGGER.fine("Deleting key-value pair using key.");
        LOGGER.log(Level.FINEST, "key: {0}", toHexString(key));
        try {
            database.delete(key.toByteArray());
        } catch (final DBException e) {
            LOGGER.log(Level.SEVERE, "Exception while deleting key-value pair: {0}", e.getMessage());
            throw new DatabaseException(e.getMessage());
        }
    }

    @Override
    public boolean has(ByteString key) {
        LOGGER.fine("Checking whether store has key-value pair using key.");
        LOGGER.log(Level.FINEST, "key: {0}", toHexString(key));
        final boolean hasKey = database.get(key.toByteArray()) != null;
        LOGGER.log(Level.FINEST, "found: {0}", hasKey);
        return hasKey;
    }

    @Override
    public Iterator<Map.Entry<ByteString, ByteString>> iterator() {
        LOGGER.fine("LevelDB iterator constructor.");
        return new Iterator<Map.Entry<ByteString, ByteString>>() {
            DBIterator iterator = database.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Map.Entry next() {
                return iterator.next();
            }
        };
    }
}
