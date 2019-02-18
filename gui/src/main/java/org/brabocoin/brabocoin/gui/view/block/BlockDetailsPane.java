package org.brabocoin.brabocoin.gui.view.block;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.SelectableLabel;
import org.brabocoin.brabocoin.gui.util.GUIUtils;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Pane displaying the block header.
 */
public class BlockDetailsPane extends TitledPane implements BraboControl {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss");

    private final ObjectProperty<Block> block = new SimpleObjectProperty<>();

    private Blockchain blockchain;

    @FXML private Label timestampLabel;

    @FXML private SelectableLabel timestampField;
    @FXML private SelectableLabel numTransactionsField;
    @FXML private SelectableLabel outputTotalField;
    @FXML private SelectableLabel sizeField;

    public BlockDetailsPane() {
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

        numTransactionsField.setText(String.valueOf(block.getTransactions().size()));

        long totalOutput = block.getTransactions().stream()
            .flatMap(t -> t.getOutputs().stream())
            .mapToLong(Output::getAmount)
            .sum();
        outputTotalField.setText(GUIUtils.formatValue(totalOutput, true));

        IndexedBlock indexedBlock = null;
        try {
            indexedBlock = blockchain.getIndexedBlock(block.getHash());
        }
        catch (DatabaseException ignore) {

        }

        // If indexed block is available, use cached value
        int sizeBytes;
        if (indexedBlock != null) {
            sizeBytes = indexedBlock.getBlockInfo().getSizeInFile();
        }
        else {
            sizeBytes = ProtoConverter.toProtoBytes(block, BrabocoinProtos.Block.class).size();
        }
        double sizeKiloBytes = sizeBytes / 1000.0;
        sizeField.setText(String.format("%.3f kB", sizeKiloBytes));

        // Show timestamp if indexed block is available
        if (indexedBlock != null) {
            long timestamp = indexedBlock.getBlockInfo().getTimeReceived();
            Instant instant = Instant.ofEpochSecond(timestamp);
            LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

            timestampField.setManaged(true);
            timestampField.setVisible(true);
            timestampLabel.setManaged(true);
            timestampLabel.setVisible(true);
            timestampField.setText(DATE_FORMATTER.format(time));
        }
        else {
            timestampField.setManaged(false);
            timestampField.setVisible(false);
            timestampLabel.setManaged(false);
            timestampLabel.setVisible(false);
        }
    }

    public void setBlock(Block value) {
        block.set(value);
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }
}
