package org.brabocoin.brabocoin.validation.transaction;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.rule.Rule;

public abstract class TransactionRule implements Rule {

    protected Transaction transaction;
    protected Consensus consensus;

}
