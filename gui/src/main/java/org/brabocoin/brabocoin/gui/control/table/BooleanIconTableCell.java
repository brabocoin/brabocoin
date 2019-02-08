package org.brabocoin.brabocoin.gui.control.table;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import org.brabocoin.brabocoin.gui.glyph.BraboGlyph;

public class BooleanIconTableCell<S> extends TableCell<S, Boolean> {
    private BraboGlyph.Icon trueIcon;
    private BraboGlyph.Icon falseIcon;

    public BooleanIconTableCell(BraboGlyph.Icon trueIcon, BraboGlyph.Icon falseIcon) {
        this.trueIcon = trueIcon;
        this.falseIcon = falseIcon;
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        this.setAlignment(Pos.CENTER);
    }

    @Override
    protected void updateItem(Boolean item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        setGraphic(new BraboGlyph(item ? trueIcon : falseIcon));
    }
}