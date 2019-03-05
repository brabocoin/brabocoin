package org.brabocoin.brabocoin.gui.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.glyph.BraboGlyph;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.processor.UTXOProcessor;
import org.brabocoin.brabocoin.validation.consensus.Consensus;
import org.brabocoin.brabocoin.validation.ValidationListener;
import org.brabocoin.brabocoin.validation.Validator;
import org.brabocoin.brabocoin.validation.annotation.CompositeRuleList;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.Rule;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.controlsfx.control.MasterDetailPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import java.util.function.BiConsumer;

public abstract class ValidationView<T extends org.brabocoin.brabocoin.model.proto.ProtoModel> extends MasterDetailPane implements BraboControl, Initializable,
                                                                                                                                   ValidationListener {

    private static final BraboGlyph.Icon ICON_PENDING = BraboGlyph.Icon.CIRCLE;
    private static final BraboGlyph.Icon ICON_SKIPPED = BraboGlyph.Icon.CIRCLEMINUS;
    private static final BraboGlyph.Icon ICON_SUCCESS = BraboGlyph.Icon.CHECK;
    private static final BraboGlyph.Icon ICON_FAIL = BraboGlyph.Icon.CROSS;
    final Map<TreeItem<? extends String>, Transaction> transactionItemMap;
    final Map<TreeItem<? extends String>, String> descriptionItemMap;
    private final Map<Class<? extends Rule>, List<TreeItem<String>>> ruleTreeItemMap;
    private final Validator validator;
    private final T subject;
    protected final Blockchain blockchain;
    protected final Consensus consensus;
    private WebEngine descriptionWebEngine;
    private TreeItem<String> root;
    private boolean hasSkippedRulesLoaded = false;
    @FXML private TreeView<String> ruleView;
    @FXML private MasterDetailPane masterDetailNode;
    @FXML private VBox treeViewPlaceHolder;

    public ValidationView(@NotNull Validator validator, @NotNull T subject,
                          @NotNull Blockchain blockchain, @NotNull Consensus consensus,
                          @Nullable Node detailView) {
        super();
        ruleTreeItemMap = new HashMap<>();
        transactionItemMap = new HashMap<>();
        descriptionItemMap = new HashMap<>();

        this.validator = validator;
        this.subject = subject;
        this.blockchain = blockchain;
        this.consensus = consensus;
        this.setDetailNode(detailView);

        BraboControlInitializer.initialize(this);
    }

    protected <U extends Validator> U getRevertedUTXOValidator(U validator, T subject) {
        ChainUTXODatabase revertedUTXO = getRevertedUTXO(subject);
        if (revertedUTXO == null) {
            return validator;
        }
        return (U) validator.withUTXOSet(revertedUTXO);
    }

    protected abstract IndexedBlock getRevertedUTXODestinationBlock(T subject);

    private ChainUTXODatabase getRevertedUTXO(T subject) {
        try {
            IndexedBlock indexedDestinationBlock = getRevertedUTXODestinationBlock(subject);

            if (indexedDestinationBlock != null && blockchain.getMainChain()
                .contains(indexedDestinationBlock)) {
                ChainUTXODatabase validationUTXO = new ChainUTXODatabase(
                    new HashMapDB(),
                    consensus
                );
                UTXOProcessor processor = new UTXOProcessor(validationUTXO);

                // Build up validation UTXO
                for (int i = 1; i < indexedDestinationBlock.getBlockInfo().getBlockHeight(); i++) {
                    Block prevBlk = blockchain.getBlock(blockchain.getMainChain()
                        .getBlockAtHeight(i));
                    processor.processBlockConnected(prevBlk);
                }

                return validationUTXO;
            }
        }
        catch (DatabaseException ignored) {
        }
        return null;
    }

    @Override
    public @NotNull String resourceName() {
        return "validation_view";
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
                List<Class<? extends Rule>> rules = getRules().getRules();
                rules.removeAll(getSkippedRules().getRules());
                applyValidator().accept(subject, new RuleList(rules));
                validator.removeListener(this);
            }
        }).start();
    }

    private void createDescriptionNode() {
        WebView browser = new WebView();
        descriptionWebEngine = browser.getEngine();
        descriptionWebEngine.setUserStyleSheetLocation(
            this.getClass().getResource("validation_description.css").toExternalForm()
        );

        masterDetailNode.setDetailNode(browser);
    }

    private void loadRules() {
        root = new TreeItem<>();
        Platform.runLater(() -> ruleView.setShowRoot(false));

        RuleList ruleList = getRules();
        addRules(root, ruleList, false);
        Platform.runLater(() -> {
            ruleView.setManaged(true);
            ruleView.setVisible(true);
            ruleView.setRoot(root);
            treeViewPlaceHolder.setManaged(false);
            treeViewPlaceHolder.setVisible(false);
        });
    }

    protected abstract RuleList getRules();

    protected abstract RuleList getSkippedRules();

    protected abstract BiConsumer<T, RuleList> applyValidator();

    protected void addRules(TreeItem<String> node, RuleList ruleList, boolean ignored) {
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

                    List<TreeItem<String>> children = addCompositeRuleChildren(rule, composite);
                    ruleTreeItem.getChildren().addAll(children);

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

    protected boolean isSkippedRuleClass(Class<? extends Rule> rule) {
        return getSkippedRules().getRules().contains(rule);
    }

    protected Node createIcon(RuleState state) {
        BraboGlyph glyph = null;

        switch (state) {
            case PENDING:
                glyph = new BraboGlyph(ICON_PENDING);
                break;
            case SKIPPED:
                glyph = new BraboGlyph(ICON_SKIPPED);
                glyph.setColor(Color.GRAY);
                break;
            case SUCCESS:
                glyph = new BraboGlyph(ICON_SUCCESS);
                glyph.setColor(Color.GREEN);
                break;
            case FAIL:
                glyph = new BraboGlyph(ICON_FAIL);
                glyph.setColor(Color.RED);
                break;
        }

        BraboGlyph background = new BraboGlyph(BraboGlyph.Icon.CIRCLE_SOLID);
        background.setColor(Color.WHITE);
        background.getStyleClass().add("outline");

        return new StackPane(background, glyph);
    }

    protected abstract List<TreeItem<String>> addCompositeRuleChildren(Class<? extends Rule> rule,
                                                                       RuleList composite);

    protected String deriveSkippedDescription(Class<? extends Rule> ruleClass) {
        return deriveSkippedDescription(
            ruleClass,
            deriveClassTwigPath(ruleClass, true)
        );
    }

    protected String deriveSkippedDescription(Class<? extends Rule> ruleClass,
                                              String templatePath) {
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

    @Override
    public void onRuleValidation(Rule rule, RuleBookResult result, RuleBook ruleBook) {
        Platform.runLater(() -> {
            List<TreeItem<String>> treeItems = ruleTreeItemMap.get(rule.getClass());

            TreeItem<String> item = findTreeItem(treeItems, rule, ruleBook);

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
                        .allMatch(t -> ((BraboGlyph)((StackPane)t.getGraphic()).getChildren()
                            .get(1)).getIcon()
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

    protected abstract @Nullable TreeItem<String> findTreeItem(List<TreeItem<String>> treeItems,
                                                               Rule rule, RuleBook ruleBook);

    private String deriveDescription(Rule rule, boolean success) {
        String templatePath = deriveClassTwigPath(rule.getClass(), false);
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

    protected String deriveClassTwigPath(Class rule, boolean skipped) {
        return deriveClassPath(rule) + (skipped ? ".skipped.twig" : ".twig");
    }

    protected String deriveClassPath(Class rule) {
        return rule.getName().replace('.', '/');
    }

    @Override
    public void onValidationStarted(FactMap facts) {
        Platform.runLater(() -> {
            setSkippedRuleDescriptions(root);
            hasSkippedRulesLoaded = true;
        });
    }

    private void setSkippedRuleDescriptions(TreeItem<String> root) {
        // Make sure we only load skipped rule descriptions once, for else we may override
        // existing results.
        if (hasSkippedRulesLoaded) {
            return;
        }

        Class<? extends Rule> inverseSearch = inverseSearchRule(root);

        if (inverseSearch != null && (isSkippedRuleClass(inverseSearch) || isSkippedDescendant(root))) {
            descriptionItemMap.put(root, deriveSkippedDescription(inverseSearch));
        }

        for (TreeItem<String> child : root.getChildren()) {
            setSkippedRuleDescriptions(child);
        }
    }

    private boolean isSkippedDescendant(TreeItem<String> descendant) {
        StackPane stackPane = (StackPane)descendant.getGraphic();
        Node graphic = null;
        if (stackPane != null) {
            graphic = stackPane.getChildren().get(1);
        }
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

    public enum RuleState {
        PENDING,
        SKIPPED,
        SUCCESS,
        FAIL
    }
}
