package org.brabocoin.brabocoin.validation;

import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface Validator extends ValidationListener {
    List<ValidationListener> validationListeners = new ArrayList<>();

    /**
     * Add a listener to validation events.
     *
     * @param listener The listener to add.
     */
    default void addListener(@NotNull ValidationListener listener) {
        this.validationListeners.add(listener);
    }

    /**
     * Remove a listener to validation events.
     *
     * @param listener The listener to remove.
     */
    default void removeListener(@NotNull ValidationListener listener) {
        this.validationListeners.remove(listener);
    }

    Validator withUTXOSet(@NotNull ReadonlyUTXOSet utxoSet);
}
