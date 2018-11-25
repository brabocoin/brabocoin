package org.brabocoin.brabocoin.gui.view;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.table.DateTimeTableCell;
import org.brabocoin.brabocoin.gui.control.table.DecimalTableCell;
import org.brabocoin.brabocoin.gui.control.table.HashTableCell;
import org.brabocoin.brabocoin.model.Hash;
import org.controlsfx.control.MasterDetailPane;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ResourceBundle;

/**
 * View for the current state.
 */
public class CurrentStateView extends TabPane implements BraboControl, Initializable {

    @FXML private MasterDetailPane masterDetailPane;
    private BlockDetailView blockDetailView;

    @FXML private TableView<IndexedBlock> blockchainTable;
    @FXML private TableColumn<IndexedBlock, Integer> heightColumn;
    @FXML private TableColumn<IndexedBlock, LocalDateTime> timeColumn;
    @FXML private TableColumn<IndexedBlock, Hash> hashColumn;
    @FXML private TableColumn<IndexedBlock, Double> sizeColumn;

    private final @NotNull Blockchain blockchain;
    private ObservableList<IndexedBlock> observableBlocks = FXCollections.observableArrayList();

    public CurrentStateView(@NotNull Blockchain blockchain) {
        super();
        this.blockchain = blockchain;

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadTable();
        loadMainChain();
    }

    private void loadTable() {
        blockchainTable.setItems(observableBlocks);

        heightColumn.setCellValueFactory(features -> {
            int blockHeight = features.getValue().getBlockInfo().getBlockHeight();
            return new ReadOnlyObjectWrapper<>(blockHeight);
        });

        timeColumn.setCellValueFactory(features -> {
            long timestamp = features.getValue().getBlockInfo().getTimeReceived();
            Instant instant = Instant.ofEpochSecond(timestamp);
            LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return new ReadOnlyObjectWrapper<>(time);
        });
        timeColumn.setCellFactory(col -> new DateTimeTableCell<>());

        hashColumn.setCellValueFactory(features -> {
            Hash hash = features.getValue().getHash();
            return new ReadOnlyObjectWrapper<>(hash);
        });
        hashColumn.setCellFactory(col -> new HashTableCell<>());

        sizeColumn.setCellValueFactory(features -> {
            int sizeBytes = features.getValue().getBlockInfo().getSizeInFile();
            double sizeKiloBytes = sizeBytes / 1000.0;
            return new ReadOnlyObjectWrapper<>(sizeKiloBytes);
        });
        sizeColumn.setCellFactory(col -> new DecimalTableCell<>(new DecimalFormat("0.00")));

        blockchainTable.getSelectionModel().selectedItemProperty().addListener((obs, old, indexedBlock) -> {
            blockDetailView.setBlock(indexedBlock);
            masterDetailPane.setShowDetailNode(true);
        });
    }

    private void loadMainChain() {
        blockDetailView = new BlockDetailView(blockchain);
        masterDetailPane.setDetailNode(blockDetailView);

        IndexedChain chain = blockchain.getMainChain();
        for (int i = chain.getHeight(); i >= 0; i--) {
            observableBlocks.add(chain.getBlockAtHeight(i));
        }
    }
}
