package org.brabocoin.brabocoin.gui.view;

import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.converter.BigIntegerStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LongStringConverter;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.BrabocoinGUI;
import org.brabocoin.brabocoin.gui.control.KeyDropDown;
import org.brabocoin.brabocoin.gui.control.NumberTextField;
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
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.wallet.Wallet;

import java.math.BigInteger;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class TransactionCreationView extends VBox implements BraboControl, Initializable {

    public static final double INDEX_COLUMN_WIDTH = 60.0;
    private final Wallet wallet;
    private final Blockchain blockchain;
    private final Consensus consensus;
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
    @FXML public Button buttonCreateChange;

    public TransactionCreationView(Wallet wallet, Blockchain blockchain, Consensus consensus) {
        super();
        this.wallet = wallet;
        this.blockchain = blockchain;
        this.consensus = consensus;

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

        TableColumn<EditableTableInputEntry, Hash> refTxHash =
            new TableColumn<>(
                "Referenced Tx Hash");
        refTxHash.setCellValueFactory(new PropertyValueFactory<>("referencedTransaction"));
        refTxHash.setCellFactory(EditCell.forTableColumn(new HashStringConverter()));
        refTxHash.setOnEditCommit(event -> {
            commitEdit(
                event, EditableTableInputEntry::setReferencedTransaction, inputTableView
            );
            updateInfoFromOutputInfo(event.getRowValue(), inputTableView);
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
            updateInfoFromOutputInfo(event.getRowValue(), outputTableView);
        });
        refOutputIndex.setEditable(true);

        TableColumn<EditableTableInputEntry, Hash> inputAddress = new TableColumn<>(
            "Address");
        inputAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        inputAddress.setCellFactory(l -> new TextFieldTableCell<>(new HashStringConverter()));
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

        TableColumn<EditableTableOutputEntry, Hash> address = new TableColumn<>("Address");
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

        TableColumn<EditableTableSignatureEntry, Integer> signatureIndex = new TableColumn<>(
            "Index");
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

    private void updateInfoFromOutputInfo(EditableTableInputEntry entry, TableView table) {
        long amount = 0;
        Hash address = new Hash(ByteString.EMPTY);
        if (entry.getReferencedTransaction() != null && !entry.getReferencedTransaction()
            .getValue()
            .equals(ByteString.EMPTY)) {
            UnspentOutputInfo info = null;
            try {
                info = wallet.getUtxoSet().findUnspentOutputInfo(
                    entry.getReferencedTransaction(), entry.getReferencedOutputIndex()
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

        entry.setAmount(amount);
        entry.setAddress(address);
        table.refresh();
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
                new Hash(ByteString.EMPTY), 0
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
                    new Hash(ByteString.EMPTY),
                    0
                ), outputTableView.getItems().size(),
                new Hash(ByteString.EMPTY),
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
        long outputSum = getAmountSum(false);
        long inputSum = getAmountSum(true);
        for (Map.Entry<Input, UnspentOutputInfo> info : wallet.getUtxoSet()) {
            if (inputSum >= outputSum) {
                break;
            }

            if (info.getValue().isCoinbase() && blockchain.getMainChain()
                .getHeight() - consensus.getCoinbaseMaturityDepth() < info.getValue()
                .getBlockHeight()) {
                continue;
            }

            inputSum += info.getValue().getAmount();
            EditableTableInputEntry entry = new EditableTableInputEntry(
                info.getKey(),
                inputTableView.getItems().size()
            );
            inputTableView.getItems().add(entry);

            updateInfoFromOutputInfo(entry, inputTableView);
        }
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
    private void createChange(ActionEvent event) {
        Dialog<Long> feeDialog = new Dialog<>();
        feeDialog.setHeaderText("Enter transaction fee");
        feeDialog.setTitle("Transaction fee");


        ButtonType okButton = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
        feeDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        NumberTextField feeField = new NumberTextField();
        KeyDropDown dropDown = new KeyDropDown(wallet);

        grid.add(new Label("Fee:"), 0, 0);
        grid.add(feeField, 1, 0);
        grid.add(new Label("Output address:"), 0, 1);
        grid.add(dropDown, 1, 1);

        feeDialog.getDialogPane().setContent(grid);

        feeDialog.getDialogPane()
            .getStylesheets()
            .add(BrabocoinGUI.class.getResource("brabocoin.css").toExternalForm());

        feeDialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                return Long.parseLong(feeField.getText());
            }
            return null;
        });

        Optional<Long> optionalFee = feeDialog.showAndWait();
        Platform.runLater(feeField::requestFocus);

        if (!optionalFee.isPresent()) {
            return;
        }

        long fee = optionalFee.get();

        long changeValue = getAmountSum(true) - getAmountSum(false) - fee;
        if (changeValue < 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Insufficient input");
            alert.setHeaderText("Change creation failure");
            alert.setContentText(
                "Change could not be created, as you do not have sufficient inputs.");

            alert.showAndWait();
            return;
        }

        outputTableView.getItems().add(
            new EditableTableOutputEntry(
                new Output(
                    dropDown.getSelectionModel().getSelectedItem().getHash(),
                    changeValue
                ), outputTableView.getItems().size())
        );
    }

    private long getAmountSum(boolean inputs) {
        return inputs ? inputTableView.getItems()
            .stream()
            .mapToLong(EditableTableInputEntry::getAmount)
            .sum()
            : outputTableView.getItems()
                .stream()
                .mapToLong(EditableTableOutputEntry::getAmount)
                .sum();
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
        }
        catch (Exception e) {
            // ignore
        }
    }

    private <S> void updateIndices(TableView<S> table) {
        for (int i = 0; i < table.getItems().size(); i++) {
            S item = table.getItems().get(i);
            if (item instanceof EditableTableInputEntry) {
                ((EditableTableInputEntry)item).setIndex(i);
            }
            else if (item instanceof EditableTableOutputEntry) {
                ((EditableTableOutputEntry)item).setIndex(i);
            }
            else if (item instanceof EditableTableSignatureEntry) {
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
