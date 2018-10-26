package org.brabocoin.brabocoin.util.collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test {@link RecursiveMultiDependenceIndex} with the following concrete use case:
 * Create an index of sentences in which every sentence depends directly on its prefix without
 * its last word.
 * <p>
 * For example, the sentence {@code "out of the blue sky"} directly depends on {@code "out of the
 * blue"}.
 * <p>
 * The sentences are indexed by their string value (hence every string is unique).
 */
class RecursiveMultiDependenceIndexTest {

    private RecursiveMultiDependenceIndex<String, String, String> index;

    @BeforeEach
    void setUp() {
        index = new RecursiveMultiDependenceIndex<>(
            Function.identity(),
            string -> Collections.singleton(string.substring(0, string.lastIndexOf(' '))),
            Function.identity()
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

        assertTrue(index.findMatchingDependants("out", s -> true).isEmpty());
    }

    @Test
    void removeKey() {
        index.put("out of the");
        index.removeValue("out of the");
        assertFalse(index.containsKey("out of the"));
        assertNull(index.get("out of the"));

        assertTrue(index.findMatchingDependants("out of", s -> true).isEmpty());
    }

    @Test
    void findAllMatchingDependants() {
        index.put("out of");
        index.put("out of the");
        index.put("out of the blue");
        index.put("out to");
        index.put("to out");

        List<String> matching = index.findMatchingDependants("out", s -> true);
        assertEquals(4, matching.size());
        assertEquals("out of the blue", matching.get(3));
        assertEquals("out of the", matching.get(2));
        assertNotEquals(matching.get(0), matching.get(1));
        assertTrue("out of".equals(matching.get(0)) || "out of".equals(matching.get(1)));
        assertTrue("out to".equals(matching.get(0)) || "out to".equals(matching.get(1)));
    }

    @Test
    void findSomeMatchingDependants() {
        index.put("out of");
        index.put("out of the");
        index.put("out of the blue");
        index.put("out to");
        index.put("to out");

        List<String> matching = index.findMatchingDependants("out", s -> s.split(" ").length % 2 == 0);
        assertEquals(2, matching.size());
        assertNotEquals(matching.get(0), matching.get(1));
        assertTrue("out of".equals(matching.get(0)) || "out of".equals(matching.get(1)));
        assertTrue("out to".equals(matching.get(0)) || "out to".equals(matching.get(1)));
    }

    @Test
    void removeAllMatchingDependents() {
        index.put("out of");
        index.put("out of the");
        index.put("out of the blue");
        index.put("out to");
        index.put("to out");

        List<String> matching = index.removeMatchingDependants("out", s -> true);
        for (String match : matching) {
            assertFalse(index.containsKey(match));
        }

        assertTrue(index.containsKey("to out"));
    }

    @Test
    void removeSomeMatchingDependants() {
        index.put("out of");
        index.put("out of the");
        index.put("out of the blue");
        index.put("out to");
        index.put("to out");

        List<String> matching = index.removeMatchingDependants("out", s -> s.split(" ").length % 2 == 0);

        for (String match : matching) {
            assertFalse(index.containsKey(match));
        }

        assertTrue(index.containsKey("to out"));
        assertTrue(index.containsKey("out of the"));
        assertTrue(index.containsKey("out of the blue"));
    }
}
