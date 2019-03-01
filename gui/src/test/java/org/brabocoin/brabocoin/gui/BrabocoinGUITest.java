package org.brabocoin.brabocoin.gui;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.brabocoin.brabocoin.test.ImageAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ExtendWith(ApplicationExtension.class)
class BrabocoinGUITest {

    private static final double ANNOTATION_CIRCLE_SIZE = 20;
    private static final Color FILL_COLOR = Color.RED;
    private static final Color CIRCLE_COLOR = Color.WHITE;
    private static final Path SAVE_LOCATION = Paths.get("screenshots");
    private BrabocoinGUI application;

    @Start
    private void start(Stage stage) throws IOException {
        Files.createDirectories(SAVE_LOCATION);
        application = new BrabocoinGUI();
        application.start(stage);
    }

    private void capture(String filename, ImageAnnotation... annotations) {
        capture(FxToolkit.toolkitContext().getRegisteredStage().getScene(), filename, annotations);
    }

    private void capture(Scene scene, String filename, ImageAnnotation... annotations) {
        Platform.runLater(() -> {
            Image image = scene.snapshot(null);

            Canvas canvas = new Canvas(image.getWidth(), image.getHeight());
            GraphicsContext context = canvas.getGraphicsContext2D();
            context.drawImage(image, 0, 0);
            context.setStroke(FILL_COLOR);
            context.setTextAlign(TextAlignment.CENTER);
            context.setTextBaseline(VPos.CENTER);

            for (int i = 0; i < annotations.length; i++) {
                double x = annotations[i].getX();
                double y = annotations[i].getY();
                String text = annotations[i].getText();

                context.setFill(CIRCLE_COLOR);
                context.fillOval(x - ANNOTATION_CIRCLE_SIZE / 2, y - ANNOTATION_CIRCLE_SIZE / 2, ANNOTATION_CIRCLE_SIZE, ANNOTATION_CIRCLE_SIZE);
                context.strokeOval(x - ANNOTATION_CIRCLE_SIZE / 2, y - ANNOTATION_CIRCLE_SIZE / 2, ANNOTATION_CIRCLE_SIZE, ANNOTATION_CIRCLE_SIZE);

                context.setFill(FILL_COLOR);
                context.fillText(text != null ? text : Integer.toString(i + 1), x, y);
            }

            WritableImage annotated = canvas.snapshot(null, null);

            try {
                ImageIO.write(SwingFXUtils.fromFXImage(annotated, null), "png", SAVE_LOCATION.resolve(filename + ".png").toFile());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    void makeScreenshots(FxRobot robot) {
        robot.sleep(500);
        capture("preloader");

        try {
            WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS, () -> application.getMainStage() != null && application.getMainStage().isShowing());
        }
        catch (TimeoutException e) {
            e.printStackTrace();
        }

        capture(application.getMainStage().getScene(), "mainview");
    }
}
