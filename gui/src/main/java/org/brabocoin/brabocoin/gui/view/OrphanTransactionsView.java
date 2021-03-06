package org.brabocoin.brabocoin.gui.view;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.TransactionPoolListener;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.CollapsibleMasterDetailPane;
import org.brabocoin.brabocoin.gui.control.table.HashTableCell;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.validation.consensus.Consensus;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * View of the orphan transactions.
 */
public class OrphanTransactionsView extends CollapsibleMasterDetailPane implements BraboControl, Initializable, TransactionPoolListener {

    private final @NotNull TransactionPool pool;
    private final @NotNull TransactionValidator validator;
    private final Blockchain blockchain;
    private final Consensus consensus;

    @FXML private TableView<Transaction> orphanTable;

    @FXML private TableColumn<Transaction, Hash> hashColumn;

    private @NotNull TransactionDetailView detailView;

    private final IntegerProperty count = new SimpleIntegerProperty(0);

    public OrphanTransactionsView(State state) {
        super();
        this.blockchain = state.getBlockchain();
        this.consensus = state.getConsensus();
        this.pool = state.getTransactionPool();
        this.validator = state.getTransactionValidator();
        BraboControlInitializer.initialize(this);

        this.pool.addListener(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        detailView = new TransactionDetailView(blockchain, consensus, null, validator, false);
        setDetailNode(detailView);

        hashColumn.setCellValueFactory(features -> {
            Hash hash = features.getValue().getHash();
            return new ReadOnlyObjectWrapper<>(hash);
        });
        hashColumn.setCellFactory(col -> new HashTableCell<>(Constants.TRANSACTION_HASH_SIZE));

        orphanTable.getSelectionModel().selectedItemProperty().addListener((obs, old, tx) -> {
            if (tx == null) {
                return;
            }

            detailView.setTransaction(tx);
            setShowDetailNode(true);
        });

        this.count.bind(Bindings.size(orphanTable.getItems()));

        this.registerTableView(orphanTable);
    }

    @Override
    public void onTransactionAddedAsOrphan(@NotNull Transaction transaction) {
        Platform.runLater(() -> orphanTable.getItems().add(transaction));
    }

    @Override
    public void onTransactionRemovedAsOrphan(@NotNull Transaction transaction) {
        Platform.runLater(() -> orphanTable.getItems().remove(transaction));
    }

    public ReadOnlyIntegerProperty countProperty() {
        return count;
    }

    public int getCount() {
        return count.get();
    }
}
