package org.brabocoin.brabocoin.gui.view;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.SelectableLabel;
import org.brabocoin.brabocoin.gui.control.TransactionDataMenuButton;
import org.brabocoin.brabocoin.gui.control.table.AddressTableCell;
import org.brabocoin.brabocoin.gui.control.table.BigIntegerTableCell;
import org.brabocoin.brabocoin.gui.control.table.HashTableCell;
import org.brabocoin.brabocoin.gui.control.table.NumberedTableCell;
import org.brabocoin.brabocoin.gui.control.table.PublicKeyTableCell;
import org.brabocoin.brabocoin.gui.window.ValidationWindow;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private final NodeEnvironment nodeEnvironment;
    private @Nullable TransactionValidator validator;

    @FXML private TransactionDataMenuButton transactionDataMenuButtonBottom;
    @FXML private TransactionDataMenuButton transactionDataMenuButton;
    @FXML private MenuItem menuShowUnsignedTx;
    @FXML private MenuItem menuShowSignedTx;

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

    @FXML private SelectableLabel hashField;
    @FXML private HBox header;
    @FXML private Button buttonValidate;
    @FXML private Button buttonPropagate;

    public TransactionDetailView(Transaction transaction,
                                 @Nullable TransactionValidator validator) {
        this(transaction, null, validator);
    }

    public TransactionDetailView(Transaction transaction,
                                 @Nullable NodeEnvironment nodeEnvironment,
                                 @Nullable TransactionValidator validator) {
        super();
        this.nodeEnvironment = nodeEnvironment;
        this.validator = validator;

        BraboControlInitializer.initialize(this);

        this.transaction.addListener((obs, old, val) -> {
            if (val != null) {
                loadTransaction(val);
            }
        });
        setTransaction(transaction);
    }

    private void loadTransaction(@NotNull Transaction transaction) {
        buttonValidate.setVisible(validator != null);
        buttonValidate.setManaged(validator != null);
        hashField.setText(ByteUtil.toHexString(
            transaction.getHash().getValue(),
            Constants.TRANSACTION_HASH_SIZE
        ));

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

        transactionDataMenuButtonBottom.managedProperty().bind(showHeader.not());
        transactionDataMenuButtonBottom.visibleProperty().bind(showHeader.not());

        // Input table
        inputIndexColumn.setCellFactory(col -> new NumberedTableCell<>());
        inputReferencedTxColumn.setCellValueFactory(new PropertyValueFactory<>(
            "referencedTransaction"));
        inputReferencedTxColumn.setCellFactory(col -> new HashTableCell<>(Constants.TRANSACTION_HASH_SIZE));
        inputReferencedOutputColumn.setCellValueFactory(new PropertyValueFactory<>(
            "referencedOutputIndex"));

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

        // Propagate button
        if (nodeEnvironment == null) {
            buttonPropagate.setManaged(false);
            buttonPropagate.setVisible(false);
        }
    }

    private void fitTableRowContent(TableView<?> tableView, int minRowsVisible) {
        tableView.prefHeightProperty()
            .bind(tableView.fixedCellSizeProperty()
                .multiply(Bindings.size(tableView.getItems()).add(1.01)));
        tableView.minHeightProperty()
            .bind(Bindings.max(
                tableView.prefHeightProperty(),
                tableView.fixedCellSizeProperty().multiply(minRowsVisible + 1.01)
            ));
        tableView.maxHeightProperty().bind(tableView.prefHeightProperty());
    }

    public Transaction getTransaction() {
        return transaction.get();
    }

    public void setTransaction(Transaction value) {
        transaction.setValue(value);
        transactionDataMenuButton.setTransaction(this.transaction.get());
        transactionDataMenuButtonBottom.setTransaction(this.transaction.get());
    }

    public void setShowHeader(boolean value) {
        showHeader.set(value);
    }

    public BooleanProperty showHeaderProperty() {
        return showHeader;
    }

    @FXML
    protected void validate(ActionEvent event) {
        Dialog dialog = new ValidationWindow(transaction.get(), validator);
        dialog.showAndWait();
    }

    @FXML
    protected void propagate(ActionEvent event) {
        nodeEnvironment.announceTransactionRequest(transaction.get());
    }
}
