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
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.window.ValidationWindow;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
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
import java.util.List;
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

    @FXML private VBox transactionsPane;

    @FXML private Label titleLabel;
    @FXML private Button buttonValidate;
    @FXML private TextField hashField;

    @FXML private TextField networkIdField;
    @FXML private TextField previousBlockHashField;
    @FXML private TextField merkleRootField;
    @FXML private TextField targetValueField;
    @FXML private TextField blockHeightField;
    @FXML private TextField nonceField;

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

        networkIdField.setText(String.valueOf(block.getNetworkId()));
        blockHeightField.setText(String.valueOf(block.getBlockHeight()));

        nonceField.setText(block.getNonce().toString(16).toUpperCase());
        targetValueField.setText(ByteUtil.toHexString(block.getTargetValue().getValue(), 32));
        merkleRootField.setText(ByteUtil.toHexString(block.getMerkleRoot().getValue(), 32));
        previousBlockHashField.setText(ByteUtil.toHexString(
            block.getPreviousBlockHash().getValue(),
            32
        ));

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

        loadBlockTransactions(block);
    }

    private void loadBlockTransactions(@NotNull Block block) {
        transactionsPane.getChildren().clear();

        List<Transaction> transactions = block.getTransactions();
        for (int i = 0; i < transactions.size(); i++) {
            Transaction tx = transactions.get(i);

            TransactionDetailView detailView = new TransactionDetailView(tx);
            detailView.setShowHeader(false);

            TitledPane pane = new TitledPane(
                i == 0 ? "Coinbase transaction" : ("Transaction " + i),
                detailView
            );
            pane.setExpanded(false);

            transactionsPane.getChildren().add(pane);
        }
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
