package org.brabocoin.brabocoin.gui.view;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.window.DataWindow;
import org.brabocoin.brabocoin.node.MessageArtifact;
import org.brabocoin.brabocoin.node.NetworkMessage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class NetworkMessageDetailView extends SplitPane implements BraboControl, Initializable {

    private static final double DIVIDER_POSITION = 0.5;
    private static final double TITLED_PANE_SPACING = 10.0;
    private final ObjectProperty<NetworkMessage> networkMessage = new SimpleObjectProperty<>();
    @FXML public VBox responseArtifactPane;
    @FXML public VBox requestArtifactPane;
    @FXML public TitledPane responseTitledPane;
    @FXML public TitledPane requestTitledPane;
    @FXML public ScrollPane requestScrollPane;
    @FXML public ScrollPane responseScrollPane;

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
        addArtifacts(requestMessages, requestArtifactPane, requestTitledPane);
        List<MessageArtifact> responseMessages = message.getResponseMessages();
        addArtifacts(responseMessages, responseArtifactPane, responseTitledPane);
    }

    private void addArtifacts(List<MessageArtifact> artifacts, VBox destinationBox,
                              TitledPane titledPane) {
        destinationBox.getChildren().clear();
        if (artifacts.size() > 1) {
            if (titledPane.getGraphic() != null) {
                titledPane.setText(
                    extractTitleFromTitledArtifactPane(titledPane)
                );
                titledPane.setGraphic(null);
            }
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
            String title;
            if (titledPane.getGraphic() == null) {
                title = titledPane.getText();
            } else {
                title = extractTitleFromTitledArtifactPane(titledPane);
            }
            titledPane.setGraphic(createTitledPaneGraphic(
                title,
                artifacts.get(0).getMessage()
            ));
            titledPane.setText("");
        }
        else {
            destinationBox.getChildren().add(
                new Label("No messages found.")
            );
        }
    }

    private String extractTitleFromTitledArtifactPane(TitledPane titledPane) {
        return ((Label)((HBox)titledPane.getGraphic()).getChildren().get(0)).getText();
    }

    private TitledPane createMessageArtifactTitledPane(MessageArtifact artifact, int index) {
        TitledPane titledPane = createShowDataTitledPane(
            "Artifact " + index,
            createMessageArtifactTextArea(artifact),
            artifact.getMessage()
        );
        titledPane.setCollapsible(true);
        titledPane.setExpanded(false);

        return titledPane;
    }

    private HBox createTitledPaneGraphic(String title, Message message) {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setSpacing(TITLED_PANE_SPACING);

        Label titleOfTitledPane = new Label(title);

        Button buttonShowData = new Button("Show data");
        buttonShowData.setOnAction(event -> new DataWindow(message).show());

        hbox.getChildren().add(titleOfTitledPane);
        hbox.getChildren().add(buttonShowData);

        return hbox;
    }

    private TitledPane createShowDataTitledPane(String title, Node content, Message message) {
        TitledPane pane = new TitledPane("", content);
        pane.setGraphic(createTitledPaneGraphic(title, message));

        return pane;
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

        return content;
    }

    public void setNetworkMessage(NetworkMessage networkMessage) {
        this.networkMessage.set(networkMessage);
    }
}
