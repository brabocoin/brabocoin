package org.brabocoin.brabocoin.gui.view;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.table.AddressTableCell;
import org.brabocoin.brabocoin.gui.control.table.BalanceTableCell;
import org.brabocoin.brabocoin.gui.control.table.BooleanTextTableCell;
import org.brabocoin.brabocoin.gui.dialog.BraboDialog;
import org.brabocoin.brabocoin.gui.dialog.UnlockDialog;
import org.brabocoin.brabocoin.gui.tableentry.TableKeyPairEntry;
import org.brabocoin.brabocoin.gui.util.GUIUtils;
import org.brabocoin.brabocoin.gui.window.TransactionCreationWindow;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.wallet.BalanceListener;
import org.brabocoin.brabocoin.wallet.KeyPairListener;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class WalletView extends TabPane implements BraboControl, Initializable, KeyPairListener,
                                                   BalanceListener {

    public static final int PRIVATE_KEY_RADIX = 16;
    private final State state;

    @FXML public Button buttonCreateTransaction;
    @FXML public TableView<TableKeyPairEntry> keyPairsTableView;
    @FXML public Button buttonCreateKeyPair;
    @FXML public Button buttonSaveWallet;
    @FXML public Label confirmedBalanceLabel;
    @FXML public Label pendingBalanceLabel;

    private ObservableList<TableKeyPairEntry> keyPairObservableList =
        FXCollections.observableArrayList();

    public WalletView(State state) {
        super();
        this.state = state;

        this.state.getWallet().addKeyPairListener(this);
        this.state.getWallet().addBalanceListener(this);

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Make private key copyable
        keyPairsTableView.setRowFactory(param -> {
            final TableRow<TableKeyPairEntry> row = new TableRow<>();

            final ContextMenu rowMenu = new ContextMenu();
            MenuItem copyAddress = new MenuItem("Copy address");
            copyAddress.setOnAction(this::copyAddress);
            MenuItem copyPrivateKey = new MenuItem("Copy private key");
            copyPrivateKey.setOnAction(this::copyPrivateKey);
            rowMenu.getItems().addAll(copyAddress, copyPrivateKey);

            // only display context menu for non-null items:
            row.contextMenuProperty().bind(
                Bindings.when(Bindings.isNotNull(row.itemProperty()))
                    .then(rowMenu)
                    .otherwise((ContextMenu)null));
            return row;
        });

        keyPairsTableView.setEditable(false);

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
        addressColumn.setCellFactory(col -> new AddressTableCell<>(false));

        TableColumn<TableKeyPairEntry, Long> confirmedBalance = new TableColumn<>(
            "Confirmed balance (BRC)");
        confirmedBalance.setCellValueFactory(new PropertyValueFactory<>("confirmedBalance"));
        confirmedBalance.setCellFactory(col -> new BalanceTableCell<>());

        TableColumn<TableKeyPairEntry, Long> pendingBalance = new TableColumn<>(
            "Pending balance (BRC)");
        pendingBalance.setCellValueFactory(new PropertyValueFactory<>("pendingBalance"));
        pendingBalance.setCellFactory(col -> new BalanceTableCell<>());

        keyPairsTableView.getColumns().addAll(
            indexColumn, encryptedColumn, addressColumn, confirmedBalance, pendingBalance
        );

        keyPairObservableList.addListener((ListChangeListener<TableKeyPairEntry>)c ->
            IntStream.range(0, keyPairObservableList.size()).forEach(
                i -> keyPairObservableList.get(i).setIndex(i)
            )
        );

        keyPairsTableView.setItems(keyPairObservableList);

        keyPairObservableList.addAll(
            StreamSupport.stream(state.getWallet().spliterator(), false).map(
                l -> new TableKeyPairEntry(l, state.getWallet())
            ).collect(Collectors.toList())
        );

        updateBalances();
    }

    private void updateBalances() {
        long confirmedBalance = state.getWallet().computeBalance(false);
        long pendingBalance = state.getWallet().computeBalance(true);
        Platform.runLater(() -> {
            confirmedBalanceLabel.textProperty().setValue(
                GUIUtils.formatValue(confirmedBalance, true)
            );

            pendingBalanceLabel.textProperty().setValue(
                GUIUtils.formatValue(pendingBalance, true)
            );
        });

        keyPairObservableList.forEach(TableKeyPairEntry::updateBalances);

        keyPairsTableView.refresh();
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
                }
                catch (CipherException | IOException e) {
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
        keyPairObservableList.add(new TableKeyPairEntry(keyPair, state.getWallet()));
    }

    @Override
    public void onBalanceChanged() {
        updateBalances();
    }

    private void copyString(String data) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();

        try {
            content.putString(data);
            clipboard.setContent(content);
        }
        catch (Exception e) {
            // ignore
        }
    }

    private void copyAddress(ActionEvent event) {
        TableKeyPairEntry entry = keyPairsTableView.getSelectionModel().getSelectedItem();
        if (entry == null) {
            return;
        }

        copyString(entry.getPublicKey().getBase58Address());
    }

    private void copyPrivateKey(ActionEvent event) {
        TableKeyPairEntry entry = keyPairsTableView.getSelectionModel().getSelectedItem();
        if (entry == null) {
            return;
        }

        PrivateKey privateKey = entry.getPrivateKey();
        BigInteger integer = BigInteger.ZERO;
        if (!privateKey.isEncrypted()) {
            try {
                integer = privateKey.getKey().getReference().get();
            }
            catch (DestructionException e) {
                // ignored
            }
        }
        else {
            UnlockDialog<Object> privateKeyUnlockDialog = new UnlockDialog<>(
                false,
                (d) -> {
                    try {
                        privateKey.unlock(d);
                    }
                    catch (CipherException | DestructionException e) {
                        return null;
                    }

                    return new Object();
                }
            );

            privateKeyUnlockDialog.setTitle("Unlock private key");
            privateKeyUnlockDialog.setHeaderText("Enter password to copy private key data"
                + ".");

            Optional<Object> unlockResult = privateKeyUnlockDialog.showAndWait();

            if (!unlockResult.isPresent() || !privateKey.isUnlocked()) {
                return;
            }

            try {
                integer = privateKey.getKey().getReference().get();
            }
            catch (DestructionException e) {
                // ignored
            }
        }

        copyString(integer.toString(PRIVATE_KEY_RADIX));
    }
}
