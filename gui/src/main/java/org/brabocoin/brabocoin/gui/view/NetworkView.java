package org.brabocoin.brabocoin.gui.view;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import io.grpc.MethodDescriptor;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.table.DateTimeTableCell;
import org.brabocoin.brabocoin.gui.control.table.DecimalTableCell;
import org.brabocoin.brabocoin.gui.control.table.MethodDescriptorTableCell;
import org.brabocoin.brabocoin.gui.control.table.PeerTableCell;
import org.brabocoin.brabocoin.node.NetworkMessage;
import org.brabocoin.brabocoin.node.NetworkMessageListener;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.node.state.State;
import org.controlsfx.control.MasterDetailPane;
import tornadofx.SmartResize;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

public class NetworkView extends TabPane implements BraboControl, Initializable,
                                                    NetworkMessageListener {

    private static final double KILO_BYTES = 1000.0;
    private final State state;

    @FXML private MasterDetailPane incomingMasterPane;
    @FXML private MasterDetailPane outgoingMasterPane;

    @FXML private TableView<NetworkMessage> incomingMessagesTable;
    @FXML private TableColumn<NetworkMessage, Peer> incomingPeerColumn;
    @FXML private TableColumn<NetworkMessage, MethodDescriptor> incomingMethod;
    @FXML private TableColumn<NetworkMessage, LocalDateTime> incomingTimeReceived;
    @FXML private TableColumn<NetworkMessage, Double> incomingSizeColumn;

    @FXML private TableView<NetworkMessage> outgoingMessagesTable;
    @FXML private TableColumn<NetworkMessage, Peer> outgoingPeerColumn;
    @FXML private TableColumn<NetworkMessage, MethodDescriptor> outgoingMethod;
    @FXML private TableColumn<NetworkMessage, LocalDateTime> outgoingTimeSent;
    @FXML private TableColumn<NetworkMessage, Double> outgoingSizeColumn;

    private ObservableList<NetworkMessage> incomingMessages = FXCollections.observableArrayList();
    private ObservableList<NetworkMessage> outgoingMessages = FXCollections.observableArrayList();
    private NetworkMessageDetailView incomingMessageDetailView;
    private NetworkMessageDetailView outgoingMessageDetailView;

    public NetworkView(State state) {
        super();
        this.state = state;

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadTable();

        state.getEnvironment().addNetworkMessageListener(this);
        incomingMessages.setAll(Lists.newArrayList(state.getEnvironment().getReceivedMessages()));
        outgoingMessages.setAll(Lists.newArrayList(state.getEnvironment().getSentMessages()));

        incomingMessageDetailView = new NetworkMessageDetailView();
        incomingMasterPane.setDetailNode(incomingMessageDetailView);

        outgoingMessageDetailView = new NetworkMessageDetailView();
        outgoingMasterPane.setDetailNode(outgoingMessageDetailView);
    }

    private static ObservableValue<LocalDateTime> readOnlyTimeFeature(
        TableColumn.CellDataFeatures<NetworkMessage, LocalDateTime> f) {
        return new ReadOnlyObjectWrapper<>(f.getValue().getRequestMessages().get(0).getTime());
    }

    private static ObservableValue<Peer> readOnlyPeerFeature(
        TableColumn.CellDataFeatures<NetworkMessage, Peer> f) {
        return new ReadOnlyObjectWrapper<>(f.getValue().getPeer());
    }

    private static ObservableValue<MethodDescriptor> readOnlyMethodFeature(
        TableColumn.CellDataFeatures<NetworkMessage, MethodDescriptor> f) {
        return new ReadOnlyObjectWrapper<>(f.getValue().getMethodDescriptor());
    }

    private static ObservableValue<Double> readOnlyMessageSizeFeature(
        TableColumn.CellDataFeatures<NetworkMessage, Double> f) {
        return new ReadOnlyObjectWrapper<>(
            StreamSupport.stream(
                Spliterators
                    .spliteratorUnknownSize(Iterators.concat(
                        f.getValue().getRequestMessages().iterator(),
                        f.getValue().getResponseMessages().iterator()
                    ), 0),
                false
            ).mapToLong(l -> l.getMessage().getSerializedSize()).sum() / KILO_BYTES
        );
    }

    private void loadTable() {
        incomingMessagesTable.setItems(incomingMessages);
        outgoingMessagesTable.setItems(outgoingMessages);

        incomingMessagesTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    incomingMessageDetailView.setNetworkMessage(newValue);
                }
                incomingMasterPane.setShowDetailNode(true);
            });
        outgoingMessagesTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    outgoingMessageDetailView.setNetworkMessage(newValue);
                }
                outgoingMasterPane.setShowDetailNode(true);
            });

        incomingMessagesTable.setColumnResizePolicy((f) -> SmartResize.Companion.getPOLICY()
            .call(f));
        outgoingMessagesTable.setColumnResizePolicy((f) -> SmartResize.Companion.getPOLICY()
            .call(f));

        incomingPeerColumn.setCellValueFactory(NetworkView::readOnlyPeerFeature);
        incomingPeerColumn.setCellFactory(col -> new PeerTableCell<>());
        outgoingPeerColumn.setCellValueFactory(NetworkView::readOnlyPeerFeature);
        outgoingPeerColumn.setCellFactory(col -> new PeerTableCell<>());

        incomingMethod.setCellValueFactory(NetworkView::readOnlyMethodFeature);
        incomingMethod.setCellFactory(col -> new MethodDescriptorTableCell<>());
        outgoingMethod.setCellValueFactory(NetworkView::readOnlyMethodFeature);
        outgoingMethod.setCellFactory(col -> new MethodDescriptorTableCell<>());

        incomingTimeReceived.setCellValueFactory(NetworkView::readOnlyTimeFeature);
        incomingTimeReceived.setCellFactory(col -> new DateTimeTableCell<>());
        outgoingTimeSent.setCellValueFactory(NetworkView::readOnlyTimeFeature);
        outgoingTimeSent.setCellFactory(col -> new DateTimeTableCell<>());

        incomingSizeColumn.setCellValueFactory(NetworkView::readOnlyMessageSizeFeature);
        incomingSizeColumn.setCellFactory(col -> new DecimalTableCell<>(new DecimalFormat("0.00")));
        outgoingSizeColumn.setCellValueFactory(NetworkView::readOnlyMessageSizeFeature);
        outgoingSizeColumn.setCellFactory(col -> new DecimalTableCell<>(new DecimalFormat("0.00")));
    }

    @Override
    public void onIncomingMessage(NetworkMessage message, boolean isUpdate) {
        Platform.runLater(() -> incomingMessages.setAll(Lists.newArrayList(state.getEnvironment()
            .getReceivedMessages())));

        incomingMessagesTable.refresh();
    }

    @Override
    public void onOutgoingMessage(NetworkMessage message, boolean isUpdate) {
        Platform.runLater(() -> outgoingMessages.setAll(Lists.newArrayList(state.getEnvironment()
            .getSentMessages())));

        outgoingMessagesTable.refresh();
    }
}
