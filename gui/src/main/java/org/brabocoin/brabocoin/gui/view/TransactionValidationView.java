package org.brabocoin.brabocoin.gui.view;

import javafx.scene.control.TreeItem;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.consensus.Consensus;
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
    private final boolean withRevertedUTXO;

    private static final RuleList transactionRules = TransactionValidator.ALL;

    private static final RuleList skippedTransactionRules = new RuleList(
        DuplicatePoolTxRule.class,
        PoolDoubleSpendingTxRule.class
    );

    public TransactionValidationView(@NotNull Transaction transaction,
                                     @NotNull Blockchain blockchain,
                                     @NotNull Consensus consensus,
                                     @NotNull TransactionValidator validator,
                                     boolean withRevertedUTXO) {
        super(
            validator,
            transaction,
            blockchain,
            consensus,
            new TransactionDetailView(blockchain, consensus, transaction, null, withRevertedUTXO)
        );
        this.transaction = transaction;
        this.validator = validator;
        this.withRevertedUTXO = withRevertedUTXO;
    }

    @Override
    protected IndexedBlock getRevertedUTXODestinationBlock(Transaction subject) {
        try {
            for (int i = 0; i <= blockchain.getMainChain().getHeight(); i++) {
                if (blockchain.getBlock(blockchain.getMainChain().getBlockAtHeight(i))
                    .getTransactions()
                    .stream()
                    .anyMatch(t -> t.getHash().equals(subject.getHash()))) {
                    return blockchain.getMainChain().getBlockAtHeight(i);
                }
            }
        }
        catch (DatabaseException e) {
            return null;
        }
        return null;
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
    protected BiConsumer<Transaction, RuleList> applyValidator() {
        if (!withRevertedUTXO) {
            return (t, r) -> validator.validate(t, r, true);
        }

        TransactionValidator revertedUTXOValidator = getRevertedUTXOValidator(
            validator,
            transaction
        );

        return (t, r) -> {
            revertedUTXOValidator.addListener(this);
            revertedUTXOValidator.validate(t, r, true);
            revertedUTXOValidator.removeListener(this);
        };
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
