package org.brabocoin.brabocoin.dal;

import com.google.common.collect.Iterators;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uses multiple UTXO sets which are used as fallback (in order).
 */
public class CompositeReadonlyUTXOSet implements ReadonlyUTXOSet, UTXOSetListener, Iterable<Map.Entry<Input,
    UnspentOutputInfo>> {

    private static final Logger LOGGER = Logger.getLogger(CompositeReadonlyUTXOSet.class.getName());

    private final @NotNull List<ReadonlyUTXOSet> utxoSets;
    private final @NotNull Set<UTXOSetListener> listeners;

    public CompositeReadonlyUTXOSet(@NotNull List<ReadonlyUTXOSet> utxoSets) {
        this.utxoSets = new ArrayList<>(utxoSets);
        this.listeners = new HashSet<>();

        this.utxoSets.forEach(set -> set.addListener(this));
    }

    public CompositeReadonlyUTXOSet(ReadonlyUTXOSet... utxoSets) {
        this(Arrays.asList(utxoSets));
    }

    @Override
    public synchronized boolean isUnspent(@NotNull Input input) throws DatabaseException {
        for (ReadonlyUTXOSet utxoSet : utxoSets) {
            if (utxoSet.isUnspent(input)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public synchronized boolean isUnspent(@NotNull Hash transactionHash,
                             int outputIndex) throws DatabaseException {
        for (ReadonlyUTXOSet utxoSet : utxoSets) {
            if (utxoSet.isUnspent(transactionHash, outputIndex)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public synchronized @Nullable UnspentOutputInfo findUnspentOutputInfo(@NotNull Input input) throws DatabaseException {
        for (ReadonlyUTXOSet utxoSet : utxoSets) {
            UnspentOutputInfo info = utxoSet.findUnspentOutputInfo(input);
            if (info != null) {
                return info;
            }
        }

        return null;
    }

    @Override
    public synchronized @Nullable UnspentOutputInfo findUnspentOutputInfo(@NotNull Hash transactionHash,
                                                             int outputIndex) throws DatabaseException {
        for (ReadonlyUTXOSet utxoSet : utxoSets) {
            UnspentOutputInfo info = utxoSet.findUnspentOutputInfo(transactionHash, outputIndex);
            if (info != null) {
                return info;
            }
        }

        return null;
    }

    @Override
    public void onOutputUnspent(@NotNull Hash transactionHash, int outputIndex,
                                @NotNull UnspentOutputInfo info) {
        listeners.forEach(l -> l.onOutputUnspent(transactionHash, outputIndex, info));
    }

    @Override
    public void onOutputSpent(@NotNull Hash transactionHash, int outputIndex) {
        // Check if output is actually spent in all sets in the composite set
        try {
            if (!this.isUnspent(transactionHash, outputIndex)) {
                listeners.forEach(l -> l.onOutputSpent(transactionHash, outputIndex));
            }
        }
        catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Could not read UTXO database.", e);
            throw new RuntimeException("Could not read UTXO database.", e);
        }
    }

    @Override
    public void addListener(@NotNull UTXOSetListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(@NotNull UTXOSetListener listener) {
        listeners.remove(listener);
    }

    @NotNull
    @Override
    public Iterator<Map.Entry<Input, UnspentOutputInfo>> iterator() {
        //noinspection unchecked
        return Iterators.concat(
            utxoSets.stream()
                .map(Iterable::iterator)
                .toArray(Iterator[]::new)
        );
    }
}
