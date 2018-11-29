package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Uses multiple UTXO sets which are used as fallback (in order).
 */
public class CompositeReadonlyUTXOSet implements ReadonlyUTXOSet {

    private final @NotNull List<ReadonlyUTXOSet> utxoSets;

    public CompositeReadonlyUTXOSet(@NotNull List<ReadonlyUTXOSet> utxoSets) {
        this.utxoSets = new ArrayList<>(utxoSets);
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

}
