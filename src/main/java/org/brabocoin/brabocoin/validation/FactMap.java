package org.brabocoin.brabocoin.validation;

import java.util.HashMap;

public class FactMap extends HashMap<String, Object> {
    @Override
    public Object put(String key, Object value) {
        if (value == null)
        {
            return super.put(key, new UninitializedFact());
        }
        return super.put(key, value);
    }
}
