package org.brabocoin.brabocoin.validation.transaction;

import com.deliveredtechnologies.rulebook.annotation.Given;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.Consensus;

public class TransactionRule {
    @Given("transaction")
    protected Transaction transaction;

    @Given("consensus")
    protected Consensus consensus;
}
