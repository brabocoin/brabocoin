package org.brabocoin.brabocoin.gui.view;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.SelectableLabel;
import org.brabocoin.brabocoin.gui.control.table.AddressTableCell;
import org.brabocoin.brabocoin.gui.control.table.BalanceTableCell;
import org.brabocoin.brabocoin.gui.control.table.NumberedTableCell;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Coinbase transaction detail view.
 * <p>
 * Side pane that shows the coinbase transaction content.
 */
public class CoinbaseDetailView extends VBox implements BraboControl, Initializable {

    private final ObjectProperty<Transaction> transaction = new SimpleObjectProperty<>();
    private final BooleanProperty showHeader = new SimpleBooleanProperty(true);

    @FXML private TableView<Output> outputTableView;
    @FXML private TableColumn<Output, Integer> outputIndexColumn;
    @FXML private TableColumn<Output, Hash> outputAddressColumn;
    @FXML private TableColumn<Output, Long> outputAmountColumn;

    @FXML private SelectableLabel hashField;
    @FXML private Label titleLabel;
    @FXML private HBox header;

    public CoinbaseDetailView(Transaction transaction) {
        super();

        BraboControlInitializer.initialize(this);

        this.transaction.addListener((obs, old, val) -> {
            if (val != null) {
                loadTransction(val);
            }
        });
        setTransaction(transaction);
    }

    private void loadTransction(@NotNull Transaction transaction) {
        hashField.setText(ByteUtil.toHexString(transaction.getHash().getValue(), Constants.TRANSACTION_HASH_SIZE));
        outputTableView.getItems().setAll(transaction.getOutputs());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind show header property
        header.managedProperty().bind(showHeader);
        header.visibleProperty().bind(showHeader);

        fitTableRowContent(outputTableView, 3);

        // Output table
        outputIndexColumn.setCellFactory(col -> new NumberedTableCell<>());
        outputAddressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        outputAddressColumn.setCellFactory(col -> new AddressTableCell<>());
        outputAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        outputAmountColumn.setCellFactory(col -> new BalanceTableCell<>());
    }

    private void fitTableRowContent(TableView<?> tableView, int minRowsVisible) {
        tableView.prefHeightProperty().bind(tableView.fixedCellSizeProperty().multiply(Bindings.size(tableView.getItems()).add(1.01)));
        tableView.minHeightProperty().bind(Bindings.max(tableView.prefHeightProperty(), tableView.fixedCellSizeProperty().multiply(minRowsVisible + 1.01)));
        tableView.maxHeightProperty().bind(tableView.prefHeightProperty());
    }

    public Transaction getTransaction() {
        return transaction.get();
    }

    public void setTransaction(Transaction value) {
        if (!value.isCoinbase()) {
            throw new IllegalArgumentException("Transaction is not coinbase.");
        }
        transaction.setValue(value);
    }

    public void setShowHeader(boolean value) {
        showHeader.set(value);
    }

    public boolean isShowHeader() {
        return showHeader.get();
    }

    public BooleanProperty showHeaderProperty() {
        return showHeader;
    }
}
