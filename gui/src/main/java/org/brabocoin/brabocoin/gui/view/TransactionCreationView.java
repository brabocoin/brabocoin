package org.brabocoin.brabocoin.gui.view;

import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.util.converter.IntegerStringConverter;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.InsufficientInputException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.DecimalTextField;
import org.brabocoin.brabocoin.gui.converter.Base58StringConverter;
import org.brabocoin.brabocoin.gui.converter.DoubleToCoinStringConverter;
import org.brabocoin.brabocoin.gui.converter.HashStringConverter;
import org.brabocoin.brabocoin.gui.converter.HexBigIntegerStringConverter;
import org.brabocoin.brabocoin.gui.dialog.FeeDialog;
import org.brabocoin.brabocoin.gui.tableentry.EditCell;
import org.brabocoin.brabocoin.gui.tableentry.EditableTableInputEntry;
import org.brabocoin.brabocoin.gui.tableentry.EditableTableOutputEntry;
import org.brabocoin.brabocoin.gui.tableentry.EditableTableSignatureEntry;
import org.brabocoin.brabocoin.gui.tableentry.ValidatedEditCell;
import org.brabocoin.brabocoin.gui.util.WalletUtils;
import org.brabocoin.brabocoin.gui.window.TransactionCreationWindow;
import org.brabocoin.brabocoin.gui.window.ValidationWindow;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.UnsignedTransaction;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.validation.consensus.Consensus;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.brabocoin.brabocoin.wallet.TransactionSigningResult;
import org.brabocoin.brabocoin.wallet.TransactionSigningStatus;
import org.brabocoin.brabocoin.wallet.Wallet;
import tornadofx.SmartResize;
import tornadofx.SmartResizeKt;

