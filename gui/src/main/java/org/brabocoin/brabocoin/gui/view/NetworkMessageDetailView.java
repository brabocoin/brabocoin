package org.brabocoin.brabocoin.gui.view;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.node.NetworkMessage;

import java.net.URL;
import java.util.ResourceBundle;

public class NetworkMessageDetailView extends VBox implements BraboControl, Initializable {

    @FXML public TextArea jsonTextAreaRequest;
    @FXML public TextArea jsonTextAreaResponse;
    private final ObjectProperty<NetworkMessage> networkMessage = new SimpleObjectProperty<>();

    public NetworkMessageDetailView() {
        super();

        BraboControlInitializer.initialize(this);

        networkMessage.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadNetworkMessage(newValue);
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    private void loadNetworkMessage(NetworkMessage message) {
        try {
            jsonTextAreaRequest.setText(
                message.getRequestMessage() != null ?
                    JsonFormat.printer().print(message.getRequestMessage()) : "");
            jsonTextAreaResponse.setText(
                message.getResponseMessage() != null ?
                    JsonFormat.printer().print(message.getResponseMessage()) : "");
        }
        catch (InvalidProtocolBufferException e) {
            // ignore
        }
    }

    public void setNetworkMessage(NetworkMessage networkMessage) {
        this.networkMessage.set(networkMessage);
    }
}
