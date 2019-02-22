package org.brabocoin.brabocoin.gui.util;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
        if (tableView.getSkin() == null) return;

        for (Object column : tableView.getColumns()) {
            try {
                columnToFitMethod.invoke(tableView.getSkin(), column, 1);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
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
}
