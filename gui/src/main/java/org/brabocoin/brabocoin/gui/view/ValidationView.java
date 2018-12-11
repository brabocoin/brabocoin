package org.brabocoin.brabocoin.gui.view;

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
import org.brabocoin.brabocoin.validation.annotation.CompositeRuleList;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.controlsfx.control.MasterDetailPane;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ResourceBundle;

public class ValidationView extends MasterDetailPane implements BraboControl, Initializable {

    private Transaction transaction;
    private Block block;

    private BlockDetailView blockDetailView;

    @FXML
    public TreeView<String> ruleView;

    private void loadRules() {
        TreeItem<String> root = new TreeItem<>();
        ruleView.setShowRoot(false);

        if (isForBlock()) {
            RuleList ruleList = BlockValidator.INCOMING_BLOCK;

            addRules(root, ruleList);
        }
        ruleView.setRoot(root);
    }

    private void addRules(TreeItem<String> node, RuleList ruleList) {
        for (Class rule : ruleList) {
            TreeItem<String> ruleTreeItem = new TreeItem<>();
            ruleTreeItem.setGraphic(new BraboGlyph(BraboGlyph.Icon.CHECK));

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
                                composite = (RuleList) field.get(rule.newInstance());
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
            } else {
                ruleTreeItem.setValue("[Could not determine annotation type]");
            }

            node.getChildren().add(ruleTreeItem);
        }
    }

    public ValidationView(@NotNull Blockchain blockchain, @NotNull Transaction transaction) {
        super();
        this.transaction = transaction;

        BraboControlInitializer.initialize(this);
    }

    public ValidationView(@NotNull Blockchain blockchain, @NotNull Block block) {
        super();
        this.block = block;

        this.setDetailNode(new BlockDetailView(blockchain, block));

        BraboControlInitializer.initialize(this);
    }

    private boolean isForBlock() {
        return block != null;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadRules();
    }
}
