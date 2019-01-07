package org.brabocoin.brabocoin.validation.transaction;

import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;

public class TransactionUtil {

    public static long computeFee(Transaction transaction,
                                  ReadonlyUTXOSet utxoSet) throws DatabaseException {
        long outputSum = 0L;
        for (Output output : transaction.getOutputs()) {
            outputSum += output.getAmount();
        }

        long inputSum = 0L;
        for (Input input : transaction.getInputs()) {
            UnspentOutputInfo unspentOutputInfo;
            unspentOutputInfo = utxoSet.findUnspentOutputInfo(input);

            if (unspentOutputInfo == null) {
                throw new IllegalStateException(
                    "Could not find unspent output info for referenced output.");
            }

            inputSum += unspentOutputInfo.getAmount();
        }

        return inputSum - outputSum;
    }
}
