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
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.TransactionPoolListener;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.CollapsibleMasterDetailPane;
import org.brabocoin.brabocoin.gui.control.table.HashTableCell;
import org.brabocoin.brabocoin.gui.control.table.RuleTableCell;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.RejectedTransaction;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.rule.Rule;
import org.brabocoin.brabocoin.validation.rule.RuleBookFailMarker;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * View for recently rejected transactions.
 */
public class RecentRejectTxView extends CollapsibleMasterDetailPane implements BraboControl,
                                                                               Initializable,
                                                                               TransactionPoolListener {

    private final Blockchain blockchain;
    private final Consensus consensus;
    private TransactionDetailView transactionDetailView;

    @FXML private TableView<RejectedTransaction> txsTable;
    @FXML private TableColumn<RejectedTransaction, Hash> hashColumn;
    @FXML private TableColumn<RejectedTransaction, Class<? extends Rule>> ruleColumn;

    private final @NotNull TransactionPool pool;
    private final NodeEnvironment nodeEnvironment;
    private final @NotNull TransactionValidator validator;
    private ObservableList<RejectedTransaction> observableTxs = FXCollections.observableArrayList();

    private final IntegerProperty count = new SimpleIntegerProperty(0);

    public RecentRejectTxView(State state) {
        super();
        this.blockchain = state.getBlockchain();
        this.consensus = state.getConsensus();
        this.pool = state.getTransactionPool();
        this.nodeEnvironment = state.getEnvironment();
        this.validator = state.getTransactionValidator();

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadTable();

        transactionDetailView = new TransactionDetailView(
            blockchain,
            consensus,
            null,
            nodeEnvironment,
            validator,
            false
        );
        setDetailNode(transactionDetailView);

        loadRejects();
        pool.addListener(this);

        count.bind(Bindings.size(txsTable.getItems()));
        this.registerTableView(txsTable);
    }

    private void loadTable() {
        txsTable.setItems(observableTxs);

        hashColumn.setCellValueFactory(features -> {
            Hash hash = features.getValue().getTransaction().getHash();
            return new ReadOnlyObjectWrapper<>(hash);
        });
        hashColumn.setCellFactory(col -> new HashTableCell<>(Constants.BLOCK_HASH_SIZE));

        ruleColumn.setCellValueFactory(features -> {
            RuleBookFailMarker marker = features.getValue().getValidationResult().getFailMarker();
            return new ReadOnlyObjectWrapper<>(marker.getFailedRule());
        });
        ruleColumn.setCellFactory(col -> new RuleTableCell<>());

        txsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, tx) -> {
            if (tx == null) {
                return;
            }

            transactionDetailView.setTransaction(tx.getTransaction());
            setShowDetailNode(true);
        });
    }

    private void loadRejects() {
        observableTxs.clear();
        pool.recentRejectsIterator().forEachRemaining(b -> observableTxs.add(b));
    }

    @Override
    public void onRecentRejectAdded(@NotNull Transaction transaction) {
        Platform.runLater(this::loadRejects);
    }

    public ReadOnlyIntegerProperty countProperty() {
        return count;
    }

    public int getCount() {
        return count.get();
    }
}
