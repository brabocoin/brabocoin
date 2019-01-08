package org.brabocoin.brabocoin.gui.view;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.BraboDialog;
import org.brabocoin.brabocoin.gui.control.table.AddressTableCell;
import org.brabocoin.brabocoin.gui.control.table.BooleanTextTableCell;
import org.brabocoin.brabocoin.gui.dialog.UnlockDialog;
import org.brabocoin.brabocoin.gui.tableentry.TableKeyPairEntry;
import org.brabocoin.brabocoin.gui.view.wallet.TransactionHistoryView;
import org.brabocoin.brabocoin.gui.window.TransactionCreationWindow;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.wallet.KeyPairListener;
import tornadofx.SmartResize;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class WalletView extends TabPane implements BraboControl, Initializable, KeyPairListener {

    private final State state;

    @FXML private Tab txHistoryTab;
    @FXML public Button buttonCreateTransaction;
    @FXML public TableView<TableKeyPairEntry> keyPairsTableView;
    @FXML public Button buttonCreateKeyPair;
    @FXML public Button buttonSaveWallet;
    @FXML public Label balanceLabel;
    @FXML public Label totalPendingBalanceLabel;
    @FXML public Label pendingLabel;

    private ObservableList<TableKeyPairEntry> keyPairObservableList =
        FXCollections.observableArrayList();

    public WalletView(State state) {
        super();
        this.state = state;

        this.state.getWallet().addKeyPairListener(this);

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txHistoryTab.setContent(new TransactionHistoryView(state.getWallet().getTransactionHistory()));
        keyPairsTableView.setEditable(false);
        keyPairsTableView.setColumnResizePolicy((f) -> SmartResize.Companion.getPOLICY().call(f));

        TableColumn<TableKeyPairEntry, Integer> indexColumn = new TableColumn<>(
            "Index");
        indexColumn.setCellValueFactory(new PropertyValueFactory<>("index"));
        indexColumn.getStyleClass().add("column-fixed");

        TableColumn<TableKeyPairEntry, Boolean> encryptedColumn = new TableColumn<>(
            "Encrypted");
        encryptedColumn.setCellValueFactory(new PropertyValueFactory<>("encrypted"));
        encryptedColumn.setCellFactory(col -> new BooleanTextTableCell<>("Yes", "No"));


        TableColumn<TableKeyPairEntry, Hash> addressColumn = new TableColumn<>(
            "Address");
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        addressColumn.setCellFactory(col -> new AddressTableCell<>());

        keyPairsTableView.getColumns().addAll(
            indexColumn, encryptedColumn, addressColumn
        );

        keyPairObservableList.addListener((ListChangeListener<TableKeyPairEntry>)c ->
            IntStream.range(0, keyPairObservableList.size()).forEach(
                i -> keyPairObservableList.get(i).setIndex(i)
            )
        );

        keyPairsTableView.setItems(keyPairObservableList);

        keyPairObservableList.addAll(
            StreamSupport.stream(state.getWallet().spliterator(), false).map(
                TableKeyPairEntry::new
            ).collect(Collectors.toList())
        );
    }

    @FXML
    private void createTransaction(ActionEvent event) {
        new TransactionCreationWindow(state).showAndWait();
    }

    @FXML
    private void createKeyPair(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        BraboDialog.setBraboStyling(alert.getDialogPane());
        alert.setTitle("Key pair generation");
        alert.setHeaderText("Choose key pair type");
        alert.setContentText(
            "Do you want to create a plain key pair or encrypt it with a new password?");

        ButtonType buttonPlain = new ButtonType("Plain");
        ButtonType buttonEncrypted = new ButtonType("Encrypted");
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonPlain, buttonEncrypted, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();

        if (!result.isPresent()) {
            return;
        }

        if (result.get() == buttonPlain) {
            try {
                state.getWallet().generatePlainKeyPair();
            }
            catch (DestructionException e) {
                throw new RuntimeException("Could not destruct generated random number.");
            }
        }
        else if (result.get() == buttonEncrypted) {
            UnlockDialog<Object> passwordDialog = new UnlockDialog<>(
                true,
                (d) -> {
                    try {
                        state.getWallet().generateEncryptedKeyPair(d);
                        d.destruct();
                    }
                    catch (DestructionException | CipherException e) {
                        return null;
                    }
                    return new Object();
                },
                "Ok"
            );

            passwordDialog.setTitle("Key pair password");
            passwordDialog.setHeaderText("Enter a password to encrypt the private key");

            passwordDialog.showAndWait();
        }
    }


    @FXML
    private void saveWallet(ActionEvent event) {
        UnlockDialog<Object> passwordDialog = new UnlockDialog<>(
            false,
            (d) -> {
                try {
                    state.getWalletIO().getCipher().decyrpt(
                        Files.readAllBytes(state.getWalletFile().toPath()),
                        d.getReference().get()
                    );
                } catch (CipherException | IOException e) {
                    try {
                        d.destruct();
                    }
                    catch (DestructionException e1) {
                        // ignore
                    }
                    return null;
                }

                try {
                    state.getWalletIO().write(
                        state.getWallet(),
                        state.getWalletFile(),
                        state.getTxHistoryFile(),
                        state.getUsedInputsFile(),
                        d
                    );
                    d.destruct();
                }
                catch (IOException | DestructionException | CipherException e) {
                    return null;
                }
                return new Object();
            },
            "Ok"
        );

        passwordDialog.setTitle("Wallet password");
        passwordDialog.setHeaderText("Enter a password to encrypt your wallet");

        passwordDialog.showAndWait();
    }

    @Override
    public void onKeyPairGenerated(KeyPair keyPair) {
        keyPairObservableList.add(new TableKeyPairEntry(keyPair));
    }
}
