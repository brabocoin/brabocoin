package org.brabocoin.brabocoin.gui.tableentry;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.brabocoin.brabocoin.gui.BrabocoinGUI;

import java.util.Objects;
import java.util.function.Function;

public class ValidatedEditCell<S, T> extends EditCell<S, T> {

    private final Function<T, Boolean> validator;

    private Boolean invalid = false;

    public ValidatedEditCell(StringConverter<T> converter, Function<T, Boolean> validator) {
        super(converter);

        this.validator = validator;

        // Add base stylesheet
        this.getStylesheets()
            .add(BrabocoinGUI.class.getResource("brabocoin.css").toExternalForm());
    }

    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
        final StringConverter<T> converter) {
        return list -> new ValidatedEditCell<>(converter, Objects::nonNull);
    }

    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
        final StringConverter<T> converter, Function<T, Boolean> validator) {
        return list -> new ValidatedEditCell<>(converter, validator);
    }

    @Override
    public void commitEdit(T newValue) {
        if (!validator.apply(newValue)) {
            invalid = true;
            return;
        }

        invalid = true;

        super.commitEdit(newValue);
    }

    @Override
    protected TextField getTextField() {
        TextField superField = super.getTextField();

        String defaultStyle = superField.getStyle();

        superField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (invalid && !newValue) {
                superField.setStyle("-fx-text-inner-color: red;");
                superField.requestFocus();
            }
        });

        superField.textProperty()
            .addListener((observable, oldValue, newValue) -> superField.setStyle(defaultStyle));

        return superField;
    }
}
