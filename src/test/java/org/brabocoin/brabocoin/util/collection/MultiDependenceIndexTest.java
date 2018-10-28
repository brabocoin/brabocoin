package org.brabocoin.brabocoin.util.collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sten Wessel
 */
public class MultiDependenceIndexTest {

    private MultiDependenceIndex<String, String, String> index;

    @BeforeEach
    void setUp() {
        index = new MultiDependenceIndex<>(
            Function.identity(),
            string -> Collections.singleton(string.substring(0, string.lastIndexOf(' ')))
        );
    }

    @Test
    void putAndGet() {
        index.put("out of");
        assertEquals("out of", index.get("out of"));
    }

    @Test
    void getNull() {
        assertNull(index.get("nonexistent key"));
    }

    @Test
    void containsKey() {
        index.put("out of");
        assertTrue(index.containsKey("out of"));
    }

    @Test
    void containsKeyFalse() {
        assertFalse(index.containsKey("nonexistent key"));
    }

    @Test
    void removeValue() {
        index.put("out of");
        index.removeValue("out of");
        assertFalse(index.containsKey("out of"));
        assertNull(index.get("out of"));

        assertTrue(index.getFromDependency("out").isEmpty());
    }

    @Test
    void removeKey() {
        index.put("out of the");
        index.removeValue("out of the");
        assertFalse(index.containsKey("out of the"));
        assertNull(index.get("out of the"));

        assertTrue(index.getFromDependency("out of").isEmpty());
    }

    @Test
    void getFromDependency() {
        index.put("out of");
        Collection<String> fromIndex = index.getFromDependency("out");
        assertEquals(1, fromIndex.size());
        assertTrue(fromIndex.contains("out of"));
    }

    @Test
    void getFromDependencyEmpty() {
        Collection<String> fromIndex = index.getFromDependency("out");
        assertTrue(fromIndex.isEmpty());
    }
}
