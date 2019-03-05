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
import javafx.scene.control.TitledPane;
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
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.validation.consensus.Consensus;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;

/**
 * View of the transaction pool.
 */
public class TransactionPoolView extends CollapsibleMasterDetailPane implements BraboControl,
                                                                                Initializable,
                                                                                TransactionPoolListener {

    private final @NotNull TransactionPool pool;
    private final @NotNull TransactionValidator validator;
    private final @NotNull NodeEnvironment nodeEnvironment;
    private final Blockchain blockchain;
    private final Consensus consensus;

    @FXML private TitledPane independentPane;
    @FXML private TitledPane dependentPane;

    @FXML private TableView<Transaction> independentTable;
    @FXML private TableView<Transaction> dependentTable;

    @FXML private TableColumn<Transaction, Hash> hashIndepColumn;
    @FXML private TableColumn<Transaction, Hash> hashDepColumn;

    private @NotNull TransactionDetailView detailView;

    private final IntegerProperty count = new SimpleIntegerProperty(0);

    public TransactionPoolView(State state) {
        super();
        this.blockchain = state.getBlockchain();
        this.consensus = state.getConsensus();
        this.pool = state.getTransactionPool();
        this.validator = state.getTransactionValidator();
        this.nodeEnvironment = state.getEnvironment();
        BraboControlInitializer.initialize(this);

        this.pool.addListener(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        detailView = new TransactionDetailView(
            blockchain,
            consensus,
            null,
            nodeEnvironment,
            validator,
            false
        );
        setDetailNode(detailView);

        independentPane.textProperty().bind(
            Bindings.createStringBinding(
                () -> "Independent transactions (" + independentTable.getItems().size() + ")",
                independentTable.getItems()
            )
        );

        dependentPane.textProperty().bind(
            Bindings.createStringBinding(
                () -> "Dependent transactions (" + dependentTable.getItems().size() + ")",
                dependentTable.getItems()
            )
        );

        hashIndepColumn.setCellValueFactory(features -> {
            Hash hash = features.getValue().getHash();
            return new ReadOnlyObjectWrapper<>(hash);
        });
        hashIndepColumn.setCellFactory(col -> new HashTableCell<>(Constants.TRANSACTION_HASH_SIZE));

        hashDepColumn.setCellValueFactory(features -> {
            Hash hash = features.getValue().getHash();
            return new ReadOnlyObjectWrapper<>(hash);
        });
        hashDepColumn.setCellFactory(col -> new HashTableCell<>(Constants.TRANSACTION_HASH_SIZE));

        independentTable.getSelectionModel().selectedItemProperty().addListener((obs, old, tx) -> {
            if (tx == null) {
                return;
            }

            detailView.setTransaction(tx);
            setShowDetailNode(true);
        });

        dependentTable.getSelectionModel().selectedItemProperty().addListener((obs, old, tx) -> {
            if (tx == null) {
                return;
            }

            detailView.setTransaction(tx);
            setShowDetailNode(true);
        });

        count.bind(Bindings.add(
            Bindings.size(independentTable.getItems()),
            Bindings.size(dependentTable.getItems())
        ));

        this.registerTableView(independentTable);
        this.registerTableView(dependentTable);
    }

    @Override
    public void onTransactionAddedToPool(@NotNull Transaction transaction) {
        Platform.runLater(() -> {
            if (pool.isDependent(transaction.getHash())) {
                dependentTable.getItems().add(transaction);
            }
            else {
                independentTable.getItems().add(transaction);
            }
        });
    }

    @Override
    public void onTransactionRemovedFromPool(@NotNull Transaction transaction) {
        Platform.runLater(() -> {
            dependentTable.getItems().remove(transaction);
            independentTable.getItems().remove(transaction);
        });
    }

    @Override
    public void onDependentTransactionsPromoted(@NotNull Collection<Transaction> transactions) {
        Platform.runLater(() -> {
            dependentTable.getItems().removeAll(transactions);
            independentTable.getItems().addAll(transactions);
        });
    }

    @Override
    public void onIndependentTransactionsDemoted(@NotNull Collection<Transaction> transactions) {
        Platform.runLater(() -> {
            independentTable.getItems().removeAll(transactions);
            dependentTable.getItems().addAll(transactions);
        });
    }

    public ReadOnlyIntegerProperty countProperty() {
        return count;
    }

    public int getCount() {
        return count.get();
    }
}
