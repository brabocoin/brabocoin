package org.brabocoin.brabocoin.gui.converter;

import javafx.util.StringConverter;
import org.brabocoin.brabocoin.gui.util.GUIUtils;

public class DoubleToCoinStringConverter extends StringConverter<Double> {

    @Override
    public String toString(Double object) {
        if (object == null) {
            return "";
        }
        return GUIUtils.formatValue(object);
    }

    @Override
    public Double fromString(String string) {
        if (string == null) {
            return 0.0;
        }
        double d;
        try {
            d = Double.parseDouble(string);
        }
        catch (NumberFormatException e) {
            return 0.0;
        }
        return round(d, 2);
    }

    private static double round(double value, int places) {
        double intermediate = value;
        if (places < 0) {
            throw new IllegalArgumentException();
        }

        long factor = (long)Math.pow(10, places);
        intermediate *= factor;
        long tmp = Math.round(intermediate);
        return (double)tmp / factor;
    }
}
