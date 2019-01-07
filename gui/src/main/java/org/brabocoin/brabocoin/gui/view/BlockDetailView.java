package org.brabocoin.brabocoin.gui.view;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.SelectableLabel;
import org.brabocoin.brabocoin.gui.view.block.BlockDetailsPane;
import org.brabocoin.brabocoin.gui.view.block.BlockHeaderPane;
import org.brabocoin.brabocoin.gui.view.block.BlockTransactionsPane;
import org.brabocoin.brabocoin.gui.window.ValidationWindow;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Block detail view.
 * <p>
 * Side pane that shows all block contents.
 */
public class BlockDetailView extends VBox implements BraboControl, Initializable {

    private final @NotNull Blockchain blockchain;
    private boolean hasActions;
    @Nullable private BlockValidator validator;

    @FXML private BlockHeaderPane blockHeaderPane;
    @FXML private BlockDetailsPane blockDetailsPane;
    @FXML private BlockTransactionsPane blockTransactionsPane;

    @FXML private Label titleLabel;
    @FXML private Button buttonValidate;
    @FXML private SelectableLabel hashField;

    private final ObjectProperty<Block> block = new SimpleObjectProperty<>();

    public BlockDetailView(@NotNull Blockchain blockchain) {
        this(blockchain, null, null);
    }

    public BlockDetailView(@NotNull Blockchain blockchain, Block block, BlockValidator validator) {
        super();
        this.blockchain = blockchain;
        this.validator = validator;

        BraboControlInitializer.initialize(this);

        hasActions = validator != null;

        this.block.addListener((obs, old, val) -> {
            if (val != null) {
                loadBlock(val);
            }
        });
        setBlock(block);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        blockDetailsPane.setBlockchain(blockchain);
    }

    private void loadBlock(@NotNull Block block) {

        titleLabel.setText("Block #" + block.getBlockHeight());
        buttonValidate.setVisible(hasActions);
        hashField.setText(ByteUtil.toHexString(block.getHash().getValue(), Constants.BLOCK_HASH_SIZE));

        blockHeaderPane.setBlock(block);
        blockDetailsPane.setBlock(block);

        blockTransactionsPane.setBlock(block);
    }

    public Block getBlock() {
        return block.get();
    }

    public void setBlock(Block value) {
        if (value != null) {
            hasActions = !new Consensus().getGenesisBlock().getHash().equals(value.getHash())
                && validator != null;
        }
        block.setValue(value);
    }

    @FXML
    protected void validate(ActionEvent event) {
        Dialog dialog = new ValidationWindow(
            blockchain,
            getBlock(),
            validator
        );

        dialog.showAndWait();
    }
}
