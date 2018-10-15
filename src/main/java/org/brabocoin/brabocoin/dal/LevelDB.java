package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.iq80.leveldb.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static org.fusesource.leveldbjni.JniDBFactory.*;

/**
 * LevelDB Data Access Layer for the LevelDB Java Native Interface provided by fusesource.
 */
public class LevelDB implements KeyValueStore {
    private DB database;
    private File databaseFile;
    private Options options;

    /**
     * Construct a LevelDB Data Access Layer.
     *
     * @param databaseFile The file to store the database in.
     */
    public LevelDB(final File databaseFile) {
        options = new Options();
        options.createIfMissing(true);

        this.databaseFile = databaseFile;
    }

    @Override
    public void open() throws IOException {
        database = factory.open(databaseFile, options);
    }

    @Override
    public void close() throws IOException {
        database.close();
    }

    @Override
    public void put(final ByteString key, final ByteString value) throws DatabaseException {
        try {
            database.put(key.toByteArray(), value.toByteArray());
        } catch (final DBException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    @Override
    public ByteString get(final ByteString key) throws DatabaseException {
        try {
            byte[] data = database.get(key.toByteArray());
            if (data == null) {
                return null;
            }

            return ByteString.copyFrom(data);
        } catch (final DBException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    @Override
    public void delete(final ByteString key) throws DatabaseException {
        try {
            database.delete(key.toByteArray());
        } catch (final DBException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    @Override
    public boolean has(byte[] key) throws DatabaseException {
        return database.get(key) != null;
    }

    @Override
    public Iterator<Map.Entry<ByteString, ByteString>> iterator() {
        Iterator<Map.Entry<ByteString, ByteString>> it = new Iterator<Map.Entry<ByteString, ByteString>>() {
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
        return it;
    }
}
