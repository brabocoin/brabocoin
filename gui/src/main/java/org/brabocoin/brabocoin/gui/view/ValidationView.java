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
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.block.rules.DuplicateStorageBlkRule;
import org.brabocoin.brabocoin.validation.rule.Rule;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleBookPipe;
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.controlsfx.control.MasterDetailPane;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class ValidationView extends MasterDetailPane implements BraboControl, Initializable,
                                                                ValidationListener, RuleBookPipe {

    private final Map<Class, TreeItem<String>> ruleTreeItemMap;
    private final Validator validator;
    private TransactionDetailView transactionDetailView;
    private BlockDetailView blockDetailView;
    private Transaction transaction;
    private Block block;

    private static final RuleList skippedBlockRules = new RuleList(
        DuplicateStorageBlkRule.class
    );

    private static final RuleList skippedTransactionRules = new RuleList(
        Collections.emptyList()
    );

    @FXML
    public TreeView<String> ruleView;

    private void loadRules() {
        TreeItem<String> root = new TreeItem<>();
        ruleView.setShowRoot(false);

        RuleList ruleList;
        if (isForBlock()) {
            ruleList = BlockValidator.INCOMING_BLOCK;
        }
        else {
            ruleList = TransactionValidator.ALL;
        }
        addRules(root, ruleList);
        ruleView.setRoot(root);
    }

    private boolean isSkippedRuleClass(Class rule) {
        return (isForBlock() && skippedBlockRules.getRules().contains(rule)) ||
            (!isForBlock() && skippedTransactionRules.getRules().contains(rule));
    }

    private void addRules(TreeItem<String> node, RuleList ruleList) {
        for (Class rule : ruleList) {
            TreeItem<String> ruleTreeItem = new TreeItem<>();
            ruleTreeItem.setGraphic(new BraboGlyph(BraboGlyph.Icon.CIRCLE));

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

                    addRules(ruleTreeItem, composite);
                    ruleTreeItem.setExpanded(true);
                }
            }
            else {
                ruleTreeItem.setValue("[Could not determine annotation type]");
            }

            ruleTreeItemMap.put(rule, ruleTreeItem);
            node.getChildren().add(ruleTreeItem);
        }
    }

    public ValidationView(@NotNull Transaction transaction, @NotNull
        Validator<Transaction> validator) {
        super();

        ruleTreeItemMap = new HashMap<>();
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
                validator.addRuleBookPipe(this);
                // Run validator
                if (isForBlock()) {
                    BlockValidator validator = (BlockValidator)me.validator;
                    validator.checkIncomingBlockValid(block);
                }
                else {
                    TransactionValidator validator = (TransactionValidator)me.validator;
                    validator.checkTransactionValid(transaction);
                }
                validator.removeRuleBookPipe(this);
                validator.removeListener(this);
            }
        });
    }

    @Override
    public void onRuleValidation(Rule rule, RuleBookResult result) {
        TreeItem<String> treeItem = ruleTreeItemMap.get(rule.getClass());

        if (treeItem == null) {
            // TODO: HELP!
            return;
        }

        if (isSkippedRuleClass(rule.getClass())) {
            return;
        }

        if (result.isPassed()) {
            Platform.runLater(() -> treeItem.setGraphic(new BraboGlyph(BraboGlyph.Icon.CHECK)));
        }
        else {
            ruleBook.setFailEarly(true);
            Platform.runLater(() -> treeItem.setGraphic(new BraboGlyph(BraboGlyph.Icon.CROSS)));
        }

        Platform.runLater(() -> ruleView.refresh());
    }

    private RuleBook ruleBook;

    @Override
    public void apply(RuleBook rulebook) {
        this.ruleBook = rulebook;
        rulebook.setFailEarly(false);
    }
}
