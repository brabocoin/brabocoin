package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 * <p>
 * The referenced outputs must be present in the UTXO set.
 */
@ValidationRule(name="Valid inputs", failedName = "Transaction double spends an output", description = "The outputs referenced by the inputs of the transaction should be unspent.")
public class ValidInputUTXOTxRule extends TransactionRule {

    private ReadonlyUTXOSet utxoSet;
    @DescriptionField
    private boolean inputsUnspent;

    public boolean isValid() {
        inputsUnspent = transaction.getInputs()
            .stream()
            .allMatch(i -> {
                try {
                    return utxoSet.isUnspent(i);
                }
                catch (DatabaseException e) {
                    e.printStackTrace();
                    return false;
                }
            });
        return inputsUnspent;
    }
}
