package org.brabocoin.brabocoin.gui.glyph;

import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Provides glyphs for the Brabocoin application.
 */
public class BraboGlyph extends Glyph {

    private static final int DEFAULT_SIZE = 14;

    /**
     * Create an empty glyph.
     */
    public BraboGlyph() {
        super();
    }

    /**
     * Create a glyph with the icon name matching one of the constants in {@link Icon}.
     *
     * @param icon
     *     The icon to display.
     * @throws IllegalArgumentException
     *     When the icon nome could not be found.
     */
    public BraboGlyph(@NotNull String icon) {
        super(Icon.valueOf(icon).font.getName(), icon);
    }

    /**
     * Create a glyph with a given icon.
     *
     * @param icon
     *     The icon to display.
     */
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

    /**
     * The registered icon fonts.
     */
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

    /**
     * Registered icons.
     */
    public enum Icon {
        ANGLE_DOUBLE_DOWN(Font.FONT_AWESOME, '\uf103'),
        ARROW_DOWN(Font.FONT_AWESOME, '\uf063'),
        ARROW_UP(Font.FONT_AWESOME, '\uf062'),
        BITCOIN(Font.FONT_AWESOME_BRANDS, '\uf379'),
        BROADCAST_TOWER(Font.FONT_AWESOME, '\uf519'),
        CHECK(Font.FONT_AWESOME, '\uf00c'),
        CODE_BRANCH(Font.FONT_AWESOME, '\uf126'),
        DATABASE(Font.FONT_AWESOME, '\uf1c0'),
        LEVEL_DOWN_ALT(Font.FONT_AWESOME, '\uf3be'),
        MICROCHIP(Font.FONT_AWESOME, '\uf2db'),
        MINUS(Font.FONT_AWESOME, '\uf068'),
        SEARCH(Font.FONT_AWESOME, '\uf002'),
        TASKS(Font.FONT_AWESOME, '\uf0ae'),
        TIMES(Font.FONT_AWESOME, '\uf00d'),
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
