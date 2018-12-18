package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRuleUtil;

@ValidationRule(name="Legal transaction fees sum", description = "The sum of all transaction fees is within the allowed range, and does not overflow.")
public class LegalTransactionFeesBlkRule extends BlockRule {

    private ReadonlyUTXOSet utxoSet;

    @Override
    public boolean isValid() {
        long sum = 0;
        for (Transaction transaction : block.getTransactions()) {
            if (transaction.isCoinbase()) {
                continue;
            }

            long fee;
            try {
                fee = TransactionRuleUtil.computeFee(transaction, utxoSet);
            }
            catch (DatabaseException e) {
                e.printStackTrace();
                return false;
            }

            if (fee <= 0) {
                return false;
            }

            try {
                sum = Math.addExact(sum, fee);
            }
            catch (ArithmeticException e) {
                // Sum overflows long type
                return false;
            }

            if (sum > consensus.getMaxMoneyValue()) {
                return false;
            }
        }

        return true;
    }
}
