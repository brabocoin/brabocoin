package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.block.BlockRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRuleUtil;

import static org.brabocoin.brabocoin.util.LambdaExceptionUtil.rethrowFunction;

public class ValidCoinbaseOutputAmountBlkRule extends BlockRule {

    private ReadonlyUTXOSet utxoSet;

    @Override
    public boolean isValid() {
        try {
            long feeSum = block.getTransactions()
                .stream()
                .skip(1)
                .map(rethrowFunction(t -> TransactionRuleUtil.computeFee(t, utxoSet)))
                .mapToLong(l -> l)
                .sum();

            Transaction coinbase = block.getCoinbaseTransaction();
            if (coinbase == null) {
                return false;
            }
            return coinbase.getOutputs().get(0).getAmount() <= consensus.getBlockReward() + feeSum;
        }
        catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
