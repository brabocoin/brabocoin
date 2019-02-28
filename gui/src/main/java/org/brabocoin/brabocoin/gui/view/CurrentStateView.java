package org.brabocoin.brabocoin.gui.view;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.BlockchainListener;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.CollapsibleMasterDetailPane;
import org.brabocoin.brabocoin.gui.control.table.BooleanTextTableCell;
import org.brabocoin.brabocoin.gui.control.table.DateTimeTableCell;
import org.brabocoin.brabocoin.gui.control.table.DecimalTableCell;
import org.brabocoin.brabocoin.gui.control.table.HashTableCell;
import org.brabocoin.brabocoin.listeners.UpdateBlockchainListener;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
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
public class CurrentStateView extends TabPane implements BraboControl, Initializable,
                                                         BlockchainListener,
                                                         UpdateBlockchainListener {

    @FXML private Tab recentRejectBlkTab;
    @FXML private Tab recentRejectTxTab;
    @FXML private Tab txPoolTab;
    @FXML private Tab txOrphansTab;
    @FXML private Tab blkOrphansTab;
    @FXML private Tab utxoTab;

    @FXML private CollapsibleMasterDetailPane masterDetailPane;
    private BlockDetailView blockDetailView;

    @FXML private TableView<IndexedBlock> blockchainTable;
    @FXML private TableColumn<IndexedBlock, Integer> heightColumn;
    @FXML private TableColumn<IndexedBlock, LocalDateTime> timeColumn;
    @FXML private TableColumn<IndexedBlock, Hash> hashColumn;
    @FXML private TableColumn<IndexedBlock, Double> sizeColumn;
    @FXML private TableColumn<IndexedBlock, Boolean> minedByColumn;

    private final @NotNull State state;
    private final @NotNull Blockchain blockchain;
    private final BlockValidator validator;
    private ObservableList<IndexedBlock> observableBlocks = FXCollections.observableArrayList();

    public CurrentStateView(@NotNull State state) {
        super();
        this.state = state;
        this.blockchain = state.getBlockchain();
        this.validator = state.getBlockValidator();

        this.state.getEnvironment().addUpdateBlockchainListener(this);

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadTable();
        masterDetailPane.registerTableView(blockchainTable);

        blockDetailView = new BlockDetailView(blockchain, null, validator);
        masterDetailPane.setDetailNode(blockDetailView);

        loadMainChain();
        blockchain.addListener(this);

        RecentRejectBlkView rejectBlkView = new RecentRejectBlkView(
            blockchain,
            validator,
            state.getEnvironment()
        );
        recentRejectBlkTab.setContent(rejectBlkView);
        recentRejectBlkTab.textProperty().bind(
            Bindings.createStringBinding(
                () -> "Recently rejected blocks (" + rejectBlkView.getCount() + ")",
                rejectBlkView.countProperty()
            )
        );

        RecentRejectTxView rejectTxView = new RecentRejectTxView(
            state.getTransactionPool(),
            state.getEnvironment(),
            state.getTransactionValidator()
        );
        recentRejectTxTab.setContent(rejectTxView);
        recentRejectTxTab.textProperty().bind(
            Bindings.createStringBinding(
                () -> "Recently rejected transactions (" + rejectTxView.getCount() + ")",
                rejectTxView.countProperty()
            )
        );

        TransactionPoolView poolView = new TransactionPoolView(
            state.getTransactionPool(),
            state.getTransactionValidator(),
            state.getEnvironment()
        );
        txPoolTab.setContent(poolView);
        txPoolTab.textProperty().bind(
            Bindings.createStringBinding(
                () -> "Transaction pool (" + poolView.getCount() + ")",
                poolView.countProperty()
            )
        );

        OrphanTransactionsView orphanTxView = new OrphanTransactionsView(
            state.getTransactionPool(),
            state.getTransactionValidator()
        );
        txOrphansTab.setContent(orphanTxView);
        txOrphansTab.textProperty().bind(
            Bindings.createStringBinding(
                () -> "Orphan transactions (" + orphanTxView.getCount() + ")",
                orphanTxView.countProperty()
            )
        );

        OrphanBlocksView orphanBlkView = new OrphanBlocksView(
            state.getBlockchain(),
            state.getBlockValidator()
        );
        blkOrphansTab.setContent(orphanBlkView);
        blkOrphansTab.textProperty().bind(
            Bindings.createStringBinding(
                () -> "Orphan blocks (" + orphanBlkView.getCount() + ")",
                orphanBlkView.countProperty()
            )
        );

        UTXOSetView utxoView = new UTXOSetView(state.getChainUTXODatabase(), state.getWallet());
        utxoTab.setContent(utxoView);
        utxoTab.setText("UTXO set");
    }

    private void loadTable() {
        blockchainTable.setItems(observableBlocks);
        blockchainTable.setDisable(state.getEnvironment().isUpdatingBlockchain());

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
        hashColumn.setCellFactory(col -> new HashTableCell<>(Constants.BLOCK_HASH_SIZE));

        sizeColumn.setCellValueFactory(features -> {
            int sizeBytes = features.getValue().getBlockInfo().getSizeInFile();
            double sizeKiloBytes = sizeBytes / 1000.0;
            return new ReadOnlyObjectWrapper<>(sizeKiloBytes);
        });
        sizeColumn.setCellFactory(col -> new DecimalTableCell<>(new DecimalFormat("0.00")));

        minedByColumn.setCellValueFactory(features -> {
            boolean minedByMe = features.getValue().getBlockInfo().isMinedByMe();
            return new ReadOnlyObjectWrapper<>(minedByMe);
        });
        minedByColumn.setCellFactory(col -> new BooleanTextTableCell<>());

        blockchainTable.getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, old, indexedBlock) -> {
                if (indexedBlock == null) {
                    return;
                }
                try {
                    blockDetailView.setBlock(blockchain.getBlock(indexedBlock));
                }
                catch (DatabaseException e) {
                    // ignore
                }
                masterDetailPane.setShowDetailNode(true);
            });
    }

    private void loadMainChain() {
        observableBlocks.clear();

        IndexedChain chain = blockchain.getMainChain();
        for (int i = chain.getHeight(); i >= 0; i--) {
            observableBlocks.add(chain.getBlockAtHeight(i));
        }
    }

    @Override
    public void onTopBlockConnected(@NotNull IndexedBlock block) {
        observableBlocks.add(0, block);
    }

    @Override
    public void onTopBlockDisconnected(@NotNull IndexedBlock block) {
        if (block.getHash().equals(observableBlocks.get(0).getHash())) {
            observableBlocks.remove(0);
        }
    }

    @Override
    public void onStartUpdate() {
        blockchainTable.setDisable(true);
    }

    @Override
    public void onUpdateFinished() {
        blockchainTable.setDisable(false);
    }
}
