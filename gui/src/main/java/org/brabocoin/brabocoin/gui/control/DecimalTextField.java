package org.brabocoin.brabocoin.gui.control;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.DoubleStringConverter;
import org.brabocoin.brabocoin.validation.Consensus;

import java.util.regex.Pattern;

public class DecimalTextField extends TextField {

    public DecimalTextField() {
        super();

        Pattern validDoubleText = Pattern.compile("(\\d+(?:[,.](\\d{1,2})?)?)?");

        TextFormatter<Double> textFormatter = new TextFormatter<>(new DoubleStringConverter(), 0.0,
            change -> {
                String newText = change.getControlNewText();
                if (validDoubleText.matcher(newText).matches()) {
                    change.setText(change.getText().replace(',', '.'));
                    return change;
                }
                else {
                    return null;
                }
            }
        );

        this.setTextFormatter(textFormatter);
    }

    @Override
    public String getText(int start, int end) {
        return super.getText(start, end);
    }

    public long getCents() {
        return (long) (Double.parseDouble(getText()) * Consensus.COIN);
    }
}
