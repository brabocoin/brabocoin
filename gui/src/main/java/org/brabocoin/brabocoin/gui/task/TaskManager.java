package org.brabocoin.brabocoin.gui.task;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import org.controlsfx.control.StatusBar;
import org.jetbrains.annotations.NotNull;

/**
 * Manages asynchronous tasks run in the GUI application.
 */
public class TaskManager {

    private static final Node INDICATOR = new ProgressIndicator();

    private final @NotNull StatusBar statusBar;
    private final @NotNull ObservableList<Task<?>> tasks;

    public TaskManager(@NotNull StatusBar statusBar) {
        this.tasks = FXCollections.observableArrayList();
        this.statusBar = statusBar;

        tasks.addListener((ListChangeListener<Task>)c -> {
            statusBar.textProperty().unbind();
            switch (tasks.size()) {
                case 0:
                    statusBar.setText(null);
                    break;
                case 1:
                    statusBar.textProperty().bind(tasks.get(0).titleProperty());
                    break;
                default:
                    statusBar.setText(tasks.size() + " tasks running...");
            }
        });

        statusBar.graphicProperty().bind(Bindings.createObjectBinding(
            () -> tasks.isEmpty() ? null : INDICATOR,
            tasks
        ));
    }

    public void runTask(@NotNull Task<?> task) {
        task.stateProperty().addListener((obs, old, state) -> {
            switch (state) {
                case SCHEDULED:
                    tasks.add(task);
                    break;

                case FAILED: // TODO: error handling?
                case SUCCEEDED:
                case CANCELLED:
                    tasks.remove(task);
            }
        });

        new Thread(task).start();
    }
}
