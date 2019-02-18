package org.brabocoin.brabocoin.gui.view.wallet;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.table.HashTableCell;
import org.brabocoin.brabocoin.gui.view.CoinbaseDetailView;
import org.brabocoin.brabocoin.gui.view.TransactionDetailView;
import org.brabocoin.brabocoin.listeners.TransactionHistoryListener;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.brabocoin.brabocoin.wallet.ConfirmedTransaction;
import org.brabocoin.brabocoin.wallet.TransactionHistory;
import org.controlsfx.control.MasterDetailPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

/**
 * Transaction history in the wallet.
 */
public class TransactionHistoryView extends MasterDetailPane implements BraboControl, Initializable, TransactionHistoryListener {

    private final @NotNull TransactionHistory transactionHistory;
    private final @Nullable TransactionValidator validator;
    private Node txDetailiew;

    @FXML private TableView<ConfirmedTransaction> confirmedTable;
    @FXML private TableView<Transaction> unconfirmedTable;

    @FXML private TableColumn<ConfirmedTransaction, LocalDateTime> dateConfColumn;
    @FXML private TableColumn<ConfirmedTransaction, Integer> blockHeightConfColumn;
    @FXML private TableColumn<ConfirmedTransaction, Hash> hashConfColumn;
    @FXML private TableColumn<ConfirmedTransaction, Long> amountConfColumn;

    @FXML private TableColumn<Transaction, LocalDateTime> dateUnconfColumn;
    @FXML private TableColumn<Transaction, Hash> hashUnconfColumn;
    @FXML private TableColumn<Transaction, Long> amountUnconfColumn;

    public TransactionHistoryView(@NotNull TransactionHistory transactionHistory,
                                  @Nullable TransactionValidator validator) {
        super();
        this.transactionHistory = transactionHistory;
        this.validator = validator;
        this.transactionHistory.addListener(this);
        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        blockHeightConfColumn.setCellValueFactory(features -> {
            int blockHeight = features.getValue().getBlockHeight();
            return new ReadOnlyObjectWrapper<>(blockHeight);
        });

        hashConfColumn.setCellValueFactory(features -> {
            Hash hash = features.getValue().getHash();
            return new ReadOnlyObjectWrapper<>(hash);
        });
        hashConfColumn.setCellFactory(col -> new HashTableCell<>(Constants.TRANSACTION_HASH_SIZE));

        hashUnconfColumn.setCellValueFactory(features -> {
            Hash hash = features.getValue().getHash();
            return new ReadOnlyObjectWrapper<>(hash);
        });
        hashUnconfColumn.setCellFactory(col -> new HashTableCell<>(Constants.TRANSACTION_HASH_SIZE));

        confirmedTable.getSelectionModel().selectedItemProperty().addListener((obs, old, tx) -> {
            if (tx != null) {
                Transaction transaction = tx.getTransaction();
                if (transaction.isCoinbase()) {
                    setDetailNode(new CoinbaseDetailView(transaction));
                }
                else {
                    setDetailNode(new TransactionDetailView(transaction, validator));
                }
                setShowDetailNode(true);
            }
        });

        unconfirmedTable.getSelectionModel().selectedItemProperty().addListener((obs, old, tx) -> {
            if (tx != null) {
                if (tx.isCoinbase()) {
                    setDetailNode(new CoinbaseDetailView(tx));
                }
                else {
                    setDetailNode(new TransactionDetailView(tx, validator));
                }
                setShowDetailNode(true);
            }
        });

        confirmedTable.setItems(FXCollections.observableArrayList(transactionHistory.getConfirmedTransactions()));
        unconfirmedTable.setItems(FXCollections.observableArrayList(transactionHistory.getUnconfirmedTransactions()));
    }

    @Override
    public void onConfirmedTransactionAdded(ConfirmedTransaction transaction) {
        confirmedTable.getItems().add(transaction);
    }

    @Override
    public void onConfirmedTransactionRemoved(ConfirmedTransaction transaction) {
        confirmedTable.getItems().remove(transaction);
    }

    @Override
    public void onUnconfirmedTransactionAdded(Transaction transaction) {
        unconfirmedTable.getItems().add(transaction);
    }

    @Override
    public void onUnconfirmedTransactionRemoved(Transaction transaction) {
        unconfirmedTable.getItems().remove(transaction);
    }
}
