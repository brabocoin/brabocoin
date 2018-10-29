package org.brabocoin.brabocoin.util.collection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Maintains multiple indices of values that have an dependence on multiple objects within the
 * value range.
 * <p>
 * A primary key index is maintained for every value, as well as an dependence index for every
 * dependency.
 *
 * @param <K>
 *     The key for the primary index.
 * @param <V>
 *     The value.
 * @param <D>
 *     The dependency.
 */
public class MultiDependenceIndex <K, V, D> {

    /**
     * Primary key index.
     */
    protected final Map<K, V> primaryIndex;

    /**
     * Queue of keys maintaining the order in which keys were added to the index.
     */
    protected final List<K> keyList;

    /**
     * Dependency index.
     */
    protected final Multimap<D, V> dependenceIndex;

    /**
     * Calculates the key from the value.
     */
    protected final Function<V, K> keySupplier;

    /**
     * Retrieves the dependencies for a given value.
     */
    protected final Function<V, Iterable<D>> multiDependency;

    /**
     * Creates a new index.
     *
     * @param keySupplier
     *     Calculates the key from the value. Note that keys must be unique for every value.
     * @param multiDependency
     *     Retrieves the dependencies for a given value.
     */
    public MultiDependenceIndex(Function<V, K> keySupplier,
                                Function<V, Iterable<D>> multiDependency) {
        this.primaryIndex = new HashMap<>();
        this.dependenceIndex = HashMultimap.create();
        this.keyList = new ArrayList<>();
        this.keySupplier = keySupplier;
        this.multiDependency = multiDependency;
    }

    /**
     * Stores a value in the index.
     * <p>
     * Keys must be If the key corresponding to the value is already present in the index, the
     * new value is not stored instead. The original data is maintained.
     *
     * @param value
     *     The value to store.
     * @throws IllegalArgumentException
     *     When the key corresponding to the value already exists in the index.
     */
    public void put(V value) {
        K key = keySupplier.apply(value);
        if (primaryIndex.containsKey(key)) {
            throw new IllegalArgumentException(
                "The computed key of the supplied value already exists in the index.");
        }

        primaryIndex.put(key, value);
        keyList.add(key);

        for (D dependency : multiDependency.apply(value)) {
            dependenceIndex.put(dependency, value);
        }
    }

    /**
     * Checks whether the key is contained in the index.
     *
     * @param key
     *     The key.
     * @return Whether the key is contained in the index.
     */
    public boolean containsKey(K key) {
        return primaryIndex.containsKey(key);
    }

    /**
     * Retrieves the value from the given key.
     *
     * @param key
     *     The key.
     * @return The value corresponding to the key in the index, or {@code null} if the key is not
     * present.
     */
    public V get(K key) {
        return primaryIndex.get(key);
    }

    /**
     * Retrieves all values that depend on {@code dependency}.
     *
     * @param dependency
     *     The dependency.
     * @return All values that depend on {@code dependency}, or an empty collection if no such
     * values exist.
     */
    public Collection<V> getFromDependency(D dependency) {
        return dependenceIndex.get(dependency);
    }

    /**
     * Removes the value from the index.
     *
     * @param value
     *     The value
     * @return The removed value, or {@code null} if the value could not be found.
     */
    public V removeValue(V value) {
        return removeKey(keySupplier.apply(value));
    }

    /**
     * Removes the value for the given key from the index.
     *
     * @param key
     *     The key.
     * @return The removed value, or {@code null} if the key could not be found.
     */
    public V removeKey(K key) {
        V value = primaryIndex.remove(key);

        if (value == null) {
            return null;
        }

        keyList.remove(key);

        for (D dependency : multiDependency.apply(value)) {
            dependenceIndex.remove(dependency, value);
        }

        return value;
    }

    /**
     * The number of values stored in the index.
     *
     * @return The number of values stored in this index.
     */
    public int size() {
        return primaryIndex.size();
    }

    /**
     * Get the key at the given index.
     *
     * @param index
     *     The index.
     * @return The key at the given index.
     * @throws IndexOutOfBoundsException
     *     If no key at the given position exists.
     */
    public K getKeyAt(int index) {
        return keyList.get(index);
    }
}
