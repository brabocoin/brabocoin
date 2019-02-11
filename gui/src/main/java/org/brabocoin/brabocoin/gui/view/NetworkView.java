package org.brabocoin.brabocoin.gui.view;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import io.grpc.MethodDescriptor;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.SelectableLabel;
import org.brabocoin.brabocoin.gui.control.table.DateTimeTableCell;
import org.brabocoin.brabocoin.gui.control.table.DecimalTableCell;
import org.brabocoin.brabocoin.gui.control.table.IntegerTableCell;
import org.brabocoin.brabocoin.gui.control.table.MethodDescriptorTableCell;
import org.brabocoin.brabocoin.gui.control.table.PeerTableCell;
import org.brabocoin.brabocoin.gui.control.table.StringTableCell;
import org.brabocoin.brabocoin.gui.dialog.PeerCreationDialog;
import org.brabocoin.brabocoin.gui.glyph.BraboGlyph;
import org.brabocoin.brabocoin.gui.task.TaskManager;
import org.brabocoin.brabocoin.listeners.PeerSetChangedListener;
import org.brabocoin.brabocoin.node.NetworkMessage;
import org.brabocoin.brabocoin.node.NetworkMessageListener;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.util.IpData;
import org.brabocoin.brabocoin.util.NetworkUtil;
import org.controlsfx.control.MasterDetailPane;
import tornadofx.SmartResize;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

