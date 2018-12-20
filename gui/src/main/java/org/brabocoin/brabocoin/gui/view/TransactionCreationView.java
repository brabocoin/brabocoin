package org.brabocoin.brabocoin.gui.view;

import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.converter.BigIntegerStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LongStringConverter;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.converter.Base58StringConverter;
import org.brabocoin.brabocoin.gui.converter.HashStringConverter;
import org.brabocoin.brabocoin.gui.tableentry.EditCell;
import org.brabocoin.brabocoin.gui.tableentry.EditableTableInputEntry;
import org.brabocoin.brabocoin.gui.tableentry.EditableTableOutputEntry;
import org.brabocoin.brabocoin.gui.tableentry.EditableTableSignatureEntry;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.wallet.Wallet;

import java.math.BigInteger;
import java.net.URL;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class TransactionCreationView extends VBox implements BraboControl, Initializable {

    public static final double INDEX_COLUMN_WIDTH = 60.0;
    private final Wallet wallet;
    @FXML public TableView<EditableTableInputEntry> inputTableView;
    @FXML public TableView<EditableTableOutputEntry> outputTableView;
    @FXML public Button buttonAddOutput;
    @FXML public Button buttonAddInput;
    @FXML public Button buttonFindInputs;
    @FXML public Button buttonRemoveOutput;
    @FXML public Button buttonRemoveInput;
    @FXML public Button buttonSignTransaction;
    @FXML public TableView signatureTableView;
    @FXML public VBox inputOutputVBox;
    @FXML public Button buttonSendTransaction;
    @FXML public Button buttonCopyJSON;
    @FXML public Button buttonAddSignature;
    @FXML public Button buttonRemoveSignature;

    public TransactionCreationView(Wallet wallet) {
        super();
        this.wallet = wallet;

        // Add custom stylesheet
        this.getStylesheets()
            .add(TransactionCreationView.class.getResource("transaction_creation_view.css")
                .toExternalForm());

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        outputTableView.setEditable(true);
        inputTableView.setEditable(true);
        signatureTableView.setEditable(true);

        TableColumn<EditableTableInputEntry, Integer> inputIndex = new TableColumn<>(
            "Index");
        inputIndex.setCellValueFactory(new PropertyValueFactory<>("index"));
        inputIndex.setEditable(false);
        inputIndex.getStyleClass().add("column-fixed");

        inputIndex.setMinWidth(INDEX_COLUMN_WIDTH);
        inputIndex.setMaxWidth(INDEX_COLUMN_WIDTH);

        TableColumn<EditableTableInputEntry, org.brabocoin.brabocoin.model.Hash> refTxHash = new TableColumn<>(
            "Referenced Tx Hash");
        refTxHash.setCellValueFactory(new PropertyValueFactory<>("referencedTransaction"));
        refTxHash.setCellFactory(EditCell.forTableColumn(new HashStringConverter()));
        refTxHash.setOnEditCommit(event -> {
            commitEdit(
                event, EditableTableInputEntry::setReferencedTransaction, inputTableView
            );
            updateInfoFromOutputInfo(event);
        });
        refTxHash.setEditable(true);

        TableColumn<EditableTableInputEntry, Integer> refOutputIndex = new TableColumn<>(
            "Output Index");
        refOutputIndex.setCellValueFactory(new PropertyValueFactory<>("referencedOutputIndex"));
        refOutputIndex.setCellFactory(EditCell.forTableColumn(new IntegerStringConverter()));
        refOutputIndex.setOnEditCommit(event -> {
            commitEdit(
                event, EditableTableInputEntry::setReferencedOutputIndex, outputTableView
            );
            updateInfoFromOutputInfo(event);
        });
        refOutputIndex.setEditable(true);

        TableColumn<EditableTableInputEntry, org.brabocoin.brabocoin.model.Hash> inputAddress = new TableColumn<>(
            "Address");
        inputAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        inputAddress.setCellFactory(l -> new TextFieldTableCell<>(new Base58StringConverter()));
        inputAddress.setEditable(false);
        inputAddress.getStyleClass().add("column-fixed");

        TableColumn<EditableTableInputEntry, Long> inputAmount = new TableColumn<>(
            "Amount");
        inputAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        inputAmount.setCellFactory(l -> new TextFieldTableCell<>(new LongStringConverter()));
        inputAmount.setEditable(false);
        inputAmount.getStyleClass().add("column-fixed");


        inputTableView.getColumns().addAll(
            inputIndex, refTxHash, refOutputIndex, inputAddress, inputAmount
        );

        TableColumn<EditableTableOutputEntry, Integer> outputIndex = new TableColumn<>("Index");
        outputIndex.setCellValueFactory(new PropertyValueFactory<>("index"));
        outputIndex.setEditable(false);
        outputIndex.getStyleClass().add("column-fixed");

        outputIndex.setMinWidth(INDEX_COLUMN_WIDTH);
        outputIndex.setMaxWidth(INDEX_COLUMN_WIDTH);

        TableColumn<EditableTableOutputEntry, org.brabocoin.brabocoin.model.Hash> address = new TableColumn<>("Address");
        address.setCellValueFactory(new PropertyValueFactory<>("address"));
        address.setCellFactory(EditCell.forTableColumn(new Base58StringConverter()));
        address.setOnEditCommit(event -> commitEdit(
            event, EditableTableOutputEntry::setAddress, outputTableView
        ));
        address.setEditable(true);

        TableColumn<EditableTableOutputEntry, Long> amount = new TableColumn<>("Amount");
        amount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amount.setCellFactory(EditCell.forTableColumn(new LongStringConverter()));
        amount.setOnEditCommit(event -> commitEdit(
            event, EditableTableOutputEntry::setAmount, outputTableView
        ));
        amount.setEditable(true);

        outputTableView.getColumns().addAll(
            outputIndex, address, amount
        );

        TableColumn<EditableTableSignatureEntry, Integer> signatureIndex = new TableColumn<>("Index");
        signatureIndex.setCellValueFactory(new PropertyValueFactory<>("index"));
        signatureIndex.setEditable(false);
        signatureIndex.getStyleClass().add("column-fixed");

        signatureIndex.setMinWidth(INDEX_COLUMN_WIDTH);
        signatureIndex.setMaxWidth(INDEX_COLUMN_WIDTH);

        TableColumn<EditableTableSignatureEntry, BigInteger> r = new TableColumn<>("R value");
        r.setCellValueFactory(new PropertyValueFactory<>("r"));
        r.setCellFactory(EditCell.forTableColumn(new BigIntegerStringConverter()));
        r.setOnEditCommit(event -> commitEdit(
            event, EditableTableSignatureEntry::setR, signatureTableView
        ));
        r.setEditable(true);

        TableColumn<EditableTableSignatureEntry, BigInteger> s = new TableColumn<>("S value");
        s.setCellValueFactory(new PropertyValueFactory<>("s"));
        s.setCellFactory(EditCell.forTableColumn(new BigIntegerStringConverter()));
        s.setOnEditCommit(event -> commitEdit(
            event, EditableTableSignatureEntry::setS, signatureTableView
        ));
        s.setEditable(true);

        TableColumn<EditableTableSignatureEntry, Hash> publicKey = new TableColumn<>("Public key");
        publicKey.setCellValueFactory(new PropertyValueFactory<>("publicKey"));
        publicKey.setCellFactory(EditCell.forTableColumn(new HashStringConverter()));
        publicKey.setOnEditCommit(event -> commitEdit(
            event, EditableTableSignatureEntry::setPublicKey, signatureTableView
        ));
        publicKey.setEditable(true);

        signatureTableView.getColumns().addAll(
            signatureIndex, r, s, publicKey
        );
    }

    private <T> void updateInfoFromOutputInfo(
        TableColumn.CellEditEvent<org.brabocoin.brabocoin.gui.tableentry.EditableTableInputEntry,
            T> event) {
        EditableTableInputEntry value = event.getRowValue();

        long amount = 0;
        org.brabocoin.brabocoin.model.Hash address = new org.brabocoin.brabocoin.model.Hash(ByteString.EMPTY);
        if (value.getReferencedTransaction() != null && !value.getReferencedTransaction().getValue().equals(ByteString.EMPTY)) {
            UnspentOutputInfo info = null;
            try {
                info = wallet.getUtxoSet().findUnspentOutputInfo(
                    value.getReferencedTransaction(), value.getReferencedOutputIndex()
                );
            }
            catch (DatabaseException e) {
                // ignored
            }

            if (info != null) {
                address = info.getAddress();
                amount = info.getAmount();
            }
        }

        value.setAmount(amount);
        value.setAddress(address);
        event.getTableView().refresh();
    }

    private <S, T> void commitEdit(TableColumn.CellEditEvent<S, T> event,
                                   BiConsumer<S, T> setter, TableView table) {
        final S model = event.getRowValue();

        setter.accept(model, event.getNewValue());
        table.refresh();
    }

    @FXML
    private void addOutput(ActionEvent event) {
        outputTableView.getItems().add(
            new EditableTableOutputEntry(new Output(
                new org.brabocoin.brabocoin.model.Hash(ByteString.EMPTY), 0
            ), outputTableView.getItems().size())
        );
        updateIndices(outputTableView);
    }

    @FXML
    private void removeOutput(ActionEvent event) {
        outputTableView.getItems().remove(
            outputTableView.getSelectionModel().getSelectedItem()
        );
        updateIndices(outputTableView);
    }

    @FXML
    private void addInput(ActionEvent event) {
        inputTableView.getItems().add(
            new EditableTableInputEntry(
                new Input(
                    new org.brabocoin.brabocoin.model.Hash(ByteString.EMPTY),
                    0
                ), outputTableView.getItems().size(),
                new org.brabocoin.brabocoin.model.Hash(ByteString.EMPTY),
                0L
            )
        );
        updateIndices(inputTableView);
    }

    @FXML
    private void removeInput(ActionEvent event) {
        inputTableView.getItems().remove(
            inputTableView.getSelectionModel().getSelectedItem()
        );
        updateIndices(inputTableView);
    }


    @FXML
    private void addSignature(ActionEvent event) {
        signatureTableView.getItems().add(
            EditableTableSignatureEntry.empty(signatureTableView.getItems().size())
        );
    }

    @FXML
    private void removeSignature(ActionEvent event) {
        signatureTableView.getItems().remove(
            signatureTableView.getSelectionModel().getSelectedItem()
        );
        updateIndices(signatureTableView);
    }

    @FXML
    private void findInputs(ActionEvent event) {

    }

    @FXML
    private void signTransaction(ActionEvent event) {
        FadeTransition fade = new FadeTransition();
        fade.setDuration(Duration.millis(100));
        fade.setFromValue(1);
        fade.setToValue(0.1);
        fade.setCycleCount(2);
        fade.setAutoReverse(true);
        fade.setNode(inputOutputVBox);
        fade.play();


    }

    @FXML
    private void sendTransaction(ActionEvent event) {

    }


    @FXML
    private void copyJSON(ActionEvent event) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();

        try {
            BrabocoinProtos.Transaction protoTransaction = ProtoConverter.toProto(
                buildTransaction(),
                BrabocoinProtos.Transaction.class
            );

            content.putString(JsonFormat.printer().print(protoTransaction));
            clipboard.setContent(content);
        } catch (Exception e) {
            // ignore
        }
    }

    private <S> void updateIndices(TableView<S> table) {
        for (int i = 0; i < table.getItems().size(); i++) {
            S item = table.getItems().get(i);
            if (item instanceof EditableTableInputEntry) {
                ((EditableTableInputEntry)item).setIndex(i);
            } else if (item instanceof EditableTableOutputEntry) {
                ((EditableTableOutputEntry)item).setIndex(i);
            } else if (item instanceof EditableTableSignatureEntry) {
                ((EditableTableSignatureEntry)item).setIndex(i);
            }
        }
    }

    private Transaction buildTransaction() {
        return new Transaction(
            inputTableView.getItems().stream().map(EditableTableInputEntry::toInput).collect(
                Collectors.toList()),
            outputTableView.getItems().stream().map(EditableTableOutputEntry::toOutput).collect(
                Collectors.toList()),
            Collections.emptyList()
        );
    }
}
