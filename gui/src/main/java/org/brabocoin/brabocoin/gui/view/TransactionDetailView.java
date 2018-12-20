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
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.table.AddressTableCell;
import org.brabocoin.brabocoin.gui.control.table.BigIntegerTableCell;
import org.brabocoin.brabocoin.gui.control.table.HashTableCell;
import org.brabocoin.brabocoin.gui.control.table.NumberedTableCell;
import org.brabocoin.brabocoin.gui.control.table.PublicKeyTableCell;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Transaction detail view.
 * <p>
 * Side pane that shows transaction content.
 */
public class TransactionDetailView extends VBox implements BraboControl, Initializable {

    private final ObjectProperty<Transaction> transaction = new SimpleObjectProperty<>();
    private final BooleanProperty showHeader = new SimpleBooleanProperty(true);

    @FXML private TableView<Input> inputTableView;
    @FXML private TableColumn<Input, Integer> inputIndexColumn;
    @FXML private TableColumn<Input, Hash> inputReferencedTxColumn;
    @FXML private TableColumn<Input, Integer> inputReferencedOutputColumn;

    @FXML private TableView<Output> outputTableView;
    @FXML private TableColumn<Output, Integer> outputIndexColumn;
    @FXML private TableColumn<Output, Hash> outputAddressColumn;
    @FXML private TableColumn<Output, Integer> outputAmountColumn;

    @FXML private TableView<Signature> signatureTableView;
    @FXML private TableColumn<Signature, Integer> sigIndexColumn;
    @FXML private TableColumn<Signature, BigInteger> sigRColumn;
    @FXML private TableColumn<Signature, BigInteger> sigSColumn;
    @FXML private TableColumn<Signature, PublicKey> sigPubKeyColumn;

    @FXML private TextField hashField;
    @FXML private Label titleLabel;
    @FXML private HBox header;

    public TransactionDetailView(Transaction transaction) {
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

        inputTableView.getItems().setAll(transaction.getInputs());
        outputTableView.getItems().setAll(transaction.getOutputs());
        signatureTableView.getItems().setAll(transaction.getSignatures());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind table heights to fit row content
        fitTableRowContent(inputTableView, 3);
        fitTableRowContent(outputTableView, 3);
        fitTableRowContent(signatureTableView, 3);

        // Bind show header property
        header.managedProperty().bind(showHeader);
        header.visibleProperty().bind(showHeader);

        // Input table
        inputIndexColumn.setCellFactory(col -> new NumberedTableCell<>());
        inputReferencedTxColumn.setCellValueFactory(new PropertyValueFactory<>("referencedTransaction"));
        inputReferencedTxColumn.setCellFactory(col -> new HashTableCell<>(Constants.TRANSACTION_HASH_SIZE));
        inputReferencedOutputColumn.setCellValueFactory(new PropertyValueFactory<>("referencedOutputIndex"));

        // Output table
        outputIndexColumn.setCellFactory(col -> new NumberedTableCell<>());
        outputAddressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        outputAddressColumn.setCellFactory(col -> new AddressTableCell<>());
        outputAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        // Signature table
        sigIndexColumn.setCellFactory(col -> new NumberedTableCell<>());

        sigRColumn.setCellValueFactory(new PropertyValueFactory<>("r"));
        sigRColumn.setCellFactory(col -> new BigIntegerTableCell<>(Constants.HEX));
        sigSColumn.setCellValueFactory(new PropertyValueFactory<>("s"));
        sigSColumn.setCellFactory(col -> new BigIntegerTableCell<>(Constants.HEX));
        sigPubKeyColumn.setCellValueFactory(new PropertyValueFactory<>("publicKey"));
        sigPubKeyColumn.setCellFactory(col -> new PublicKeyTableCell<>());
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
