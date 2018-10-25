package org.brabocoin.brabocoin.util.collection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;

/**
 * Maintains multiple indices of values that have an dependence on multiple objects within the
 * value range, where the dependency can be recursively resolved over the stored index.
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
public class RecursiveMultiDependenceIndex <K, V, D> {

    /**
     * Primary key index.
     */
    private final Map<K, V> primaryIndex;

    /**
     * Dependency index.
     */
    private final Multimap<D, V> dependenceIndex;

    /**
     * Calculates the key from the value.
     */
    private final Function<V, K> keySupplier;

    /**
     * Retrieves the dependencies for a given value.
     */
    private final Function<V, Iterable<D>> multiDependency;

    /**
     * Links the value to a dependency object, to traverse the dependencies within the index
     * recursively.
     */
    private final Function<V, D> recursiveLink;

    /**
     * Creates a new index.
     *
     * @param keySupplier
     *     Calculates the key from the value. Note that keys must be unique for every value.
     * @param multiDependency
     *     Retrieves the dependencies for a given value.
     * @param recursiveLink
     *     Links the value to a dependency object, to traverse the dependencies
     *     within the index recursively.
     */
    public RecursiveMultiDependenceIndex(Function<V, K> keySupplier,
                                         Function<V, Iterable<D>> multiDependency,
                                         Function<V, D> recursiveLink) {
        this.primaryIndex = new HashMap<>();
        this.dependenceIndex = HashMultimap.create();
        this.keySupplier = keySupplier;
        this.multiDependency = multiDependency;
        this.recursiveLink = recursiveLink;
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

        primaryIndex.put(keySupplier.apply(value), value);

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

        for (D dependency : multiDependency.apply(value)) {
            dependenceIndex.remove(dependency, value);
        }

        return value;
    }

    /**
     * Recursively find all dependent values from the given initial dependency that match a
     * specific condition.
     * <p>
     * Only children of matching parents are included: when a value does not match, it is not
     * included and the children are not checked.
     *
     * @param initialDependency
     *     The initial dependency to find matching values for.
     * @param matchingFunction
     *     The matching condition on which dependent values are found.
     * @return The recursively matching dependent values.
     */
    public List<V> findMatchingDependants(D initialDependency,
                                          Function<V, Boolean> matchingFunction) {
        List<V> dependants = new ArrayList<>();

        Queue<D> queue = new ArrayDeque<>();
        queue.add(initialDependency);

        while (!queue.isEmpty()) {
            D dependency = queue.remove();
            Collection<V> values = dependenceIndex.get(dependency);
            for (V value : values) {
                if (matchingFunction.apply(value)) {
                    dependants.add(value);
                    queue.add(recursiveLink.apply(value));
                }
            }
        }

        return dependants;
    }

    /**
     * Remove all recursively dependent values from the given initial dependency that match a
     * specific condition.
     *
     * @param initialDependency
     *     The initial dependency to find matching values for.
     * @param matchingFunction
     *     The matching condition on which dependent values are found.
     * @return The recursively matching dependent values that are removed from the index.
     * @see RecursiveMultiDependenceIndex#findMatchingDependants(Object, Function)
     */
    public List<V> removeMatchingDependants(D initialDependency,
                                            Function<V, Boolean> matchingFunction) {
        List<V> dependants = findMatchingDependants(initialDependency, matchingFunction);

        for (V value : dependants) {
            removeValue(value);
        }

        return dependants;
    }
}
