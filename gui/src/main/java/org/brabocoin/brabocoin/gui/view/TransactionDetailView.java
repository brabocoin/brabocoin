package org.brabocoin.brabocoin.gui.view;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Signature;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Transaction detail view.
 * <p>
 * Side pane that shows transaction content.
 */
public class TransactionDetailView extends VBox implements BraboControl, Initializable {

    private final ObjectProperty<Transaction> transaction = new SimpleObjectProperty<>();
    @FXML private TableView<TableSignatureEntry> signatureTableView;
    @FXML private TableView<TableOutputEntry> outputTableView;
    @FXML private TableView<TableInputEntry> inputTableView;
    @FXML private TextField hashField;
    @FXML private Label titleLabel;

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
        titleLabel.setText("Transaction");
        hashField.setText(ByteUtil.toHexString(transaction.getHash().getValue(), 32));

        // Remove all rows
        inputTableView.getItems().clear();

        @NotNull List<Input> inputs = transaction.getInputs();

        for (int i = 0; i < inputs.size(); i++) {
            Input input = inputs.get(i);

            inputTableView.getItems().add(
                new TableInputEntry(input, i)
            );
        }

        @NotNull List<Output> outputs = transaction.getOutputs();

        for (int i = 0; i < outputs.size(); i++) {
            Output output = outputs.get(i);

            outputTableView.getItems().add(
                new TableOutputEntry(output, i)
            );
        }

        @NotNull List<Signature> signatures = transaction.getSignatures();

        for (int i = 0; i < signatures.size(); i++) {
            Signature signature = signatures.get(i);

            signatureTableView.getItems().add(
                new TableSignatureEntry(signature, i)
            );
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<TableInputEntry, Integer> inputIndex = new TableColumn<>("Index");
        inputIndex.setCellValueFactory(new PropertyValueFactory<>("index"));

        inputIndex.setMinWidth(50.0);
        inputIndex.setMaxWidth(50.0);

        TableColumn<TableInputEntry, String> refTxHash = new TableColumn<>("Referenced Tx Hash");
        refTxHash.setCellValueFactory(new PropertyValueFactory<>("referencedTransactionHash"));
        TableColumn<TableInputEntry, Integer> refOutputIndex = new TableColumn<>("Output Index");
        refOutputIndex.setCellValueFactory(new PropertyValueFactory<>("referencedOutputIndex"));

        inputTableView.getColumns().addAll(
            inputIndex, refTxHash, refOutputIndex
        );

        TableColumn<TableOutputEntry, Integer> outputIndex = new TableColumn<>("Index");
        outputIndex.setCellValueFactory(new PropertyValueFactory<>("index"));

        outputIndex.setMinWidth(50.0);
        outputIndex.setMaxWidth(50.0);

        TableColumn<TableOutputEntry, String> address = new TableColumn<>("Address");
        address.setCellValueFactory(new PropertyValueFactory<>("addressHash"));
        TableColumn<TableOutputEntry, Integer> amount = new TableColumn<>("Amount");
        amount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        outputTableView.getColumns().addAll(
            outputIndex, address, amount
        );

        TableColumn<TableSignatureEntry, Integer> signatureIndex = new TableColumn<>("Index");
        signatureIndex.setCellValueFactory(new PropertyValueFactory<>("index"));

        signatureIndex.setMinWidth(50.0);
        signatureIndex.setMaxWidth(50.0);

        TableColumn<TableSignatureEntry, String> rValue = new TableColumn<>("R");
        rValue.setCellValueFactory(new PropertyValueFactory<>("rInHex"));
        TableColumn<TableSignatureEntry, String> sValue = new TableColumn<>("S");
        sValue.setCellValueFactory(new PropertyValueFactory<>("sInHex"));
        TableColumn<TableSignatureEntry, String> pubKey = new TableColumn<>("Public Key");
        pubKey.setCellValueFactory(new PropertyValueFactory<>("publicKeyPoint"));

        signatureTableView.getColumns().addAll(
            signatureIndex, rValue, sValue, pubKey
        );
    }

    public Transaction getTransaction() {
        return transaction.get();
    }

    public void setTransaction(Transaction value) {
        transaction.setValue(value);
    }

    public class TableInputEntry extends Input {
        private int index;
        public TableInputEntry(Input input, int index) {
            super(input.getReferencedTransaction(), input.getReferencedOutputIndex());
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public String getReferencedTransactionHash() {
            return ByteUtil.toHexString(this.getReferencedTransaction().getValue(), 32);
        }
    }

    public class TableOutputEntry extends Output {
        private int index;
        public TableOutputEntry(Output output, int index) {
            super(output.getAddress(), output.getAmount());
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public String getAddressHash() {
            return ByteUtil.toHexString(this.getAddress().getValue(), 32);
        }
    }


    public class TableSignatureEntry extends Signature {
        private int index;
        public TableSignatureEntry(Signature signature, int index) {
            super(signature.getR(), signature.getS(), signature.getPublicKey());
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public String getRInHex() {
            return getR().toString(16);
        }

        public String getSInHex() {
            return getS().toString(16);
        }

        public String getPublicKeyPoint() {
            return ByteUtil.toHexString(getPublicKey().toCompressed());
        }
    }
}
