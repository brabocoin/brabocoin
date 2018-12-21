package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionUtil;

import static org.brabocoin.brabocoin.util.LambdaExceptionUtil.rethrowFunction;

@ValidationRule(name="Valid coinbase transaction output", description = "The coinbase transaction output is smaller than, or equal to, the maximum block reward defined in consensus, plus the sum of all transaction fees.")
public class ValidCoinbaseOutputAmountBlkRule extends BlockRule {

    private ReadonlyUTXOSet utxoSet;

    @Override
    public boolean isValid() {
        try {
            long feeSum = block.getTransactions()
                .stream()
                .skip(1)
                .map(rethrowFunction(t -> TransactionUtil.computeFee(t, utxoSet)))
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
