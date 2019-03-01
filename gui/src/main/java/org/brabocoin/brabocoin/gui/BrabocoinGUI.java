package org.brabocoin.brabocoin.gui;

import com.beust.jcommander.JCommander;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.brabocoin.brabocoin.BrabocoinApplication;
import org.brabocoin.brabocoin.cli.BraboArgs;
import org.brabocoin.brabocoin.exceptions.StateInitializationException;
import org.brabocoin.brabocoin.gui.dialog.BraboDialog;
import org.brabocoin.brabocoin.gui.dialog.UnlockDialog;
import org.brabocoin.brabocoin.gui.util.WalletUtils;
import org.brabocoin.brabocoin.gui.view.MainView;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.node.state.Unlocker;
import org.brabocoin.brabocoin.util.Destructible;
import org.brabocoin.brabocoin.util.LoggingUtil;
import org.brabocoin.brabocoin.wallet.Wallet;
import org.controlsfx.dialog.ExceptionDialog;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Brabocoin GUI application.
 */
public class BrabocoinGUI extends Application {

    private static final String VERSION = BrabocoinGUI.class.getPackage()
        .getImplementationVersion() != null ? BrabocoinGUI.class.getPackage()
        .getImplementationVersion() : "[development version]";

    public static final List<String> ICONS = Arrays.asList(
        "icon/16.png",
        "icon/24.png",
        "icon/32.png",
        "icon/40.png",
        "icon/48.png"
    );

    private static final double MIN_WIDTH = 800.0;
    private static final double MIN_HEIGHT = 600.0;

    private Stage mainStage;

    @Override
    public void start(Stage stage) {
        // Set exception dialog handler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            ExceptionDialog dialog = new ExceptionDialog(e);
            BraboDialog.setBraboStyling(dialog.getDialogPane());
            Platform.runLater(dialog::showAndWait);
        });

        // Parse parameters
        BraboArgs arguments = new BraboArgs();
        JCommander commander = JCommander.newBuilder().addObject(arguments).build();
        Parameters parameters = getParameters();
        if (parameters != null) {
            commander.parse(parameters.getRaw().toArray(new String[0]));
        }

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

                // Set log level
                Level levelSet = LoggingUtil.setLogLevel(arguments.getLogLevel());
                if (levelSet == null) {
                    levelSet = Level.INFO;
                }

                updateMessage("Starting network node...");

                application.start();

                updateMessage("Initializing user interface...");

                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Stage> mainStage = new AtomicReference<>();
                final Level finalLevelSet = levelSet;
                Platform.runLater(() -> {
                    MainView mainView = new MainView(application.getState(), finalLevelSet);
                    Scene scene = new Scene(mainView);

                    // Add base stylesheet
                    scene.getStylesheets()
                        .add(getClass().getResource("brabocoin.css").toExternalForm());

                    Stage stage = new Stage();
                    stage.getIcons()
                        .addAll(ICONS.stream()
                            .map(path -> new Image(BrabocoinGUI.class.getResourceAsStream(path)))
                            .collect(Collectors.toList()));

                    stage.setTitle("Brabocoin " + VERSION);
                    stage.setScene(scene);

                    stage.setMinWidth(MIN_WIDTH);
                    stage.setMinHeight(MIN_HEIGHT);

                    stage.setOnCloseRequest(t -> onExit(t, application.getState()));

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

        showSplash(stage, startupTask, () -> {
            mainStage = startupTask.getValue();
            mainStage.show();
        });
        new Thread(startupTask).start();
    }

    private void onExit(WindowEvent t, State state) {
        Object result = null;

        Optional<Object> optionalO = WalletUtils.saveWallet(state);
        if (optionalO.isPresent()) {
            result = optionalO.get();
        }

        if (result == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);

            alert.getButtonTypes().add(ButtonType.CANCEL);

            alert.setTitle("Discard wallet changes");
            alert.setHeaderText("Your wallet is not saved.");
            alert.setContentText("Your wallet has unsaved changes, are you sure you want to exit?");

            BraboDialog.setBraboStyling(alert.getDialogPane());

            Optional<ButtonType> alertResult = alert.showAndWait();

            if (alertResult.isPresent()) {
                ButtonType r = alertResult.get();
                if (r == ButtonType.CLOSE || r == ButtonType.CANCEL) {
                    t.consume();
                    return;
                }
            }
        }

        System.exit(0);
    }

    private void showSplash(Stage primaryStage, Task<?> task, Runnable completionHandler) {
        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("icon/icon-h50.png")));
        icon.setCache(true);
        Label text = new Label("Brabocoin");
        text.setFont(Font.font(30));
        text.setTextFill(Color.WHITE);
        HBox hBox = new HBox(20, icon, text);
        hBox.setAlignment(Pos.CENTER);
        Group logoGroup = new Group(hBox);
        StackPane logoPane = new StackPane(logoGroup);
        StackPane.setAlignment(logoGroup, Pos.CENTER);
        logoPane.setPadding(new Insets(20, 20, 10, 20));
        logoPane.setStyle("-fx-background-color: -fx-accent;");
        VBox.setVgrow(logoPane, Priority.ALWAYS);

        ProgressBar progressBar = new ProgressBar();
        Label progressLabel = new Label("");
        VBox progressContainer = new VBox(progressLabel, progressBar);
        VBox.setVgrow(progressBar, Priority.NEVER);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressContainer.setPadding(new Insets(10, 20, 20, 20));
        progressContainer.setAlignment(Pos.BOTTOM_LEFT);

        VBox layout = new VBox(logoPane, progressContainer);
        layout.setPrefWidth(300);
        layout.setPrefHeight(200);

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

    private Wallet createUnlockerDialog(boolean creation,
                                        Function<Destructible<char[]>, Wallet> creator) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Optional<Wallet>> wallet = new AtomicReference<>();

        Platform.runLater(() -> {
            UnlockDialog<Wallet> dialog = new UnlockDialog<>(creation, creator);

            dialog.setTitle("Brabocoin " + VERSION);
            if (creation) {
                dialog.setHeaderText("Create a password to encrypt your wallet");
            }
            else {
                dialog.setHeaderText("Unlock your wallet");
            }

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

    public Stage getMainStage() {
        return mainStage;
    }
}
