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
        FONT_AWESOME("Font Awesome 5 Pro Regular", "fa-regular-400.ttf"),
        FONT_AWESOME_SOLID("Font Awesome 5 Pro Solid", "fa-solid-900.ttf"),
        FONT_AWESOME_LIGHT("Font Awesome 5 Pro Light", "fa-light-300.ttf"),
        FONT_AWESOME_BRANDS("Font Awesome 5 Brands Regular", "fa-brands-400.ttf");

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
        ANGLE_DOUBLE_DOWN(Font.FONT_AWESOME_SOLID, '\uf103'),
        ARROW_DOWN(Font.FONT_AWESOME_SOLID, '\uf063'),
        ARROW_UP(Font.FONT_AWESOME_SOLID, '\uf062'),
        BITCOIN(Font.FONT_AWESOME_BRANDS, '\uf379'),
        BROADCAST_TOWER(Font.FONT_AWESOME_SOLID, '\uf519'),
        CHECK(Font.FONT_AWESOME_SOLID, '\uf058'),
        CODE_BRANCH(Font.FONT_AWESOME_SOLID, '\uf126'),
        DATABASE(Font.FONT_AWESOME_SOLID, '\uf1c0'),
        LEVEL_DOWN_ALT(Font.FONT_AWESOME_SOLID, '\uf3be'),
        MAGIC(Font.FONT_AWESOME_SOLID, '\uf0d0'),
        MICROCHIP(Font.FONT_AWESOME_SOLID, '\uf2db'),
        MINUS(Font.FONT_AWESOME_SOLID, '\uf068'),
        PLUS(Font.FONT_AWESOME_SOLID, '\uf067'),
        SEARCH(Font.FONT_AWESOME_SOLID, '\uf002'),
        STOP(Font.FONT_AWESOME_SOLID, '\uf04d'),
        TASKS(Font.FONT_AWESOME_SOLID, '\uf0ae'),
        TIMES(Font.FONT_AWESOME_SOLID, '\uf00d'),
        WALLET(Font.FONT_AWESOME_SOLID, '\uf555'),
        CIRCLE(Font.FONT_AWESOME_SOLID, '\uf111'),
        CROSS(Font.FONT_AWESOME_SOLID, '\uf057'),
        CIRCLEMINUS(Font.FONT_AWESOME_SOLID, '\uf056'),
        CUBE(Font.FONT_AWESOME_SOLID, '\uf1b2'),
        TOOLS(Font.FONT_AWESOME_SOLID, '\uf0ad'),
        REPEAT(Font.FONT_AWESOME_SOLID, '\uf363'),
        LOCK(Font.FONT_AWESOME_SOLID, '\uf023'),
        UNLOCK(Font.FONT_AWESOME_SOLID, '\uf3c1'),
        COPY(Font.FONT_AWESOME_SOLID, '\uf0c5'),
        REFRESH(Font.FONT_AWESOME_SOLID, '\uf2f1'),
        HAPPY(Font.FONT_AWESOME_SOLID, '\uf5b8'),
        CRYSAD(Font.FONT_AWESOME_SOLID, '\uf5b3'),
        INFO(Font.FONT_AWESOME_SOLID, '\uf05a');

        private final @NotNull Font font;
        private final char ch;

        Icon(@NotNull Font font, char ch) {
            this.font = font;
            this.ch = ch;

            this.font.glyphFont.register(this.name(), this.ch);
        }
    }
}
