package org.brabocoin.brabocoin.gui.view;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.node.MessageArtifact;
import org.brabocoin.brabocoin.node.NetworkMessage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class NetworkMessageDetailView extends SplitPane implements BraboControl, Initializable {

    private static final double DIVIDER_POSITION = 0.5;
    private final ObjectProperty<NetworkMessage> networkMessage = new SimpleObjectProperty<>();
    @FXML public VBox responseArtifactPane;
    @FXML public VBox requestArtifactPane;

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
        this.setDividerPositions(DIVIDER_POSITION);
        this.setOrientation(Orientation.VERTICAL);
    }

    private void loadNetworkMessage(NetworkMessage message) {
        List<MessageArtifact> requestMessages = message.getRequestMessages();
        addArtifacts(requestMessages, requestArtifactPane);
        List<MessageArtifact> responseMessages = message.getResponseMessages();
        addArtifacts(responseMessages, responseArtifactPane);
    }

    private void addArtifacts(List<MessageArtifact> artifacts, VBox destinationBox) {
        destinationBox.getChildren().clear();
        if (artifacts.size() > 1) {
            for (int i = 0; i < artifacts.size(); i++) {
                MessageArtifact artifact = artifacts.get(i);
                TitledPane artifactPane = createMessageArtifactTitledPane(artifact, i);

                destinationBox.getChildren().add(artifactPane);
            }
        }
        else if (artifacts.size() == 1) {
            destinationBox.getChildren().add(
                createMessageArtifactTextArea(artifacts.get(0))
            );
        }
    }

    private TitledPane createMessageArtifactTitledPane(MessageArtifact artifact, int index) {
        TitledPane titledPane = new TitledPane(
            "Artifact " + index,
            createMessageArtifactTextArea(artifact)
        );
        titledPane.setCollapsible(true);
        titledPane.collapsibleProperty().setValue(index == 0);

        return titledPane;
    }

    private TextArea createMessageArtifactTextArea(MessageArtifact artifact) {
        String json = "";
        try {
            json = JsonFormat.printer().print(artifact.getMessage());
        }
        catch (InvalidProtocolBufferException e) {
            // ignored
        }
        TextArea content = new TextArea(json);
        content.setEditable(false);

        content.autosize();

        return content;
    }

    public void setNetworkMessage(NetworkMessage networkMessage) {
        this.networkMessage.set(networkMessage);
    }
}
