package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * UTXO Set.
 */
public interface ReadonlyUTXOSet extends Iterable<Map.Entry<Input,
    UnspentOutputInfo>> {

    /**
     * Checks whether the output referenced by the given transaction input is unspent.
     *
     * @param input
     *     The input to check.
     * @return Whether the input is unspent.
     * @throws DatabaseException
     *     When the output data could not be read.
     */
    boolean isUnspent(@NotNull Input input) throws DatabaseException;

    /**
     * Checks whether the indicated transaction output is unspent.
     *
     * @param transactionHash
     *     The hash of the transaction of the output to check.
     * @param outputIndex
     *     The index of the output in the transaction.
     * @return Whether the output is unspent.
     * @throws DatabaseException
     *     When the output data could not be read.
     */
    boolean isUnspent(@NotNull Hash transactionHash, int outputIndex) throws DatabaseException;

    /**
     * Find the unspent output information for the output referenced by the given input.
     *
     * @param input
     *     The input to get the unspent information from.
     * @return The unspent output information, or {@code null} when the output is already spent.
     * @throws DatabaseException
     *     When the output data could not be read.
     */
    @Nullable UnspentOutputInfo findUnspentOutputInfo(@NotNull Input input) throws DatabaseException;

    /**
     * Find the unspent output information for the indicated output.
     *
     * @param transactionHash
     *     The hash of the transaction of the output
     * @param outputIndex
     *     The index of the output in the transaction.
     * @return The unspent output information, or {@code null} when the output is already spent.
     * @throws DatabaseException
     *     When the output data could not be read.
     */
    @Nullable UnspentOutputInfo findUnspentOutputInfo(@NotNull Hash transactionHash,
                                                      int outputIndex) throws DatabaseException;

    /**
     * Add a UTXO set listener.
     *
     * @param listener
     *     The listener to add.
     */
    void addListener(@NotNull UTXOSetListener listener);

    /**
     * Remove a UTXO set listener.
     *
     * @param listener
     *     The listener to remove.
     */
    void removeListener(@NotNull UTXOSetListener listener);

}
