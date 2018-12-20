package org.brabocoin.brabocoin.gui.view;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.view.block.BlockHeaderPane;
import org.brabocoin.brabocoin.gui.view.block.BlockTransactionsPane;
import org.brabocoin.brabocoin.gui.window.ValidationWindow;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Block detail view.
 * <p>
 * Side pane that shows all block contents.
 */
public class BlockDetailView extends VBox implements BraboControl, Initializable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");

    private final @NotNull Blockchain blockchain;
    private boolean hasActions;
    @Nullable private BlockValidator validator;

    @FXML private BlockHeaderPane blockHeaderPane;
    @FXML private BlockTransactionsPane blockTransactionsPane;

    @FXML private Label titleLabel;
    @FXML private Button buttonValidate;
    @FXML private TextField hashField;

    @FXML private TextField timestampField;
    @FXML private TextField numTransactionsField;
    @FXML private TextField outputTotalField;
    @FXML private TextField sizeField;

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
    }

    private void loadBlock(@NotNull Block block) {

        titleLabel.setText("Block #" + block.getBlockHeight());
        buttonValidate.setVisible(hasActions);
        hashField.setText(ByteUtil.toHexString(block.getHash().getValue(), 32));

        blockHeaderPane.setBlock(block);

        long timestamp = 0;
        try {
            IndexedBlock indexedBlock = blockchain.getIndexedBlock(block.getHash());
            timestamp = indexedBlock.getBlockInfo().getTimeReceived();
            Instant instant = Instant.ofEpochSecond(timestamp);
            LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            timestampField.setText(DATE_FORMATTER.format(time));
            numTransactionsField.setText(String.valueOf(indexedBlock.getBlockInfo()
                .getTransactionCount()));
            int sizeBytes = indexedBlock.getBlockInfo().getSizeInFile();
            double sizeKiloBytes = sizeBytes / 1000.0;
            sizeField.setText(String.format("%.3f kB", sizeKiloBytes));
        }
        catch (DatabaseException | NullPointerException e) {
            // ignore
        }



        long totalOutput = block.getTransactions().stream()
            .flatMap(t -> t.getOutputs().stream())
            .mapToLong(Output::getAmount)
            .sum();
        BigDecimal roundedOutput = BigDecimal.valueOf(totalOutput)
            .divide(BigDecimal.valueOf(Consensus.COIN), 2, RoundingMode.UNNECESSARY);
        outputTotalField.setText(DECIMAL_FORMAT.format(roundedOutput) + " BRC");

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
