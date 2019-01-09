package org.brabocoin.brabocoin.gui.view;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.TransactionPoolListener;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.table.HashTableCell;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.controlsfx.control.MasterDetailPane;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * View of the transaction pool.
 */
public class TransactionPoolView extends MasterDetailPane implements BraboControl, Initializable, TransactionPoolListener {

    private final @NotNull TransactionPool pool;

    @FXML private TitledPane independentPane;
    @FXML private TitledPane dependentPane;
    @FXML private TitledPane orphanPane;

    @FXML private TableView<Transaction> independentTable;
    @FXML private TableView<Transaction> dependentTable;
    @FXML private TableView<Transaction> orphanTable;

    @FXML private TableColumn<Transaction, Hash> hashIndepColumn;
    @FXML private TableColumn<Transaction, Hash> hashDepColumn;
    @FXML private TableColumn<Transaction, Hash> hashOrphanColumn;

    private @NotNull TransactionDetailView detailView;

    public TransactionPoolView(@NotNull TransactionPool pool) {
        super();
        this.pool = pool;
        BraboControlInitializer.initialize(this);

        this.pool.addListener(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        detailView = new TransactionDetailView(null);
        setDetailNode(detailView);

        independentPane.textProperty().bind(
            Bindings.createStringBinding(
                () -> "Independent transactions (" + independentTable.getItems().size() + ")",
                Bindings.size(independentTable.getItems())
            )
        );

        dependentPane.textProperty().bind(
            Bindings.createStringBinding(
                () -> "Dependent transactions (" + dependentTable.getItems().size() + ")",
                Bindings.size(dependentTable.getItems())
            )
        );

        orphanPane.textProperty().bind(
            Bindings.createStringBinding(
                () -> "Orphan transactions (" + orphanTable.getItems().size() + ")",
                Bindings.size(orphanTable.getItems())
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

        hashOrphanColumn.setCellValueFactory(features -> {
            Hash hash = features.getValue().getHash();
            return new ReadOnlyObjectWrapper<>(hash);
        });
        hashOrphanColumn.setCellFactory(col -> new HashTableCell<>(Constants.TRANSACTION_HASH_SIZE));

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

        orphanTable.getSelectionModel().selectedItemProperty().addListener((obs, old, tx) -> {
            if (tx == null) {
                return;
            }

            detailView.setTransaction(tx);
            setShowDetailNode(true);
        });
    }

    @Override
    public void onTransactionAddedToPool(@NotNull Transaction transaction) {
        if (pool.isDependent(transaction.getHash())) {
            dependentTable.getItems().add(transaction);
        }
        else {
            independentTable.getItems().add(transaction);
        }
    }

    @Override
    public void onTransactionAddedAsOrphan(@NotNull Transaction transaction) {
        orphanTable.getItems().add(transaction);
    }

    @Override
    public void onTransactionRemovedFromPool(@NotNull Transaction transaction) {
        dependentTable.getItems().remove(transaction);
        independentTable.getItems().remove(transaction);
    }

    @Override
    public void onTransactionRemovedAsOrphan(@NotNull Transaction transaction) {
        orphanTable.getItems().remove(transaction);
    }
}
