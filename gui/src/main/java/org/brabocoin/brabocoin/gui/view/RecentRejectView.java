package org.brabocoin.brabocoin.gui.view;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.BlockchainListener;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.table.HashTableCell;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.controlsfx.control.MasterDetailPane;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * View to display a collection of blocks, with detail view.
 */
public class RecentRejectView extends MasterDetailPane implements BraboControl, Initializable, BlockchainListener {

    private BlockDetailView blockDetailView;

    @FXML private TableView<Block> blocksTable;
    @FXML private TableColumn<Block, Integer> heightColumn;
    @FXML private TableColumn<Block, Hash> hashColumn;

    private final @NotNull Blockchain blockchain;
    private final BlockValidator validator;
    private ObservableList<Block> observableBlocks = FXCollections.observableArrayList();

    public RecentRejectView(@NotNull Blockchain blockchain, @NotNull BlockValidator validator) {
        super();
        this.blockchain = blockchain;
        this.validator = validator;

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadTable();

        blockDetailView = new BlockDetailView(blockchain, null, validator);
        setDetailNode(blockDetailView);

        loadRejects();
        blockchain.addListener(this);
    }

    private void loadTable() {
        blocksTable.setItems(observableBlocks);

        heightColumn.setCellValueFactory(features -> {
            int blockHeight = features.getValue().getBlockHeight();
            return new ReadOnlyObjectWrapper<>(blockHeight);
        });

        hashColumn.setCellValueFactory(features -> {
            Hash hash = features.getValue().getHash();
            return new ReadOnlyObjectWrapper<>(hash);
        });
        hashColumn.setCellFactory(col -> new HashTableCell<>());

        blocksTable.getSelectionModel().selectedItemProperty().addListener((obs, old, block) -> {
            if (block == null) {
                return;
            }

            blockDetailView.setBlock(block);
            setShowDetailNode(true);
        });
    }

    private void loadRejects() {
        observableBlocks.clear();
        blockchain.recentRejectsIterator().forEachRemaining(b -> observableBlocks.add(b));
    }

    @Override
    public void onRecentRejectAdded(@NotNull Block block) {
        loadRejects();
    }
}
