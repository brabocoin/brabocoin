package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.exceptions.DatabaseException;

import java.io.IOException;
import java.util.Map;

/**
 * Maps a byte array key to a byte array value.
 */
public interface KeyValueStore extends Iterable<Map.Entry<ByteString, ByteString>> {
    /**
     * Put a new key-value in the database.
     *
     * @param key
     *         The key under which the value is store.
     * @param value
     *         The value corresponding to the key.
     * @throws DatabaseException
     *         Exception thrown when the database returns an error.
     */
    void put(final ByteString key, final ByteString value) throws DatabaseException;

    /**
     * Gets a value from the database based on its key.
     *
     * @param key
     *         Key to search the database.
     * @return ByteString value or null if not found.
     * @throws DatabaseException
     *         Exception thrown when the database could not read the value.
     */
    ByteString get(final ByteString key) throws DatabaseException;

    /**
     * Deletes a key-value pair from the database.
     *
     * @param key
     *         The key under which the value is stored.
     * @throws DatabaseException
     *         Exception thrown when the database returns an error.
     */
    void delete(final ByteString key) throws DatabaseException;

    /**
     * Checks whether the key exists.
     *
     * @param key
     *         The key to check.
     * @return Whether the key exists in the database.
     * @throws DatabaseException
     *         When the database returns an error.
     */
    boolean has(ByteString key) throws DatabaseException;

    /**
     * Tries to open the database.
     *
     * @throws IOException
     *         Thrown when the database file can not be opened.
     */
    void open() throws IOException;

    /**
     * Closes the database.
     *
     * @throws IOException
     *         Thrown when the database file can not be written.
     */
    void close() throws IOException;
}
