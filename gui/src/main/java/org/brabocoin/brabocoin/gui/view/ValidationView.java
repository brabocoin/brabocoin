package org.brabocoin.brabocoin.gui.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.glyph.BraboGlyph;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.ValidationListener;
import org.brabocoin.brabocoin.validation.Validator;
import org.brabocoin.brabocoin.validation.annotation.CompositeRuleList;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.block.rules.DuplicateStorageBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.UniqueUnspentCoinbaseBlkRule;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.Rule;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.controlsfx.control.MasterDetailPane;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ValidationView extends MasterDetailPane implements BraboControl, Initializable,
                                                                ValidationListener {

    private final Map<Class, List<TreeItem<String>>> ruleTreeItemMap;
    private final Map<TreeItem, Transaction> transactionItemMap;
    private final Validator validator;
    private TransactionDetailView transactionDetailView;
    private BlockDetailView blockDetailView;
    private Transaction transaction;
    private Block block;

    private static final RuleList skippedBlockRules = new RuleList(
        DuplicateStorageBlkRule.class,
        UniqueUnspentCoinbaseBlkRule.class
    );

    private static final RuleList skippedTransactionRules = new RuleList(
        Collections.emptyList()
    );

    private static final RuleList blockRules = new RuleList(
        BlockValidator.INCOMING_BLOCK,
        BlockValidator.CONNECT_TO_CHAIN
    );
    private static final RuleList transactionRules = TransactionValidator.ALL;

    @FXML
    public TreeView<String> ruleView;

    private void loadRules() {
        TreeItem<String> root = new TreeItem<>();
        ruleView.setShowRoot(false);

        RuleList ruleList;
        if (isForBlock()) {
            ruleList = blockRules;
        }
        else {
            ruleList = transactionRules;
        }
        addRules(root, ruleList, false);
        ruleView.setRoot(root);
    }

    private boolean isSkippedRuleClass(Class rule) {
        return (isForBlock() && skippedBlockRules.getRules().contains(rule)) ||
            (!isForBlock() && skippedTransactionRules.getRules().contains(rule));
    }

    private void addRules(TreeItem<String> node, RuleList ruleList, boolean ignored) {
        for (Class rule : ruleList) {
            TreeItem<String> ruleTreeItem = new TreeItem<>();
            if (ignored) {
                ruleTreeItem.setGraphic(new BraboGlyph(BraboGlyph.Icon.CIRCLEMINUS));
            } else {
                ruleTreeItem.setGraphic(new BraboGlyph(BraboGlyph.Icon.CIRCLE));
            }

            Annotation annotation = rule.getAnnotation(ValidationRule.class);
            if (annotation instanceof ValidationRule) {
                ValidationRule validationRule = (ValidationRule)annotation;
                ruleTreeItem.setValue(validationRule.name());

                if (isSkippedRuleClass(rule)) {
                    ruleTreeItem.setGraphic(new BraboGlyph(BraboGlyph.Icon.CIRCLEMINUS));
                }
                else if (validationRule.composite()) {
                    RuleList composite = null;
                    for (Field field : rule.getDeclaredFields()) {
                        field.setAccessible(true);
                        if (field.getAnnotation(CompositeRuleList.class) != null) {
                            try {
                                composite = (RuleList)field.get(rule.newInstance());
                            }
                            catch (IllegalAccessException | InstantiationException e) {
                                break;
                            }
                        }
                    }

                    if (composite == null) {
                        ruleTreeItem.setValue("[Could not get composite rule list]");
                        continue;
                    }

                    List<Transaction> transactions = block.getTransactions();
                    for (int i = 0; i < transactions.size(); i++) {
                        Transaction tx = transactions.get(i);
                        TreeItem<String> txItem = new TreeItem<>();
                        transactionItemMap.put(txItem, tx);

                        if (tx.isCoinbase()) {
                            txItem.setValue("Coinbase");
                            txItem.setGraphic(new BraboGlyph(BraboGlyph.Icon.CIRCLEMINUS));
                            txItem.setExpanded(false);
                            addRules(txItem, composite, true);
                        }
                        else {
                            txItem.setValue("Transaction " + i);
                            txItem.setGraphic(new BraboGlyph(BraboGlyph.Icon.CIRCLE));
                            txItem.setExpanded(true);

                            addRules(txItem, composite, false);
                        }

                        ruleTreeItem.getChildren().add(txItem);
                    }

                    ruleTreeItem.setExpanded(true);
                }
            }
            else {
                ruleTreeItem.setValue("[Could not determine annotation type]");
            }

            if (ruleTreeItemMap.containsKey(rule)) {
                ruleTreeItemMap.get(rule).add(ruleTreeItem);
            }
            else {
                ruleTreeItemMap.put(rule, new ArrayList<TreeItem<String>>() {{
                    add(ruleTreeItem);
                }});
            }
            node.getChildren().add(ruleTreeItem);
        }
    }

    public ValidationView(@NotNull Transaction transaction, @NotNull
        Validator<Transaction> validator) {
        super();

        ruleTreeItemMap = new HashMap<>();
        transactionItemMap = new HashMap<>();
        this.transaction = transaction;
        this.validator = validator;

        transactionDetailView = new TransactionDetailView(transaction);
        this.setDetailNode(transactionDetailView);

        BraboControlInitializer.initialize(this);
    }

    public ValidationView(@NotNull Blockchain blockchain, @NotNull Block block,
                          @NotNull Validator<Block> validator) {
        super();

        ruleTreeItemMap = new HashMap<>();
        transactionItemMap = new HashMap<>();
        this.block = block;
        this.validator = validator;

        blockDetailView = new BlockDetailView(blockchain, block, null);
        this.setDetailNode(blockDetailView);

        BraboControlInitializer.initialize(this);
    }

    private boolean isForBlock() {
        return block != null;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadRules();

        ValidationView me = this;
        Platform.runLater(() -> {
            synchronized (validator) {
                validator.addListener(this);
                // Run validator
                if (isForBlock()) {
                    BlockValidator validator = (BlockValidator)me.validator;

                    List<Class<? extends Rule>> rules = blockRules.getRules();
                    rules.removeAll(skippedBlockRules.getRules());
                    validator.validate(block, new RuleList(rules));
                }
                else {
                    TransactionValidator validator = (TransactionValidator)me.validator;

                    List<Class<? extends Rule>> rules = transactionRules.getRules();
                    rules.removeAll(skippedTransactionRules.getRules());
                    validator.validate(transaction, new RuleList(rules), true);
                }
                validator.removeListener(this);
            }
        });
    }

    @Override
    public void onRuleValidation(Rule rule, RuleBookResult result, RuleBook ruleBook) {
        List<TreeItem<String>> treeItems = ruleTreeItemMap.get(rule.getClass());

        TreeItem<String> item;
        if (rule instanceof TransactionRule) {
            if (isForBlock()) {
                FactMap map = ruleBook.getFacts();
                Transaction tx = (Transaction)map.get("transaction");

                List<TreeItem<String>> treeItemParents = treeItems.stream().map(
                    TreeItem::getParent
                ).collect(Collectors.toList());

                TreeItem<String> parent = treeItemParents.stream()
                    .filter(t -> transactionItemMap.get(t).getHash().equals(tx.getHash()))
                    .findFirst()
                    .orElse(null);

                item = treeItems.stream()
                    .filter(t -> t.getParent().equals(parent))
                    .findFirst()
                    .orElse(null);

            }
            else {
                if (treeItems.size() != 1) {
                    throw new IllegalStateException(
                        "Found not exactly one rule tree items for transactions");
                }

                item = treeItems.get(0);
            }
        }
        else if (rule instanceof BlockRule) {
            if (treeItems.size() != 1) {
                throw new IllegalStateException("Found not exactly one rule tree items for blocks");
            }

            item = treeItems.get(0);
        }
        else {
            throw new IllegalStateException("Rule of invalid type");
        }

        if (result.isPassed()) {
            Platform.runLater(() -> {
                item.setGraphic(new BraboGlyph(BraboGlyph.Icon.CHECK));

                if (item.getParent()
                    .getChildren()
                    .stream()
                    .allMatch(t -> ((BraboGlyph)t.getGraphic()).getIcon()
                        .equals(BraboGlyph.Icon.CHECK))) {
                    item.getParent().setGraphic(new BraboGlyph(BraboGlyph.Icon.CHECK));
                }
            });
        }
        else {
            Platform.runLater(() -> item.setGraphic(new BraboGlyph(BraboGlyph.Icon.CROSS)));
        }

        Platform.runLater(() -> ruleView.refresh());
    }
}
