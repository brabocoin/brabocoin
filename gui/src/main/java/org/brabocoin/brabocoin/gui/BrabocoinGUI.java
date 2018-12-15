package org.brabocoin.brabocoin.gui;

import com.beust.jcommander.JCommander;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.brabocoin.brabocoin.BrabocoinApplication;
import org.brabocoin.brabocoin.cli.BraboArgs;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.StateInitializationException;
import org.brabocoin.brabocoin.gui.dialog.UnlockDialog;
import org.brabocoin.brabocoin.gui.view.MainView;
import org.brabocoin.brabocoin.node.state.Unlocker;
import org.brabocoin.brabocoin.util.Destructible;
import org.brabocoin.brabocoin.wallet.Wallet;
import org.fxmisc.cssfx.CSSFX;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Brabocoin GUI application.
 */
public class BrabocoinGUI extends Application {

    private static final String VERSION = BrabocoinGUI.class.getPackage()
        .getImplementationVersion() != null ? BrabocoinGUI.class.getPackage()
        .getImplementationVersion() : "[development version]";

    private static final List<String> ICONS = Arrays.asList(
        "icon/16.png",
        "icon/24.png",
        "icon/32.png",
        "icon/40.png",
        "icon/48.png"
    );

    @Override
    public void start(Stage stage) throws DatabaseException {
        // Parse parameters
        BraboArgs arguments = new BraboArgs();
        JCommander commander = JCommander.newBuilder().addObject(arguments).build();
        commander.parse(getParameters().getRaw().toArray(new String[0]));

        if (arguments.isHelp()) {
            commander.usage();
            Platform.runLater(Platform::exit);
            return;
        }

        Unlocker<Wallet> walletUnlocker = arguments.getPassword() != null
            ? (creation, creator) -> creator.apply(arguments.getPassword())
            : this::createUnlockerDialog;

        BrabocoinApplication application = new BrabocoinApplication(
            arguments.getConfig(),
            walletUnlocker
        );

        MainView mainView = new MainView(application.getState());
        Scene scene = new Scene(mainView);

        // Auto CSS reloading
        CSSFX.addConverter(uri -> Paths.get(uri.startsWith("file:/") ? uri.replace("file:/", "")
            .replace("out/production/", "src/main/") : uri)).start();

        // Add base stylesheet
        scene.getStylesheets().add(getClass().getResource("brabocoin.css").toExternalForm());

        stage.getIcons()
            .addAll(ICONS.stream()
                .map(path -> new Image(BrabocoinGUI.class.getResourceAsStream(path)))
                .collect(Collectors.toList()));

        stage.setTitle("Brabocoin " + VERSION);
        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> {
            try {
                application.start();
            }
            catch (IOException | DatabaseException e) {
                Platform.exit();
            }
        });
    }

    private Wallet createUnlockerDialog(boolean creation, Function<Destructible<char[]>, Wallet> creator) {
        UnlockDialog<Wallet> dialog = new UnlockDialog<>(creation, creator);

        dialog.setTitle("Brabocoin " + VERSION);
        dialog.setHeaderText("Unlock your wallet");

        ((Stage)dialog.getDialogPane().getScene().getWindow()).getIcons()
            .addAll(ICONS.stream()
                .map(path -> new Image(BrabocoinGUI.class.getResourceAsStream(path)))
                .collect(Collectors.toList()));

        dialog.getDialogPane().getScene().getStylesheets().add(getClass().getResource("brabocoin.css").toExternalForm());

        return dialog.showAndWait().orElseThrow(() ->
            new StateInitializationException("Unlock dialog was cancelled.")
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}
