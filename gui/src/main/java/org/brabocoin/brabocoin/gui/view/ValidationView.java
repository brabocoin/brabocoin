package org.brabocoin.brabocoin.gui.view;

import javafx.application.Platform;
import javafx.concurrent.Task;
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
import org.brabocoin.brabocoin.validation.rule.Rule;
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.controlsfx.control.MasterDetailPane;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class ValidationView extends MasterDetailPane implements BraboControl, Initializable,
                                                                ValidationListener {

    private final Map<Class, TreeItem<String>> ruleTreeItemMap;
    private final Validator validator;
    private TransactionDetailView transactionDetailView;
    private BlockDetailView blockDetailView;
    private Transaction transaction;
    private Block block;


    @FXML
    public TreeView<String> ruleView;

    private void loadRules() {
        TreeItem<String> root = new TreeItem<>();
        ruleView.setShowRoot(false);

        RuleList ruleList;
        if (isForBlock()) {
            ruleList = BlockValidator.INCOMING_BLOCK;
        } else {
            ruleList = TransactionValidator.ALL;
        }
        addRules(root, ruleList);
        ruleView.setRoot(root);
    }

    private void addRules(TreeItem<String> node, RuleList ruleList) {
        for (Class rule : ruleList) {
            TreeItem<String> ruleTreeItem = new TreeItem<>();
            ruleTreeItem.setGraphic(new BraboGlyph(BraboGlyph.Icon.CIRCLE));

            Annotation annotation = rule.getAnnotation(ValidationRule.class);
            if (annotation instanceof ValidationRule) {
                ValidationRule validationRule = (ValidationRule)annotation;
                ruleTreeItem.setValue(validationRule.name());

                if (validationRule.composite()) {
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

        blockDetailView = new BlockDetailView(blockchain, block);
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

        Task task = new Task<Void>() {
            @Override
            public Void call() {
                // Run validator
                if (isForBlock()) {
                    BlockValidator validator = (BlockValidator)me.validator;
                    validator.addListener(me);
                    validator.checkIncomingBlockValid(block);
                }
                else {
                    TransactionValidator validator = (TransactionValidator)me.validator;
                    validator.addListener(me);
                    validator.checkTransactionValid(transaction);
                }

                return null;
            }
        };

        new Thread(task).run();
    }

    @Override
    public void onRuleValidation(Rule rule, RuleBookResult result) {
        Platform.runLater(() -> {
            TreeItem<String> treeItem = ruleTreeItemMap.get(rule.getClass());

            if (treeItem == null) {
                // TODO: HELP!
                return;
            }

            if (result.isPassed()) {
                treeItem.setGraphic(new BraboGlyph(BraboGlyph.Icon.CHECK));
            }
            else {
                treeItem.setGraphic(new BraboGlyph(BraboGlyph.Icon.CROSS));
            }

            ruleView.refresh();
        });
    }
}
