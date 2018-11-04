package org.brabocoin.brabocoin.gui.glyph;

import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sten Wessel
 */
public class BraboGlyph extends Glyph {

    private static final int DEFAULT_SIZE = 14;

    public BraboGlyph() {
        super();
    }

    public BraboGlyph(@NotNull String icon) {
        super(Icon.valueOf(icon).font.getName(), icon);
    }

    public BraboGlyph(@NotNull Icon icon) {
        super(icon.font.getName(), icon);
    }

    @Override
    public void setIcon(Object iconValue) {
        if (iconValue instanceof String) {
            setFontFamily(Icon.valueOf((String)iconValue).font.getName());
        }
        else if (iconValue instanceof Icon) {
            setFontFamily(((Icon)iconValue).font.getName());
        }

        super.setIcon(iconValue);
    }

    public enum Font {
        FONT_AWESOME("Font Awesome 5 Free", "fa-regular-400.ttf", "fa-solid-900.ttf"),
        FONT_AWESOME_BRANDS("Font Awesome 5 Brands", "fa-brands-400.ttf");

        private final @NotNull String name;
        private final @NotNull GlyphFont glyphFont;

        Font(@NotNull String name, @NotNull String... paths) {
            this.name = name;

            // Load font
            for (String path : paths) {
                javafx.scene.text.Font.loadFont(BraboGlyph.class.getResourceAsStream(path), -1);
            }

            this.glyphFont = new GlyphFont(name, DEFAULT_SIZE, "", false);

            GlyphFontRegistry.register(glyphFont);
        }

        public @NotNull String getName() {
            return name;
        }
    }

    public enum Icon {
        ANGLE_DOUBLE_DOWN(Font.FONT_AWESOME, '\uf103'),
        BITCOIN(Font.FONT_AWESOME_BRANDS, '\uf379'),
        BROADCAST_TOWER(Font.FONT_AWESOME, '\uf519'),
        DATABASE(Font.FONT_AWESOME, '\uf1c0'),
        LEVEL_DOWN_ALT(Font.FONT_AWESOME, '\uf3be'),
        MICROCHIP(Font.FONT_AWESOME, '\uf2db'),
        MINUS(Font.FONT_AWESOME, '\uf068'),
        TASKS(Font.FONT_AWESOME, '\uf0ae'),
        WALLET(Font.FONT_AWESOME, '\uf555');

        private final @NotNull Font font;
        private final char ch;

        Icon(@NotNull Font font, char ch) {
            this.font = font;
            this.ch = ch;

            this.font.glyphFont.register(this.name(), this.ch);
        }
    }
}
