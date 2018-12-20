package org.brabocoin.brabocoin.gui.view.block;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.util.ByteUtil;

/**
 * Pane displaying the block header.
 */
public class BlockHeaderPane extends TitledPane implements BraboControl {

    private final ObjectProperty<Block> block = new SimpleObjectProperty<>();

    @FXML private TextField networkIdField;
    @FXML private TextField previousBlockHashField;
    @FXML private TextField merkleRootField;
    @FXML private TextField targetValueField;
    @FXML private TextField blockHeightField;
    @FXML private TextField nonceField;

    public BlockHeaderPane() {
        super();
        BraboControlInitializer.initialize(this);

        block.addListener((obs, old, val) -> {
            if (val != null) loadBlock();
        });
    }

    private void loadBlock() {
        Block block = this.block.get();

        if (block == null) {
            return;
        }

        networkIdField.setText(String.valueOf(block.getNetworkId()));
        blockHeightField.setText(String.valueOf(block.getBlockHeight()));

        nonceField.setText(block.getNonce().toString(Constants.HEX).toUpperCase());
        targetValueField.setText(ByteUtil.toHexString(block.getTargetValue().getValue(), Constants.BLOCK_HASH_SIZE));
        merkleRootField.setText(ByteUtil.toHexString(block.getMerkleRoot().getValue(), Constants.BLOCK_HASH_SIZE));
        previousBlockHashField.setText(ByteUtil.toHexString(
            block.getPreviousBlockHash().getValue(),
            Constants.BLOCK_HASH_SIZE
        ));
    }

    public void setBlock(Block value) {
        block.set(value);
    }
}