import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TransactionCreationView extends VBox implements BraboControl, Initializable {

    private static final Logger LOGGER = Logger.getLogger(TransactionCreationView.class.getName());

    public static final double INDEX_COLUMN_WIDTH = 60.0;
    private final Wallet wallet;
    private final Blockchain blockchain;
    private final Consensus consensus;
    private final TransactionValidator transactionValidator;
    private final NodeEnvironment environment;
    private final TransactionCreationWindow transactionCreationWindow;
    @FXML public TableView<EditableTableInputEntry> inputTableView;
    @FXML public TableView<EditableTableOutputEntry> outputTableView;
    @FXML public TableView<EditableTableSignatureEntry> signatureTableView;
    @FXML public Button buttonAddOutput;
    @FXML public Button buttonAddInput;
    @FXML public Button buttonFindInputs;
    @FXML public Button buttonRemoveOutput;
    @FXML public Button buttonRemoveInput;
    @FXML public Button buttonSignTransaction;
    @FXML public VBox inputOutputVBox;
    @FXML public Button buttonSendTransaction;
    @FXML public Button buttonAddSignature;
    @FXML public Button buttonRemoveSignature;
    @FXML public Button buttonCreateChange;
    @FXML public Button buttonValidate;
    @FXML public Label messageLabel;

    public TransactionCreationView(State state,
                                   TransactionCreationWindow transactionCreationWindow) {
        super();
        this.wallet = state.getWallet();
        this.blockchain = state.getBlockchain();
        this.consensus = state.getConsensus();
        this.transactionValidator = state.getTransactionValidator();
        this.environment = state.getEnvironment();
        this.transactionCreationWindow = transactionCreationWindow;

        // Add custom stylesheet
        this.getStylesheets()
            .add(TransactionCreationView.class.getResource("transaction_creation_view.css")
                .toExternalForm());

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messageLabel.getStyleClass().add("error");
        hideErrorLabel();

        outputTableView.setEditable(true);
        outputTableView.setColumnResizePolicy((f) -> SmartResize.Companion.getPOLICY().call(f));

        inputTableView.setEditable(true);
        inputTableView.setColumnResizePolicy((f) -> SmartResize.Companion.getPOLICY().call(f));

        signatureTableView.setEditable(true);

        TableColumn<EditableTableInputEntry, Integer> inputIndex = new TableColumn<>(
            "Index");
        inputIndex.setCellValueFactory(new PropertyValueFactory<>("index"));
        inputIndex.setEditable(false);
        inputIndex.getStyleClass().add("column-fixed");

        TableColumn<EditableTableInputEntry, Hash> refTxHash =
            new TableColumn<>(
                "Referenced Tx Hash");
        refTxHash.setCellValueFactory(new PropertyValueFactory<>("referencedTransaction"));
        refTxHash.setCellFactory(ValidatedEditCell.forTableColumn(new HashStringConverter()));
        refTxHash.setOnEditCommit(event -> {
            commitEdit(
                event, EditableTableInputEntry::setReferencedTransaction, inputTableView
            );
            updateInfoFromOutputInfo(event.getRowValue(), inputTableView);
        });
        refTxHash.setEditable(true);
        SmartResizeKt.remainingWidth(refTxHash);

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
        inputAddress.setCellFactory(l -> new TextFieldTableCell<>(new Base58StringConverter()));
        inputAddress.setEditable(false);
        inputAddress.getStyleClass().add("column-fixed");

        TableColumn<EditableTableInputEntry, Double> inputAmount = new TableColumn<>(
            "Amount");
        inputAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        inputAmount.setCellFactory(l -> new TextFieldTableCell<>(new DoubleToCoinStringConverter()));
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
        address.setCellFactory(ValidatedEditCell.forTableColumn(new Base58StringConverter()));
        address.setOnEditCommit(event -> commitEdit(
            event, EditableTableOutputEntry::setAddress, outputTableView
        ));
        address.setEditable(true);
        SmartResizeKt.remainingWidth(address);

        TableColumn<EditableTableOutputEntry, Double> amount = new TableColumn<>("Amount");
        amount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amount.setCellFactory(EditCell.forTableColumn(
            new DoubleToCoinStringConverter(),
            DecimalTextField::new
        ));
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
        r.setCellFactory(EditCell.forTableColumn(new HexBigIntegerStringConverter()));
        r.setOnEditCommit(event -> commitEdit(
            event, EditableTableSignatureEntry::setR, signatureTableView
        ));
        r.setEditable(true);

        TableColumn<EditableTableSignatureEntry, BigInteger> s = new TableColumn<>("S value");
        s.setCellValueFactory(new PropertyValueFactory<>("s"));
        s.setCellFactory(EditCell.forTableColumn(new HexBigIntegerStringConverter()));
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
                info = wallet.getCompositeUtxoSet().findUnspentOutputInfo(
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
        final EditableTableOutputEntry entry = new EditableTableOutputEntry(new Output(
            new Hash(ByteString.EMPTY), 0
        ), outputTableView.getItems().size());
        outputTableView.getItems().add(
            entry
        );
        updateIndices(outputTableView);
        outputTableView.getSelectionModel().select(entry);
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
        final EditableTableInputEntry entry = new EditableTableInputEntry(
            new Input(
                new Hash(ByteString.EMPTY),
                0
            ), outputTableView.getItems().size(),
            new Hash(ByteString.EMPTY),
            0.0
        );
        inputTableView.getItems().add(
            entry
        );
        updateIndices(inputTableView);
        inputTableView.getSelectionModel().select(entry);
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
        final EditableTableSignatureEntry entry = EditableTableSignatureEntry.empty(
            signatureTableView.getItems().size());
        signatureTableView.getItems().add(
            entry
        );
        signatureTableView.getSelectionModel().select(entry);
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

        Map<Input, UnspentOutputInfo> inputs;
        try {
            inputs = wallet.findInputs(outputSum, inputSum,
                inputTableView.getItems().stream().map(EditableTableInputEntry::toInput).collect(
                    Collectors.toList()), false
            );
        }
        catch (InsufficientInputException e) {
            setError("Insufficient input to match output");
            return;
        }

        inputs.forEach((input, outputInfo) -> {
            EditableTableInputEntry entry =
                new EditableTableInputEntry(input, inputTableView.getItems().size());
            inputTableView.getItems().add(entry);

            entry.setAddress(outputInfo.getAddress());
            entry.setAmount(outputInfo.getAmount());
        });

        hideErrorLabel();
    }

    @FXML
    private void signTransaction(ActionEvent event) {
        signatureTableView.getItems().clear();


        Task<TransactionSigningResult> signingTask = new Task<TransactionSigningResult>() {
            @Override
            protected TransactionSigningResult call() {
                TransactionSigningResult result = WalletUtils.signTransaction(
                    buildUnsignedTransaction(),
                    wallet
                );

                return result;
            }
        };

        signingTask.setOnSucceeded(e -> {
            TransactionSigningResult result = signingTask.getValue();

            if (result != null && result.getStatus() == TransactionSigningStatus.SIGNED) {
                Transaction signedTransaction = result.getTransaction();

                List<EditableTableSignatureEntry> entries = IntStream.range(
                    0,
                    signedTransaction.getSignatures().size()
                ).mapToObj(i -> new EditableTableSignatureEntry(signedTransaction.getSignatures()
                    .get(i), i)).collect(Collectors.toList());

                Platform.runLater(() -> signatureTableView.getItems().setAll(entries));
            }
        });

        signingTask.run();
    }

    @FXML
    private void sendTransaction(ActionEvent event) {
        if (WalletUtils.sendTransaction(
            buildTransaction(),
            transactionValidator,
            environment,
            wallet
        )) {
            transactionCreationWindow.close();
        }
    }


    @FXML
    private void createChange(ActionEvent event) {
        Optional<Map.Entry<Long, PublicKey>> optionalFee = new FeeDialog(
            wallet, getAmountSum(true), getAmountSum(false)
        ).showAndWait();

        if (!optionalFee.isPresent()) {
            return;
        }

        Map.Entry<Long, PublicKey> feeDialogResult = optionalFee.get();

        long changeValue = getAmountSum(true) - getAmountSum(false) - feeDialogResult.getKey();

        outputTableView.getItems().add(
            new EditableTableOutputEntry(
                new Output(
                    feeDialogResult.getValue().getHash(),
                    changeValue
                ), outputTableView.getItems().size())
        );
    }

    private long getAmountSum(boolean inputs) {
        return inputs ? inputTableView.getItems()
            .stream()
            .mapToDouble(EditableTableInputEntry::getAmount)
            .mapToLong(d -> (long)(d * Consensus.COIN))
            .sum()
            : outputTableView.getItems()
                .stream()
                .mapToDouble(EditableTableOutputEntry::getAmount)
                .mapToLong(d -> (long)(d * Consensus.COIN))
                .sum();
    }

    @FXML
    private void validateTransaction(ActionEvent event) {
        new ValidationWindow(blockchain, buildTransaction(), transactionValidator, consensus, false)
            .showAndWait();
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


    @FXML
    private void copyUnsignedData(ActionEvent event) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();

        try {
            content.putString(
                ByteUtil.toHexString(buildUnsignedTransaction().getRawData()));
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

    private UnsignedTransaction buildUnsignedTransaction() {
        return new UnsignedTransaction(
            inputTableView.getItems().stream().map(EditableTableInputEntry::toInput).collect(
                Collectors.toList()),
            outputTableView.getItems().stream().map(EditableTableOutputEntry::toOutput).collect(
                Collectors.toList())
        );
    }

    private Transaction buildTransaction() {
        return new Transaction(
            inputTableView.getItems().stream().map(EditableTableInputEntry::toInput).collect(
                Collectors.toList()),
            outputTableView.getItems().stream().map(EditableTableOutputEntry::toOutput).collect(
                Collectors.toList()),
            signatureTableView.getItems().stream().map(
                s -> s.toSignature(consensus.getCurve())
            ).collect(Collectors.toList())
        );
    }


    void setError(String message) {
        messageLabel.setVisible(true);
        messageLabel.setText(message);
    }

    void hideErrorLabel() {
        messageLabel.setVisible(false);
    }
}
