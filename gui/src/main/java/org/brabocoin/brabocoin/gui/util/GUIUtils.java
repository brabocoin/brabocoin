package org.brabocoin.brabocoin.gui.util;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.brabocoin.brabocoin.gui.dialog.BraboDialog;
import org.brabocoin.brabocoin.validation.Consensus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class GUIUtils {
    private static Method columnToFitMethod;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");

    static {
        try {
            columnToFitMethod = TableViewSkin.class.getDeclaredMethod("resizeColumnToFitContent", TableColumn.class, int.class);
            columnToFitMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void autoFitTable(TableView tableView) {
        tableView.getItems().addListener((ListChangeListener<Object>)c -> {
            for (Object column : tableView.getColumns()) {
                try {
                    columnToFitMethod.invoke(tableView.getSkin(), column, -1);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static String formatValue(long value, boolean addSuffix) {
        BigDecimal roundedOutput = BigDecimal.valueOf(value)
            .divide(BigDecimal.valueOf(Consensus.COIN), 2, RoundingMode.UNNECESSARY);
        String formattedValue = DECIMAL_FORMAT.format(roundedOutput);

        if (addSuffix) {
            return formattedValue + " BRC";
        }

        return formattedValue;
    }

    public static String formatValue(double value) {
        return DECIMAL_FORMAT.format(value);
    }

    public static void displayErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        BraboDialog.setBraboStyling(alert.getDialogPane());

        alert.showAndWait();
    }
}