public class NetworkView extends TabPane implements BraboControl, Initializable,
                                                    NetworkMessageListener, PeerSetChangedListener {

    private static final double KILO_BYTES = 1000.0;
    private static final int CELL_SIZE = 25;
    private static final double IP_TABLE_INITIAL_HEIGHT = 100.0;
    private static final double IP_TABLE_HEIGHT_OFFSET = 1.50;
    public static final int REFRESH_ICON_SIZE = 14;
    public static final double SERVICE_INFO_HBOX_SPACING = 10.0;
    private final State state;
    private final TaskManager taskManager;
    private SimpleStringProperty externalIp = new SimpleStringProperty();
    private Task<String> externalIpTask;
    private Task<List<IpData>> ipDataTask;
    @FXML private SelectableLabel externalIpLabel;
    @FXML private TitledPane serviceInfoTitledPane;

    @FXML private TableView<Peer> peerTable;
    @FXML private TableColumn<Peer, String> peerIPColumn;
    @FXML private TableColumn<Peer, String> peerHostnameColumn;
    @FXML private TableColumn<Peer, Integer> peerPortColumn;
    @FXML private TableColumn<Peer, Integer> peerIncomingMessageCountColumn;
    @FXML private TableColumn<Peer, Integer> peerOutgoingMessageCountColumn;

    @FXML private SelectableLabel portLabel;
    @FXML private TableView<IpData> ipTable;
    @FXML private TableColumn<IpData, String> ipNameColumn;
    @FXML private TableColumn<IpData, String> ipValueColumn;
    @FXML private TableColumn<IpData, String> ipHostnameColumn;
    @FXML private Label ipTablePlaceholderLabel;

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

    private ObservableList<Peer> peers = FXCollections.observableArrayList();

    public NetworkView(State state, TaskManager taskManager) {
        super();
        this.state = state;
        this.taskManager = taskManager;

        state.getPeerProcessor().addPeerSetChangedListener(this);
        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadIPTable();
        loadPeerTable();
        loadMessageTable();

        state.getEnvironment().addNetworkMessageListener(this);
        incomingMessages.setAll(Lists.newArrayList(state.getEnvironment().getReceivedMessages()));
        outgoingMessages.setAll(Lists.newArrayList(state.getEnvironment().getSentMessages()));

        incomingMessageDetailView = new NetworkMessageDetailView();
        incomingMasterPane.setDetailNode(incomingMessageDetailView);

        outgoingMessageDetailView = new NetworkMessageDetailView();
        outgoingMasterPane.setDetailNode(outgoingMessageDetailView);

        externalIpLabel.textProperty().bind(externalIp);
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

    private void refreshIPData() {
        ipDataTask = new Task<List<IpData>>() {
            @Override
            protected List<IpData> call() {
                updateTitle("Getting ip data...");
                try {
                    return NetworkUtil.getIpData();
                }
                catch (SocketException e) {
                    return null;
                }
            }
        };

        externalIpTask = new Task<String>() {
            @Override
            protected String call() {
                externalIp.set("Loading...");
                updateTitle("Getting external IP...");
                try {
                    URL ipCheck = new URL("http://checkip.amazonaws.com");
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                        ipCheck.openStream()));

                    return in.readLine();
                }
                catch (IOException e) {
                    // ignored
                }

                return null;
            }
        };

        ipDataTask.setOnSucceeded(event -> {
            if (ipDataTask.getValue() == null) {
                ipTablePlaceholderLabel.setText("Could not get IP data.");
                return;
            }

            Platform.runLater(() -> {
                List<IpData> data = ipDataTask.getValue();
                if (data != null) {
                    ipTable.setItems(FXCollections.observableArrayList(data));
                    ipTable.prefHeightProperty()
                        .bind(ipTable.fixedCellSizeProperty()
                            .multiply(Bindings.size(ipTable.getItems())
                                .add(IP_TABLE_HEIGHT_OFFSET)));
                }
            });
        });

        externalIpTask.setOnSucceeded(event -> {
            String result = externalIpTask.getValue();
            if (result == null) {
                externalIp.set("Failed getting ip");
            }
            else {

                externalIp.set(result);
            }
        });

        portLabel.setText(Integer.toString(state.getConfig().servicePort()));
        taskManager.runTask(ipDataTask);
        taskManager.runTask(externalIpTask);
    }

    private void loadIPTable() {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setSpacing(SERVICE_INFO_HBOX_SPACING);

        Label titleOfTitledPane = new Label("Service info");

        Button buttonRefresh = new Button();
        BraboGlyph refreshIcon = new BraboGlyph("REFRESH");
        refreshIcon.setFontSize(REFRESH_ICON_SIZE);
        buttonRefresh.setGraphic(refreshIcon);

        hbox.getChildren().add(titleOfTitledPane);
        hbox.getChildren().add(buttonRefresh);

        serviceInfoTitledPane.setGraphic(hbox);

        buttonRefresh.setOnAction(event -> refreshIPData());

        ipNameColumn.setCellValueFactory(f -> new ReadOnlyStringWrapper(f.getValue()
            .getDeviceName()));
        ipValueColumn.setCellValueFactory(f -> new ReadOnlyStringWrapper(f.getValue()
            .getAddress()));
        ipValueColumn.setCellFactory(f -> new StringTableCell<>());
        ipHostnameColumn.setCellValueFactory(f -> new ReadOnlyStringWrapper(f.getValue()
            .getHostname()));
        ipHostnameColumn.setCellFactory(f -> new StringTableCell<>());

        ipTable.setFixedCellSize(CELL_SIZE);
        ipTable.prefHeightProperty().set(IP_TABLE_INITIAL_HEIGHT);
        ipTable.minHeightProperty().bind(ipTable.prefHeightProperty());
        ipTable.maxHeightProperty().bind(ipTable.prefHeightProperty());

        ipTable.setColumnResizePolicy((f) -> SmartResize.Companion.getPOLICY().call(f));

        refreshIPData();
    }

    private void loadPeerTable() {
        peers.addAll(state.getPeerProcessor().getPeers());
        peerTable.setItems(peers);

        peerTable.setColumnResizePolicy((f) -> SmartResize.Companion.getPOLICY().call(f));

        peerIPColumn.setCellValueFactory(f -> new ReadOnlyStringWrapper(f.getValue()
            .getAddress()
            .getHostAddress()));
        peerIPColumn.setCellFactory(f -> new StringTableCell<>());

        peerHostnameColumn.setCellValueFactory(f -> new ReadOnlyStringWrapper(f.getValue()
            .getAddress()
            .getHostName()));
        peerHostnameColumn.setCellFactory(f -> new StringTableCell<>());

        peerPortColumn.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue()
            .getPort()));
        peerPortColumn.setCellFactory(f -> new IntegerTableCell<>());

        peerOutgoingMessageCountColumn.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue()
            .getOutgoingMessageQueue()
            .size()));
        peerOutgoingMessageCountColumn.setCellFactory(f -> new IntegerTableCell<>());

        peerIncomingMessageCountColumn.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue()
            .getIncomingMessageQueue()
            .size()));
        peerIncomingMessageCountColumn.setCellFactory(f -> new IntegerTableCell<>());
    }

    private void loadMessageTable() {
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
        incomingSizeColumn.setCellFactory(col -> new DecimalTableCell<>(new DecimalFormat("0"
            + ".00")));
        outgoingSizeColumn.setCellValueFactory(NetworkView::readOnlyMessageSizeFeature);
        outgoingSizeColumn.setCellFactory(col -> new DecimalTableCell<>(new DecimalFormat("0"
            + ".00")));
    }

    @Override
    public void onIncomingMessage(NetworkMessage message, boolean isUpdate) {
        Platform.runLater(() -> {
            incomingMessages.setAll(Lists.newArrayList(state.getEnvironment()
                .getReceivedMessages()));

            incomingMessagesTable.refresh();
            peerTable.refresh();
        });
    }

    @Override
    public void onOutgoingMessage(NetworkMessage message, boolean isUpdate) {
        Platform.runLater(() -> {
            outgoingMessages.setAll(Lists.newArrayList(state.getEnvironment()
                .getSentMessages()));

            outgoingMessagesTable.refresh();
            peerTable.refresh();
        });
    }

    @Override
    public void onPeerAdded(Peer peer) {
        peers.add(peer);
    }

    @Override
    public void onPeerRemoved(Peer peer) {
        peers.remove(peer);
    }

    @FXML
    protected void addPeer(ActionEvent event) {
        Optional<Peer> peerOptional = new PeerCreationDialog(
            state.getConfig(),
            state.getPeerProcessor()
        ).showAndWait();

        peerOptional.ifPresent(p -> state.getPeerProcessor().addPeer(peerOptional.get()));
    }

    @FXML
    protected void discoverPeers(ActionEvent event) {
        new Thread(() -> state.getPeerProcessor().updatePeers()).start();
    }


    @FXML
    protected void removePeer(ActionEvent event) {
        Peer peer = peerTable.getSelectionModel().getSelectedItem();
        if (peer == null) {
            return;
        }

        state.getPeerProcessor().shutdownPeer(peer);

        peerTable.refresh();
    }

    @FXML
    protected void copyPort(ActionEvent event) {
        setClipboardContent(Integer.toString(state.getConfig().servicePort()));
    }

    @FXML
    protected void copyExternalIP(ActionEvent event) {
        setClipboardContent(externalIp.get());
    }

    @FXML
    protected void refreshExternalIP(ActionEvent event) {
        taskManager.runTask(externalIpTask);
    }

    private void setClipboardContent(String content) {
        StringSelection selection =
            new StringSelection(content);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }
}
