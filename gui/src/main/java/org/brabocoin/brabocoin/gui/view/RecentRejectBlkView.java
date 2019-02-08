package org.brabocoin.brabocoin.gui.view;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.BlockchainListener;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.table.HashTableCell;
import org.brabocoin.brabocoin.gui.control.table.RuleTableCell;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.RejectedBlock;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.rule.Rule;
import org.brabocoin.brabocoin.validation.rule.RuleBookFailMarker;
import org.controlsfx.control.MasterDetailPane;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * View to display a collection of blocks, with detail view.
 */
public class RecentRejectBlkView extends MasterDetailPane implements BraboControl, Initializable, BlockchainListener {

    private BlockDetailView blockDetailView;

    @FXML private TableView<RejectedBlock> blocksTable;
    @FXML private TableColumn<RejectedBlock, Integer> heightColumn;
    @FXML private TableColumn<RejectedBlock, Hash> hashColumn;
    @FXML private TableColumn<RejectedBlock, Class<? extends Rule>> ruleColumn;

    private final @NotNull Blockchain blockchain;
    private final BlockValidator validator;
    private ObservableList<RejectedBlock> observableBlocks = FXCollections.observableArrayList();

    private final IntegerProperty count = new SimpleIntegerProperty(0);

    public RecentRejectBlkView(@NotNull Blockchain blockchain, @NotNull BlockValidator validator) {
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

        count.bind(Bindings.size(blocksTable.getItems()));
    }

    private void loadTable() {
        blocksTable.setItems(observableBlocks);

        heightColumn.setCellValueFactory(features -> {
            int blockHeight = features.getValue().getBlock().getBlockHeight();
            return new ReadOnlyObjectWrapper<>(blockHeight);
        });

        hashColumn.setCellValueFactory(features -> {
            Hash hash = features.getValue().getBlock().getHash();
            return new ReadOnlyObjectWrapper<>(hash);
        });
        hashColumn.setCellFactory(col -> new HashTableCell<>(Constants.BLOCK_HASH_SIZE));

        ruleColumn.setCellValueFactory(features -> {
            RuleBookFailMarker marker = features.getValue().getValidationResult().getFailMarker();
            return new ReadOnlyObjectWrapper<>(marker.getFailedRule());
        });
        ruleColumn.setCellFactory(col -> new RuleTableCell<>());

        blocksTable.getSelectionModel().selectedItemProperty().addListener((obs, old, block) -> {
            if (block == null) {
                return;
            }

            blockDetailView.setBlock(block.getBlock());
            setShowDetailNode(true);
        });
    }

    private void loadRejects() {
        observableBlocks.clear();
        blockchain.recentRejectsIterator().forEachRemaining(b -> observableBlocks.add(b));
    }

    @Override
    public void onRecentRejectAdded(@NotNull Block block) {
        Platform.runLater(this::loadRejects);
    }

    public ReadOnlyIntegerProperty countProperty() {
        return count;
    }

    public int getCount() {
        return count.get();
    }
}
