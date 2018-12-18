package org.brabocoin.brabocoin.gui.view;

import com.google.protobuf.ByteString;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.converter.LongStringConverter;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.converter.HashStringConverter;
import org.brabocoin.brabocoin.gui.tableentry.EditCell;
import org.brabocoin.brabocoin.gui.tableentry.EditableTableOutputEntry;
import org.brabocoin.brabocoin.gui.tableentry.TableInputEntry;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Output;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

public class TransactionCreationView extends VBox implements BraboControl, Initializable {

    @FXML public TableView<TableInputEntry> inputTableView;
    @FXML public TableView<EditableTableOutputEntry> outputTableView;
    @FXML public Button buttonAddOutput;

    public TransactionCreationView() {
        super();

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        outputTableView.setEditable(true);

        outputTableView.getSelectionModel().cellSelectionEnabledProperty().set(true);

        TableColumn<TableInputEntry, Integer> inputIndex = new TableColumn<>(
            "Index");
        inputIndex.setCellValueFactory(new PropertyValueFactory<>("index"));

        TableColumn<TableInputEntry, String> refTxHash = new TableColumn<>(
            "Referenced Tx Hash");
        refTxHash.setCellValueFactory(new PropertyValueFactory<>("referencedTransactionHash"));
        TableColumn<TableInputEntry, Integer> refOutputIndex =
            new TableColumn<>(
                "Output Index");
        refOutputIndex.setCellValueFactory(new PropertyValueFactory<>("referencedOutputIndex"));

        inputTableView.getColumns().addAll(
            inputIndex, refTxHash, refOutputIndex
        );

        TableColumn<EditableTableOutputEntry, Integer> outputIndex = new TableColumn<>("Index");
        outputIndex.setCellValueFactory(new PropertyValueFactory<>("index"));
        outputIndex.setEditable(false);

        TableColumn<EditableTableOutputEntry, Hash> address = new TableColumn<>("Address");
        address.setCellValueFactory(new PropertyValueFactory<>("address"));
        address.setCellFactory(EditCell.forTableColumn(new HashStringConverter()));
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
    }

    public static <S, T> void commitEdit(TableColumn.CellEditEvent<S, T> event,
                                     BiConsumer<S, T> setter, TableView table) {
        final T value;
        final S model = event.getRowValue();

        if (event.getNewValue() == null) {
            value = event.getOldValue();
        }
        else {
            value = event.getNewValue();
        }

        setter.accept(model, value);
        table.refresh();
    }

    @FXML
    private void addOutput(ActionEvent event) {
        outputTableView.getItems().add(
            new EditableTableOutputEntry(new Output(
                new Hash(ByteString.EMPTY), 0
            ), outputTableView.getItems().size())
        );
    }
}
