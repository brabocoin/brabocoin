package org.brabocoin.brabocoin.gui.view;

import javafx.scene.control.TreeItem;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.processor.UTXOProcessor;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.Validator;
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
    private Consensus consensus;
    private boolean withRevertedUTXO;

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

    private static final RuleList skippedBlockRulesWithRevertedUTXO = new RuleList(
        DuplicateStorageBlkRule.class
    );

    public BlockValidationView(@NotNull Blockchain blockchain, @NotNull Block block,
                               @NotNull BlockValidator validator, @NotNull Consensus consensus,
                               boolean withRevertedUTXO) {
        super(validator, block, new BlockDetailView(blockchain, block, null, consensus));
        this.block = block;
        this.blockchain = blockchain;
        this.validator = validator;
        this.consensus = consensus;
        this.withRevertedUTXO = withRevertedUTXO;
    }

    @Override
    protected RuleList getRules() {
        return blockRules;
    }

    @Override
    protected RuleList getSkippedRules() {
        return withRevertedUTXO ? skippedBlockRulesWithRevertedUTXO : skippedBlockRules;
    }

    @Override
    protected BiConsumer<Block, RuleList> getValidator() {
        if (!withRevertedUTXO) {
            return (block, ruleList) -> validator.validate(block, ruleList);
        }

        return this::validateWithRevertedUTXO;
    }

    private void validateWithRevertedUTXO(Block block, RuleList ruleList) {
        try {
            IndexedBlock indexedBlock = blockchain.getIndexedBlock(block.getHash());

            if (indexedBlock != null && blockchain.getMainChain().contains(indexedBlock)) {
                ChainUTXODatabase validationUTXO = new ChainUTXODatabase(
                    new HashMapDB(),
                    consensus
                );
                UTXOProcessor processor = new UTXOProcessor(validationUTXO);

                // Build up validation UTXO
                for (int i = 1; i < indexedBlock.getBlockInfo().getBlockHeight(); i++) {
                    Block prevBlk = blockchain.getBlock(blockchain.getMainChain()
                        .getBlockAtHeight(i));
                    processor.processBlockConnected(prevBlk);
                }

                // Construct new validator
                BlockValidator dummyValidator = validator.withUTXOSet(validationUTXO);

                dummyValidator.addListener(this);
                dummyValidator.validate(block, ruleList);
                dummyValidator.removeListener(this);

                return;
            }
        }
        catch (DatabaseException ignored) {

        }

        validator.validate(block, ruleList);
    }

    @Override
    protected @Nullable TreeItem<String> findTreeItem(List<TreeItem<String>> treeItems, Rule rule,
                                                      RuleBook ruleBook) {
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

                // Hotfix for describing why coinbase tx's are skipped
                if (rule == ContextualTransactionCheckBlkRule.class) {
                    String validatorClassPath = deriveClassPath(Validator.class);
                    // This should really be done differently... :/
                    descriptionItemMap.put(txItem, deriveSkippedDescription(
                        rule,
                        validatorClassPath.substring(
                            0,
                            validatorClassPath.lastIndexOf('/') + 1
                        ) + "CoinbaseContextual.skipped.twig"
                    ));
                }
                else {
                    addRules(txItem, composite, true);
                }
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
