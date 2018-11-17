package org.brabocoin.brabocoin.gui.view;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * @author Sten Wessel
 */
public class BlockDetailView extends VBox implements BraboControl, Initializable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss");

    private final @NotNull Blockchain blockchain;

    @FXML private Label titleLabel;
    @FXML private TextField hashField;

    @FXML private TextField blockHeightField;
    @FXML private TextField nonceField;
    @FXML private TextField targetValueField;
    @FXML private TextField merkleRootField;
    @FXML private TextField previousBlockHashField;

    @FXML private TextField timestampField;
    @FXML private TextField numTransactionsField;
    @FXML private TextField outputTotalField;
    @FXML private TextField sizeField;

    private final ObjectProperty<IndexedBlock> block = new SimpleObjectProperty<>();

    public BlockDetailView(@NotNull Blockchain blockchain) {
        this(blockchain, null);
    }

    public BlockDetailView(@NotNull Blockchain blockchain, IndexedBlock block) {
        super();
        this.blockchain = blockchain;

        BraboControlInitializer.initialize(this);

        this.block.addListener((obs, old, val) -> { if (val != null) loadBlock(val); });
        setBlock(block);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    private void loadBlock(@NotNull IndexedBlock indexedBlock) {
        Block block;
        try {
            block = blockchain.getBlock(indexedBlock.getHash());
        }
        catch (DatabaseException e) {
            return;
        }

        if (block == null) {
            return;
        }

        titleLabel.setText("Block #" + indexedBlock.getBlockInfo().getBlockHeight());
        hashField.setText(ByteUtil.toHexString(indexedBlock.getHash().getValue(), 32));

        blockHeightField.setText(String.valueOf(indexedBlock.getBlockInfo().getBlockHeight()));

        long timestamp = indexedBlock.getBlockInfo().getTimeReceived();
        Instant instant = Instant.ofEpochSecond(timestamp);
        LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        timestampField.setText(DATE_FORMATTER.format(time));
        nonceField.setText(indexedBlock.getBlockInfo().getNonce().toString(16));
        targetValueField.setText(ByteUtil.toHexString(indexedBlock.getBlockInfo().getTargetValue().getValue(), 32));
        merkleRootField.setText(ByteUtil.toHexString(indexedBlock.getBlockInfo().getMerkleRoot().getValue(), 32));
        previousBlockHashField.setText(ByteUtil.toHexString(indexedBlock.getBlockInfo().getPreviousBlockHash().getValue(), 32));

        numTransactionsField.setText(String.valueOf(indexedBlock.getBlockInfo().getTransactionCount()));

        int sizeBytes = indexedBlock.getBlockInfo().getSizeInFile();
        double sizeKiloBytes = sizeBytes / 1000.0;
        sizeField.setText(String.format("%.3f kB", sizeKiloBytes));

        long totalOutput = block.getTransactions().stream()
            .flatMap(t -> t.getOutputs().stream())
            .mapToLong(Output::getAmount)
            .sum();
        outputTotalField.setText(String.valueOf(totalOutput) + " BRC");

        // TODO: block reward and transaction fees

        loadBlockTransactions(block);
    }

    private void loadBlockTransactions(@NotNull Block block) {

    }

    public IndexedBlock getBlock() {
        return block.get();
    }

    public void setBlock(IndexedBlock value) {
        block.setValue(value);
    }
}
