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

    @FXML private TableView<Transaction> independentTable;
    @FXML private TableView<Transaction> dependentTable;

    @FXML private TableColumn<Transaction, Hash> hashIndepColumn;
    @FXML private TableColumn<Transaction, Hash> hashDepColumn;

    private @NotNull TransactionDetailView detailView;

    private final IntegerProperty count = new SimpleIntegerProperty(0);

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

    public ReadOnlyIntegerProperty countProperty() {
        return count;
    }

    public int getCount() {
        return count.get();
    }
}
