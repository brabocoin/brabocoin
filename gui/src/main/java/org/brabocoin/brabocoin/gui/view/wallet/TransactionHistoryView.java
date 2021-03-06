package org.brabocoin.brabocoin.gui.view.wallet;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.CollapsibleMasterDetailPane;
import org.brabocoin.brabocoin.gui.control.table.ColoredBalanceTableCell;
import org.brabocoin.brabocoin.gui.control.table.DateTimeTableCell;
import org.brabocoin.brabocoin.gui.control.table.HashTableCell;
import org.brabocoin.brabocoin.gui.control.table.IntegerTableCell;
import org.brabocoin.brabocoin.gui.view.CoinbaseDetailView;
import org.brabocoin.brabocoin.gui.view.TransactionDetailView;
import org.brabocoin.brabocoin.listeners.TransactionHistoryListener;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.wallet.ConfirmedTransaction;
import org.brabocoin.brabocoin.wallet.UnconfirmedTransaction;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ResourceBundle;

/**
 * Transaction history in the wallet.
 */
public class TransactionHistoryView extends CollapsibleMasterDetailPane implements BraboControl,
                                                                                   Initializable,
                                                                                   TransactionHistoryListener {

    @NotNull private final State state;

    @FXML private TableView<ConfirmedTransaction> confirmedTable;
    @FXML private TableView<UnconfirmedTransaction> unconfirmedTable;

    @FXML private TableColumn<ConfirmedTransaction, LocalDateTime> dateConfColumn;
    @FXML private TableColumn<ConfirmedTransaction, Integer> blockHeightConfColumn;
    @FXML private TableColumn<ConfirmedTransaction, Hash> hashConfColumn;
    @FXML private TableColumn<ConfirmedTransaction, Long> amountConfColumn;

    @FXML private TableColumn<UnconfirmedTransaction, LocalDateTime> dateUnconfColumn;
    @FXML private TableColumn<UnconfirmedTransaction, Hash> hashUnconfColumn;
    @FXML private TableColumn<UnconfirmedTransaction, Long> amountUnconfColumn;

    public TransactionHistoryView(State state) {
        super();
        this.state = state;
        this.state.getWallet().getTransactionHistory().addListener(this);
        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dateConfColumn.setCellValueFactory(features -> {
            long timestamp = features.getValue().getTimeReceived();
            Instant instant = Instant.ofEpochSecond(timestamp);
            LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return new ReadOnlyObjectWrapper<>(time);
        });
        dateConfColumn.setCellFactory(col -> new DateTimeTableCell<>());

        dateUnconfColumn.setCellValueFactory(features -> {
            long timestamp = features.getValue().getTimeReceived();
            Instant instant = Instant.ofEpochSecond(timestamp);
            LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return new ReadOnlyObjectWrapper<>(time);
        });
        dateUnconfColumn.setCellFactory(col -> new DateTimeTableCell<>());

        blockHeightConfColumn.setCellValueFactory(features -> {
            int blockHeight = features.getValue().getBlockHeight();
            return new ReadOnlyObjectWrapper<>(blockHeight);
        });
        blockHeightConfColumn.setCellFactory(col -> new IntegerTableCell<>());

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

        amountConfColumn.setCellValueFactory(features -> {
            long amount = features.getValue().getAmount();
            return new ReadOnlyObjectWrapper<>(amount);
        });
        amountConfColumn.setCellFactory(col -> new ColoredBalanceTableCell<>());

        amountUnconfColumn.setCellValueFactory(features -> {
            long amount = features.getValue().getAmount();
            return new ReadOnlyObjectWrapper<>(amount);
        });
        amountUnconfColumn.setCellFactory(col -> new ColoredBalanceTableCell<>());

        confirmedTable.getSelectionModel().selectedItemProperty().addListener((obs, old, tx) -> {
            if (tx != null) {
                unconfirmedTable.getSelectionModel().clearSelection();
                Transaction transaction = tx.getTransaction();
                if (transaction.isCoinbase()) {
                    setDetailNode(new CoinbaseDetailView(transaction));
                }
                else {
                    setDetailNode(new TransactionDetailView(
                        state.getBlockchain(),
                        state.getConsensus(),
                        transaction,
                        state.getTransactionValidator(),
                        true
                    ));
                }
                setShowDetailNode(true);
            }
        });

        unconfirmedTable.getSelectionModel().selectedItemProperty().addListener((obs, old, tx) -> {
            if (tx != null) {
                confirmedTable.getSelectionModel().clearSelection();
                if (tx.getTransaction().isCoinbase()) {
                    setDetailNode(new CoinbaseDetailView(tx.getTransaction()));
                }
                else {
                    setDetailNode(new TransactionDetailView(
                        state.getBlockchain(),
                        state.getConsensus(),
                        tx.getTransaction(),
                        state.getTransactionValidator(),
                        false
                    ));
                }
                setShowDetailNode(true);
            }
        });

        confirmedTable.setItems(FXCollections.observableArrayList(state.getWallet()
            .getTransactionHistory()
            .getConfirmedTransactions()
            .values()));
        unconfirmedTable.setItems(FXCollections.observableArrayList(state.getWallet()
            .getTransactionHistory()
            .getUnconfirmedTransactions()
            .values()));

        this.registerTableView(confirmedTable);
        this.registerTableView(unconfirmedTable);
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
    public void onUnconfirmedTransactionAdded(UnconfirmedTransaction transaction) {
        unconfirmedTable.getItems().add(transaction);
    }

    @Override
    public void onUnconfirmedTransactionRemoved(UnconfirmedTransaction transaction) {
        unconfirmedTable.getItems().remove(transaction);
    }
}
