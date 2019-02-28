package org.brabocoin.brabocoin.gui.view;

import javafx.scene.control.TreeItem;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.rule.Rule;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.brabocoin.brabocoin.validation.transaction.rules.DuplicatePoolTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.PoolDoubleSpendingTxRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class TransactionValidationView extends ValidationView<Transaction> {

    private Transaction transaction;
    private TransactionValidator validator;

    private static final RuleList transactionRules = TransactionValidator.ALL;

    private static final RuleList skippedTransactionRules = new RuleList(
        DuplicatePoolTxRule.class,
        PoolDoubleSpendingTxRule.class
    );

    public TransactionValidationView(@NotNull Transaction transaction, @NotNull
        TransactionValidator validator) {
        super(validator, transaction, new TransactionDetailView(transaction, null));
        this.transaction = transaction;
        this.validator = validator;
    }

    @Override
    protected RuleList getRules() {
        return transactionRules;
    }

    @Override
    protected RuleList getSkippedRules() {
        return skippedTransactionRules;
    }

    @Override
    protected BiConsumer<Transaction, RuleList> getValidator() {
        return (t, r) -> validator.validate(t, r, true);
    }

    @Override
    protected @Nullable TreeItem<String> findTreeItem(List<TreeItem<String>> treeItems, Rule rule,
                                                      RuleBook ruleBook) {
        if (rule instanceof TransactionRule) {
            if (treeItems.size() != 1) {
                throw new IllegalStateException(
                    "Found not exactly one rule tree items for transactions");
            }

            return treeItems.get(0);
        }
        else {
            throw new IllegalStateException("Rule of invalid type");
        }
    }

    @Override
    protected List<TreeItem<String>> addCompositeRuleChildren(Class<? extends Rule> rule,
                                                              RuleList composite) {
        return Collections.emptyList();
    }
}
