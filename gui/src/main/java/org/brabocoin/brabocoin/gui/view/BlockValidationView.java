package org.brabocoin.brabocoin.gui.view;

import javafx.scene.control.TreeItem;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.block.BlockRule;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.block.rules.ContextualTransactionCheckBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.DuplicateStorageBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.LegalTransactionFeesBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.UniqueUnspentCoinbaseBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.ValidCoinbaseOutputAmountBlkRule;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.Rule;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class BlockValidationView extends ValidationView<Block> {

    private Block block;
    private Blockchain blockchain;
    private BlockValidator validator;

    private static final RuleList blockRules = new RuleList(
        BlockValidator.INCOMING_BLOCK,
        BlockValidator.CONNECT_TO_CHAIN
    );

    private static final RuleList skippedBlockRules = new RuleList(
        DuplicateStorageBlkRule.class,
        UniqueUnspentCoinbaseBlkRule.class,
        ContextualTransactionCheckBlkRule.class,
        LegalTransactionFeesBlkRule.class,
        ValidCoinbaseOutputAmountBlkRule.class
    );

    public BlockValidationView(@NotNull Blockchain blockchain, @NotNull Block block,
                               @NotNull BlockValidator validator) {
        super(validator, block, new BlockDetailView(blockchain, block, null));
        this.block = block;
        this.blockchain = blockchain;
        this.validator = validator;
    }

    @Override
    protected RuleList getRules() {
        return blockRules;
    }

    @Override
    protected RuleList getSkippedRules() {
        return skippedBlockRules;
    }

    @Override
    protected BiConsumer<Block, RuleList> getValidator() {
        return (b, r) -> validator.validate(b, r);
    }

    @Override
    protected @Nullable TreeItem<String> findTreeItem(List<TreeItem<String>> treeItems, Rule rule, RuleBook ruleBook) {
        if (rule instanceof TransactionRule) {
            FactMap map = ruleBook.getFacts();
            Transaction tx = (Transaction)map.get("transaction");

            List<TreeItem<String>> treeItemParents = treeItems.stream().map(
                TreeItem::getParent
            ).collect(Collectors.toList());

            TreeItem<String> parent = treeItemParents.stream()
                .filter(t -> transactionItemMap.get(t).getHash().equals(tx.getHash()))
                .findFirst()
                .orElse(null);

            return treeItems.stream()
                .filter(t -> t.getParent().equals(parent))
                .findFirst()
                .orElse(null);
        }
        else if (rule instanceof BlockRule) {
            if (treeItems.size() != 1) {
                throw new IllegalStateException(
                    "Found not exactly one rule tree items for blocks");
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
        List<TreeItem<String>> treeItems = new ArrayList<>();

        List<Transaction> transactions = block.getTransactions();
        for (int i = 0; i < transactions.size(); i++) {
            Transaction tx = transactions.get(i);
            TreeItem<String> txItem = new TreeItem<>();
            descriptionItemMap.put(txItem, "");
            transactionItemMap.put(txItem, tx);

            if (tx.isCoinbase()) {
                txItem.setValue("Coinbase transaction");
                txItem.setGraphic(createIcon(RuleState.SKIPPED));
                txItem.setExpanded(false);
                addRules(txItem, composite, true);
            }
            else {
                txItem.setValue("Transaction " + i);
                txItem.setGraphic(
                    isSkippedRuleClass(rule) ?
                        createIcon(RuleState.SKIPPED) : createIcon(RuleState.PENDING)
                );
                txItem.setExpanded(true);

                addRules(txItem, composite,
                    isSkippedRuleClass(rule)
                );
            }

            treeItems.add(txItem);
        }

        return treeItems;
    }
}
