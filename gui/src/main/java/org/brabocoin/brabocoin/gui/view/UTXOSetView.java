package org.brabocoin.brabocoin.gui.view;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.UTXOSetListener;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.table.AddressTableCell;
import org.brabocoin.brabocoin.gui.control.table.BalanceTableCell;
import org.brabocoin.brabocoin.gui.control.table.BooleanTextTableCell;
import org.brabocoin.brabocoin.gui.control.table.HashTableCell;
import org.brabocoin.brabocoin.gui.control.table.IntegerTableCell;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.wallet.Wallet;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.AbstractMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * View to display a UTXO database.
 */
public class UTXOSetView extends VBox implements BraboControl, Initializable, UTXOSetListener {

    @FXML private TableView<Map.Entry<Input, UnspentOutputInfo>> utxoTable;
    @FXML private TableColumn<Map.Entry<Input, UnspentOutputInfo>, Integer> heightColumn;
    @FXML private TableColumn<Map.Entry<Input, UnspentOutputInfo>, Hash> txHashColumn;
    @FXML private TableColumn<Map.Entry<Input, UnspentOutputInfo>, Integer> outputColumn;
    @FXML private TableColumn<Map.Entry<Input, UnspentOutputInfo>, Hash> addressColumn;
    @FXML private TableColumn<Map.Entry<Input, UnspentOutputInfo>, Long> amountColumn;
    @FXML private TableColumn<Map.Entry<Input, UnspentOutputInfo>, Boolean> coinbaseColumn;
    @FXML private TableColumn<Map.Entry<Input, UnspentOutputInfo>, Boolean> inMyWalletColumn;

    private final @NotNull ReadonlyUTXOSet utxoSet;
    private final @NotNull Wallet wallet;
    private ObservableList<Map.Entry<Input, UnspentOutputInfo>> observableEntries = FXCollections.observableArrayList();

    public UTXOSetView(@NotNull ReadonlyUTXOSet utxoSet, @NotNull Wallet wallet) {
        super();
        this.utxoSet = utxoSet;
        this.wallet = wallet;

        BraboControlInitializer.initialize(this);
    }

    @Override
    public @NotNull String resourceName() {
        return "utxo_set_view";
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadTable();

        loadUTXOSet();
        utxoSet.addListener(this);
    }

    private void loadTable() {
        utxoTable.setItems(observableEntries);

        heightColumn.setCellValueFactory(features -> {
            int blockHeight = features.getValue().getValue().getBlockHeight();
            return new ReadOnlyObjectWrapper<>(blockHeight);
        });
        heightColumn.setCellFactory(col -> new IntegerTableCell<>());

        txHashColumn.setCellValueFactory(features -> {
            Hash hash = features.getValue().getKey().getReferencedTransaction();
            return new ReadOnlyObjectWrapper<>(hash);
        });
        txHashColumn.setCellFactory(col -> new HashTableCell<>(Constants.TRANSACTION_HASH_SIZE));

        outputColumn.setCellValueFactory(features -> {
            int index = features.getValue().getKey().getReferencedOutputIndex();
            return new ReadOnlyObjectWrapper<>(index);
        });
        outputColumn.setCellFactory(col -> new IntegerTableCell<>());

        addressColumn.setCellValueFactory(features -> {
            Hash address = features.getValue().getValue().getAddress();
            return new ReadOnlyObjectWrapper<>(address);
        });
        addressColumn.setCellFactory(col -> new AddressTableCell<>());

        amountColumn.setCellValueFactory(features -> {
            long amount = features.getValue().getValue().getAmount();
            return new ReadOnlyObjectWrapper<>(amount);
        });
        amountColumn.setCellFactory(col -> new BalanceTableCell<>());

        coinbaseColumn.setCellValueFactory(features -> {
            boolean coinbase = features.getValue().getValue().isCoinbase();
            return new ReadOnlyObjectWrapper<>(coinbase);
        });
        coinbaseColumn.setCellFactory(col -> new BooleanTextTableCell<>("Yes", "No"));

        inMyWalletColumn.setCellValueFactory(features -> {
            Hash address = features.getValue().getValue().getAddress();
            return new ReadOnlyObjectWrapper<>(wallet.hasAddress(address));
        });
        inMyWalletColumn.setCellFactory(col -> new BooleanTextTableCell<>("Yes", "No"));

        utxoTable.getSortOrder().add(heightColumn);
        utxoTable.getSortOrder().add(txHashColumn);
        utxoTable.getSortOrder().add(outputColumn);
    }

    private void loadUTXOSet() {
        utxoSet.iterator().forEachRemaining(e -> observableEntries.add(e));
        utxoTable.sort();
    }

    @Override
    public void onOutputUnspent(@NotNull Hash transactionHash, int outputIndex,
                                @NotNull UnspentOutputInfo info) {
        observableEntries.add(new UTXOEntry(new Input(transactionHash, outputIndex), info));
        utxoTable.sort();
    }

    @Override
    public void onOutputSpent(@NotNull Hash transactionHash, int outputIndex) {
        observableEntries.remove(new UTXOEntry(new Input(transactionHash, outputIndex), null));
    }

    private static class UTXOEntry extends AbstractMap.SimpleEntry<Input, UnspentOutputInfo> {

        public UTXOEntry(Input key, UnspentOutputInfo value) {
            super(key, value);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            return getKey().equals(e.getKey());
        }

        @Override
        public int hashCode() {
            return (getKey() == null ? 0 : getKey().hashCode());
        }
    }
}
