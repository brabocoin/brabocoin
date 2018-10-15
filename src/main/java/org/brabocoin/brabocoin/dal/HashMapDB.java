package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.exceptions.DatabaseException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HashMapDB implements KeyValueStore {
    private Map<ByteString, ByteString> map = new HashMap<>();

    @Override
    public void put(ByteString key, ByteString value) throws DatabaseException {
        map.put(key, value);
    }

    @Override
    public ByteString get(ByteString key) throws DatabaseException {
        return map.get(key);
    }

    @Override
    public void delete(ByteString key) throws DatabaseException {
        map.remove(key);
    }

    @Override
    public boolean has(ByteString key) throws DatabaseException {
        return map.containsKey(key);
    }

    @Override
    public void open() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public Iterator<Map.Entry<ByteString, ByteString>> iterator() {
        return map.entrySet().iterator();
    }
}
