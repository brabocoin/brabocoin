package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.iq80.leveldb.*;

import java.io.File;
import java.io.IOException;

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
    public void put(final byte[] key, final byte[] value) throws DatabaseException {
        try {
            database.put(key, value);
        } catch (final DBException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    @Override
    public byte[] get(final byte[] key) throws DatabaseException {
        try {
            return database.get(key);
        } catch (final DBException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    @Override
    public void delete(final byte[] key) throws DatabaseException {
        try {
            database.delete(key);
        } catch (final DBException e) {
            throw new DatabaseException(e.getMessage());
        }
    }
}
