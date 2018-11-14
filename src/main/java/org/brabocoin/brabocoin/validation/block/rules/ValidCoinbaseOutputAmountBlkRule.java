package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.block.BlockRule;

import static org.brabocoin.brabocoin.util.LambdaExceptionUtil.rethrowFunction;

public class ValidCoinbaseOutputAmountBlkRule extends BlockRule {
    private TransactionProcessor transactionProcessor;
    @Override
    public boolean valid() {
        try {
            long feeSum = block.getTransactions()
                    .stream()
                    .map(rethrowFunction(transactionProcessor::computeFee))
                    .mapToLong(l -> l)
                    .sum();

            return block.getCoinbaseTransaction()
                    .getOutputs().get(0).getAmount() <= consensus.getCoinbaseOutputAmount() + feeSum;
        } catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
