package org.brabocoin.brabocoin.gui.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.glyph.BraboGlyph;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.ValidationListener;
import org.brabocoin.brabocoin.validation.Validator;
import org.brabocoin.brabocoin.validation.annotation.CompositeRuleList;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
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
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.brabocoin.brabocoin.validation.transaction.rules.DuplicatePoolTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.PoolDoubleSpendingTxRule;
import org.controlsfx.control.MasterDetailPane;
import org.jetbrains.annotations.NotNull;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.resource.exceptions.ResourceNotFoundException;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ValidationView extends MasterDetailPane implements BraboControl, Initializable,
                                                                ValidationListener {

    private final Map<Class<? extends Rule>, List<TreeItem<String>>> ruleTreeItemMap;
    private final Map<TreeItem<? extends String>, Transaction> transactionItemMap;
    private final Map<TreeItem<? extends String>, String> descriptionItemMap;
    private final Validator<? extends org.brabocoin.brabocoin.model.proto.ProtoModel> validator;
    private TransactionDetailView transactionDetailView;
    private BlockDetailView blockDetailView;
    private Transaction transaction;
    private Block block;
    private WebEngine descriptionWebEngine;
    private TreeItem<String> root;
    @FXML private MasterDetailPane masterDetailNode;
    @FXML private VBox treeViewPlaceHolder;

    private final BraboGlyph.Icon ICON_PENDING = BraboGlyph.Icon.CIRCLE;
    private final BraboGlyph.Icon ICON_SKIPPED = BraboGlyph.Icon.CIRCLEMINUS;
    private final BraboGlyph.Icon ICON_SUCCESS = BraboGlyph.Icon.CHECK;
    private final BraboGlyph.Icon ICON_FAIL = BraboGlyph.Icon.CROSS;

    public enum RuleState {
        PENDING,
        SKIPPED,
        SUCCESS,
        FAIL
    }

    private BraboGlyph createIcon(RuleState state) {
        switch (state) {
            case PENDING:
                BraboGlyph glyphPending = new BraboGlyph(ICON_PENDING);
                return glyphPending;
            case SKIPPED:
                BraboGlyph glyphSkipped = new BraboGlyph(ICON_SKIPPED);
                glyphSkipped.setColor(Color.GRAY);
                return glyphSkipped;
            case SUCCESS:
                BraboGlyph glyphSuccess = new BraboGlyph(ICON_SUCCESS);
                glyphSuccess.setColor(Color.GREEN);
                return glyphSuccess;
            case FAIL:
                BraboGlyph glyphFail = new BraboGlyph(ICON_FAIL);
                glyphFail.setColor(Color.RED);
                return glyphFail;
        }

        return null;
    }

    private static final RuleList skippedBlockRules = new RuleList(
        DuplicateStorageBlkRule.class,
        UniqueUnspentCoinbaseBlkRule.class,
        ContextualTransactionCheckBlkRule.class,
        LegalTransactionFeesBlkRule.class,
        ValidCoinbaseOutputAmountBlkRule.class
    );

    private static final RuleList skippedTransactionRules = new RuleList(
        DuplicatePoolTxRule.class,
        PoolDoubleSpendingTxRule.class
    );

    private static final RuleList blockRules = new RuleList(
        BlockValidator.INCOMING_BLOCK,
        BlockValidator.CONNECT_TO_CHAIN
    );
    private static final RuleList transactionRules = TransactionValidator.ALL;

    @FXML
    public TreeView<String> ruleView;

    private void loadRules() {
        root = new TreeItem<>();
        Platform.runLater(() -> ruleView.setShowRoot(false));

        RuleList ruleList;
        if (isForBlock()) {
            ruleList = blockRules;
        }
        else {
            ruleList = transactionRules;
        }
        addRules(root, ruleList, false);
        Platform.runLater(() -> {
            ruleView.setManaged(true);
            ruleView.setVisible(true);
            ruleView.setRoot(root);
            treeViewPlaceHolder.setManaged(false);
            treeViewPlaceHolder.setVisible(false);
        });
    }

    private boolean isSkippedRuleClass(Class<? extends Rule> rule) {
        return (isForBlock() && skippedBlockRules.getRules().contains(rule)) ||
            (!isForBlock() && skippedTransactionRules.getRules().contains(rule));
    }

    private void addRules(TreeItem<String> node, RuleList ruleList, boolean ignored) {
        for (Class<? extends Rule> rule : ruleList) {
            TreeItem<String> ruleTreeItem = new TreeItem<>();
            if (ignored || isSkippedRuleClass(rule)) {
                ruleTreeItem.setGraphic(createIcon(RuleState.SKIPPED));
            }
            else {
                ruleTreeItem.setGraphic(createIcon(RuleState.PENDING));
            }

            ValidationRule annotation = rule.getAnnotation(ValidationRule.class);
            if (annotation != null) {
                ruleTreeItem.setValue(annotation.name());

                if (isSkippedRuleClass(rule) && !annotation.composite()) {
                    ruleTreeItem.setGraphic(createIcon(RuleState.SKIPPED));
                }
                else if (annotation.composite()) {
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

    private void createDescriptionNode() {
        WebView browser = new WebView();
        descriptionWebEngine = browser.getEngine();
        descriptionWebEngine.setUserStyleSheetLocation(
            this.getClass().getResource("validation_description.css").toExternalForm()
        );

        masterDetailNode.setDetailNode(browser);
    }

    public ValidationView(@NotNull Transaction transaction, @NotNull
        Validator<Transaction> validator) {
        super();

        ruleTreeItemMap = new HashMap<>();
        transactionItemMap = new HashMap<>();
        descriptionItemMap = new HashMap<>();
        this.transaction = transaction;
        this.validator = validator;

        transactionDetailView = new TransactionDetailView(transaction, null);
        this.setDetailNode(transactionDetailView);

        BraboControlInitializer.initialize(this);
    }

    public ValidationView(@NotNull Blockchain blockchain, @NotNull Block block,
                          @NotNull Validator<Block> validator) {
        super();

        ruleTreeItemMap = new HashMap<>();
        transactionItemMap = new HashMap<>();
        descriptionItemMap = new HashMap<>();
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
        createDescriptionNode();
        new Thread(() -> {
            loadRules();

            Platform.runLater(() -> ruleView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    String data = "This rule was not executed.";
                    if (descriptionItemMap.containsKey(newValue)) {
                        data = descriptionItemMap.get(newValue);
                    }
                    descriptionWebEngine.loadContent(data);
                }));

            synchronized (validator) {
                validator.addListener(this);
                // Run validator
                if (isForBlock()) {
                    BlockValidator blockValidator = (BlockValidator)validator;

                    List<Class<? extends Rule>> rules = blockRules.getRules();
                    rules.removeAll(skippedBlockRules.getRules());
                    blockValidator.validate(block, new RuleList(rules));
                }
                else {
                    TransactionValidator transactionValidator = (TransactionValidator)validator;

                    List<Class<? extends Rule>> rules = transactionRules.getRules();
                    rules.removeAll(skippedTransactionRules.getRules());
                    transactionValidator.validate(transaction, new RuleList(rules), true);
                }
                validator.removeListener(this);
            }
        }).start();
    }

    private String deriveSkippedDescription(Class<? extends Rule> ruleClass) {
        String templatePath = ruleClass.getName().replace('.', '/') + ".skipped.twig";
        JtwigTemplate template = JtwigTemplate.classpathTemplate(templatePath);
        JtwigModel model = JtwigModel.newModel();

        ValidationRule annotation = ruleClass.getAnnotation(ValidationRule.class);
        model.with("title", annotation.name());

        try {
            return template.render(model);
        }
        catch (
            ResourceNotFoundException e) {
            return "Could not find rule description template.";
        }
    }

    private String deriveDescription(Rule rule, boolean success) {
        String templatePath = rule.getClass().getName().replace('.', '/') + ".twig";
        JtwigTemplate template = JtwigTemplate.classpathTemplate(templatePath);

        JtwigModel model = JtwigModel.newModel();

        for (Field field : rule.getClass().getDeclaredFields()) {
            if (field.getAnnotation(DescriptionField.class) != null) {
                field.setAccessible(true);

                Object value;
                try {
                    value = field.get(rule);
                }
                catch (IllegalAccessException e) {
                    value = "NOT-FOUND";
                }

                model.with(
                    field.getName(),
                    value
                );
            }
        }

        model.with("result", success);

        ValidationRule annotation = rule.getClass().getAnnotation(ValidationRule.class);
        model.with("title", annotation.name());

        try {
            return template.render(model);
        }
        catch (
            ResourceNotFoundException e) {
            return "Could not find rule description template.";
        }
    }

    @Override
    public void onRuleValidation(Rule rule, RuleBookResult result, RuleBook ruleBook) {
        Platform.runLater(() -> {
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
                    throw new IllegalStateException(
                        "Found not exactly one rule tree items for blocks");
                }

                item = treeItems.get(0);
            }
            else {
                throw new IllegalStateException("Rule of invalid type");
            }

            descriptionItemMap.put(item, deriveDescription(rule, result.isPassed()));

            if (item == null) {
                return;
            }

            if (result.isPassed()) {
                Platform.runLater(() -> {
                    item.setGraphic(createIcon(RuleState.SUCCESS));

                    if (item.getParent()
                        .getChildren()
                        .stream()
                        .allMatch(t -> ((BraboGlyph)t.getGraphic()).getIcon()
                            .equals(ICON_SUCCESS))) {
                        item.getParent().setGraphic(createIcon(RuleState.SUCCESS));
                    }
                });
            }
            else {
                item.setGraphic(createIcon(RuleState.FAIL));
            }

            ruleView.refresh();
        });
    }

    @Override
    public void onValidationStarted(FactMap facts) {
        Platform.runLater(() -> setSkippedRuleDescriptions(root));
    }

    private void setSkippedRuleDescriptions(TreeItem<String> root) {
        Class<? extends Rule> inverseSearch = inverseSearchRule(root);

        if (inverseSearch != null && (isSkippedRuleClass(inverseSearch) || isSkippedDescendant(root))) {
            descriptionItemMap.put(root, deriveSkippedDescription(inverseSearch));
        }

        for (TreeItem<String> child : root.getChildren()) {
            setSkippedRuleDescriptions(child);
        }
    }

    private boolean isSkippedDescendant(TreeItem<String> descendant) {
        Node graphic = descendant.getGraphic();
        if (graphic != null && ((BraboGlyph)graphic).getIcon()
            .equals(ICON_SKIPPED)) {
            return true;
        }
        else {
            if (descendant.getParent() != null) {
                return isSkippedDescendant(descendant.getParent());
            }
            else {
                return false;
            }
        }
    }

    private Class<? extends Rule> inverseSearchRule(TreeItem<String> item) {
        for (Map.Entry<Class<? extends Rule>, List<TreeItem<String>>> ruleTreeItem :
            ruleTreeItemMap.entrySet()) {
            if (ruleTreeItem.getValue().contains(item)) {
                return ruleTreeItem.getKey();
            }
        }

        return null;
    }
}
