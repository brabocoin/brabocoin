package org.brabocoin.brabocoin.gui.control.table;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;
import org.brabocoin.brabocoin.gui.glyph.BraboGlyph;

public class BooleanIconTableCell<S> extends TableCell<S, Boolean> {

    private final BraboGlyph.Icon trueIcon;
    private final BraboGlyph.Icon falseIcon;
    private final Color trueColor;
    private final Color falseColor;

    public BooleanIconTableCell(BraboGlyph.Icon trueIcon, BraboGlyph.Icon falseIcon) {
        this(trueIcon, falseIcon, Color.BLACK, Color.BLACK);
    }

    public BooleanIconTableCell(BraboGlyph.Icon trueIcon, BraboGlyph.Icon falseIcon,
                                Color trueColor, Color falseColor) {
        this.trueIcon = trueIcon;
        this.falseIcon = falseIcon;
        this.trueColor = trueColor;
        this.falseColor = falseColor;
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        this.setAlignment(Pos.CENTER);
    }

    @Override
    protected void updateItem(Boolean item, boolean empty) {
        super.updateItem(item, empty);

        boolean clearGraphic = empty || item == null;


        BraboGlyph.Icon icon = null;
        if (!clearGraphic) {
            icon = item ? trueIcon : falseIcon;
        }

        if (clearGraphic || icon == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        BraboGlyph glyph = new BraboGlyph(icon);
        glyph.setColor(item ? trueColor : falseColor);

        setGraphic(glyph);
    }
}