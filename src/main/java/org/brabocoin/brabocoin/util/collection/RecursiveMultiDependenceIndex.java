package org.brabocoin.brabocoin.util.collection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
public class RecursiveMultiDependenceIndex <K, V, D> extends MultiDependenceIndex<K, V, D> {

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
        super(keySupplier, multiDependency);
        this.recursiveLink = recursiveLink;
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
