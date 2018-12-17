package org.brabocoin.brabocoin.gui;

import com.beust.jcommander.JCommander;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.brabocoin.brabocoin.BrabocoinApplication;
import org.brabocoin.brabocoin.cli.BraboArgs;
import org.brabocoin.brabocoin.exceptions.StateInitializationException;
import org.brabocoin.brabocoin.gui.dialog.UnlockDialog;
import org.brabocoin.brabocoin.gui.view.MainView;
import org.brabocoin.brabocoin.node.state.Unlocker;
import org.brabocoin.brabocoin.util.Destructible;
import org.brabocoin.brabocoin.wallet.Wallet;
import org.controlsfx.dialog.ExceptionDialog;
import org.fxmisc.cssfx.CSSFX;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
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
    public void start(Stage stage) {
        // Parse parameters
        BraboArgs arguments = new BraboArgs();
        JCommander commander = JCommander.newBuilder().addObject(arguments).build();
        commander.parse(getParameters().getRaw().toArray(new String[0]));

        if (arguments.isHelp()) {
            commander.usage();
            Platform.exit();
            return;
        }

        // Expensive startup task
        Task<Stage> startupTask = new Task<Stage>() {
            @Override
            protected Stage call() throws Exception {
                Unlocker<Wallet> walletUnlocker = arguments.getPassword() != null
                    ? (creation, creator) -> creator.apply(arguments.getPassword())
                    : BrabocoinGUI.this::createUnlockerDialog;

                updateMessage("Loading data from disk...");

                BrabocoinApplication application = new BrabocoinApplication(
                    arguments.getConfig(),
                    walletUnlocker
                );

                updateMessage("Starting network node...");

                application.start();

                updateMessage("Initializing user interface...");

                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Stage> mainStage = new AtomicReference<>();
                Platform.runLater(() -> {
                    MainView mainView = new MainView(application.getState());
                    Scene scene = new Scene(mainView);

                    // TODO: remove!
                    // DEBUG: Auto CSS reloading
                    CSSFX.addConverter(uri -> Paths.get(uri.startsWith("file:/") ? uri.replace("file:/", "")
                        .replace("out/production/", "src/main/") : uri)).start();

                    // Add base stylesheet
                    scene.getStylesheets().add(getClass().getResource("brabocoin.css").toExternalForm());

                    Stage stage = new Stage();
                    stage.getIcons()
                        .addAll(ICONS.stream()
                            .map(path -> new Image(BrabocoinGUI.class.getResourceAsStream(path)))
                            .collect(Collectors.toList()));

                    stage.setTitle("Brabocoin " + VERSION);
                    stage.setScene(scene);

                    stage.setOnCloseRequest(t -> {
                        Platform.exit();
                        System.exit(0);
                    });

                    mainStage.set(stage);
                    latch.countDown();
                });

                try {
                    latch.await();
                }
                catch (InterruptedException e) {
                    throw new RuntimeException("Stage could not be created.", e);
                }

                return mainStage.get();
            }
        };

        showSplash(stage, startupTask, () -> startupTask.getValue().show());
        new Thread(startupTask).start();
    }

    private void showSplash(Stage primaryStage, Task<?> task, Runnable completionHandler) {
        ProgressBar progressBar = new ProgressBar();
        Label progressLabel = new Label("");
        VBox layout = new VBox(progressLabel, progressBar);
        VBox.setVgrow(progressBar, Priority.ALWAYS);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        layout.setPrefWidth(300);
        layout.setPrefHeight(200);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.BOTTOM_LEFT);

        progressBar.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());

        task.stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                progressBar.progressProperty().unbind();
                progressBar.setProgress(1);
                primaryStage.hide();
                completionHandler.run();
            }
            else if (state == Worker.State.CANCELLED || state == Worker.State.FAILED) {
                primaryStage.hide();
                if (task.getException() != null) {
                    ExceptionDialog dialog = new ExceptionDialog(task.getException());
                    dialog.showAndWait();
                }
                Platform.exit();
            }
        });

        Scene splashScene = new Scene(layout);
        splashScene.getStylesheets().add(getClass().getResource("brabocoin.css").toExternalForm());
        primaryStage.setScene(splashScene);

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.getIcons()
            .addAll(ICONS.stream()
                .map(path -> new Image(BrabocoinGUI.class.getResourceAsStream(path)))
                .collect(Collectors.toList()));

        primaryStage.show();
    }

    private Wallet createUnlockerDialog(boolean creation, Function<Destructible<char[]>, Wallet> creator) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Optional<Wallet>> wallet = new AtomicReference<>();

        Platform.runLater(() -> {
            UnlockDialog<Wallet> dialog = new UnlockDialog<>(creation, creator);

            dialog.setTitle("Brabocoin " + VERSION);
            dialog.setHeaderText("Unlock your wallet");

            ((Stage)dialog.getDialogPane().getScene().getWindow()).getIcons()
                .addAll(ICONS.stream()
                    .map(path -> new Image(BrabocoinGUI.class.getResourceAsStream(path)))
                    .collect(Collectors.toList()));

            dialog.getDialogPane().getScene().getStylesheets().add(getClass().getResource("brabocoin.css").toExternalForm());

            wallet.set(dialog.showAndWait());
            latch.countDown();
        });

        try {
            latch.await();
        }
        catch (InterruptedException e) {
            throw new StateInitializationException("Unlock dialog was interrupted.", e);
        }

        if (!wallet.get().isPresent()) {
            Platform.exit();
            System.exit(0);
        }

        return wallet.get().get();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
