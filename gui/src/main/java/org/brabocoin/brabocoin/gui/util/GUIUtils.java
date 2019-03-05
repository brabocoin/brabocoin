package org.brabocoin.brabocoin.gui.util;

import javafx.scene.control.Alert;
import org.brabocoin.brabocoin.gui.dialog.BraboDialog;
import org.brabocoin.brabocoin.validation.consensus.Consensus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class GUIUtils {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");

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
