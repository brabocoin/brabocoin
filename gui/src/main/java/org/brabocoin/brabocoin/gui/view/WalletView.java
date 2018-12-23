package org.brabocoin.brabocoin.gui.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.converter.PublicKeyStringConverter;
import org.brabocoin.brabocoin.gui.tableentry.TableKeyPairEntry;
import org.brabocoin.brabocoin.gui.window.TransactionCreationWindow;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.wallet.KeyPairListener;
import tornadofx.ItemControlsKt;
import tornadofx.SmartResize;

import java.net.URL;
import java.util.ResourceBundle;

public class WalletView extends TabPane implements BraboControl, Initializable, KeyPairListener {

    private final State state;

    @FXML public Button buttonCreateTransaction;
    @FXML public TableView<TableKeyPairEntry> keyPairsTableView;


    private ObservableList<TableKeyPairEntry> keyPairObservableList = FXCollections.observableArrayList();

    public WalletView(State state) {
        super();
        this.state = state;

        this.state.getWallet().addKeyPairListener(this);

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        keyPairsTableView.setEditable(false);
        keyPairsTableView.setColumnResizePolicy((f) -> SmartResize.Companion.getPOLICY().call(f));

        TableColumn<TableKeyPairEntry, Integer> indexColumn = new TableColumn<>(
            "Index");
        indexColumn.setCellValueFactory(new PropertyValueFactory<>("index"));
        indexColumn.getStyleClass().add("column-fixed");

        TableColumn<TableKeyPairEntry, Boolean> encryptedColumn = new TableColumn<>(
            "Encrypted");
        encryptedColumn.setCellValueFactory(new PropertyValueFactory<>("encrypted"));
        ItemControlsKt.useCheckbox(encryptedColumn, false);

        TableColumn<TableKeyPairEntry, PublicKey> publicKeyColumn = new TableColumn<>(
            "Public Key");
        publicKeyColumn.setCellValueFactory(new PropertyValueFactory<>("publicKey"));
        publicKeyColumn.setCellFactory(l -> new TextFieldTableCell<>(new PublicKeyStringConverter()));

        keyPairsTableView.getColumns().addAll(
            indexColumn, encryptedColumn, publicKeyColumn
        );

        keyPairObservableList.addListener();

        keyPairsTableView.setItems(keyPairObservableList);
    }

    @FXML
    private void createTransaction(ActionEvent event) {
        new TransactionCreationWindow(state).showAndWait();
    }

    @Override
    public void onKeyPairGenerated(KeyPair keyPair) {

    }
}
