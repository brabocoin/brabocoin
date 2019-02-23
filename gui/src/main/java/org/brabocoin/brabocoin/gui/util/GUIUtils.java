package org.brabocoin.brabocoin.gui.util;

import org.brabocoin.brabocoin.validation.Consensus;

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
}
