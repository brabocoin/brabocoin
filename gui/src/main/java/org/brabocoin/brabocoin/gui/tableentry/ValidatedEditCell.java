package org.brabocoin.brabocoin.gui.tableentry;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.brabocoin.brabocoin.gui.BrabocoinGUI;

import java.util.Objects;
import java.util.function.Function;

public class ValidatedEditCell<S, T> extends EditCell<S, T> {

    private final Function<T, Boolean> validator;

    private BooleanProperty invalid = new SimpleBooleanProperty(false);

    public ValidatedEditCell(StringConverter<T> converter, Function<T, Boolean> validator) {
        super(converter);

        this.validator = validator;

        // Add base stylesheet
        this.getStylesheets()
            .add(BrabocoinGUI.class.getResource("brabocoin.css").toExternalForm());

        invalid.addListener((obs, old, invalid) -> {
            if (invalid) {
                getTextField().getStyleClass().add("invalid");
            }
            else {
                getTextField().getStyleClass().remove("invalid");
            }
        });
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
        invalid.set(!validator.apply(newValue));

        if (invalid.get()) {
            return;
        }
        super.commitEdit(newValue);
    }

    @Override
    public void cancelEdit() {
        if (invalid.get()) {
            return;
        }

        super.cancelEdit();
    }
}
