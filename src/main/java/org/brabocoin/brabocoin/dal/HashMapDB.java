package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

public class HashMapDB implements KeyValueStore {
    private static final Logger LOGGER = Logger.getLogger(HashMapDB.class.getName());
    private Map<ByteString, ByteString> map = new HashMap<>();

    @Override
    public void put(ByteString key, ByteString value) {
        LOGGER.fine("Putting key-value pair.");
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("key: {0}", toHexString(key)));
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("value: {0}", toHexString(value)));
        map.put(key, value);
    }

    @Override
    public ByteString get(ByteString key) {
        LOGGER.fine("Getting value using key.");
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("key: {0}", toHexString(key)));
        return map.get(key);
    }

    @Override
    public void delete(ByteString key) {
        LOGGER.fine("Deleting key-value pair using key.");
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("key: {0}", toHexString(key)));
        map.remove(key);
    }

    @Override
    public boolean has(ByteString key) {
        LOGGER.fine("Checking if store has key-value pair using key.");
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("key: {0}", toHexString(key)));
        final boolean hasKey = map.containsKey(key);
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("found: {0}", hasKey));
        return hasKey;
    }

    @Override
    public void open() {

    }

    @Override
    public void close() {

    }

    @Override
    public Iterator<Map.Entry<ByteString, ByteString>> iterator() {
        LOGGER.fine("HashMapDB iterator constructor.");
        return map.entrySet().iterator();
    }
}
