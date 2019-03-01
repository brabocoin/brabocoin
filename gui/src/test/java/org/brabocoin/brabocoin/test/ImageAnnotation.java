package org.brabocoin.brabocoin.test;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import org.testfx.service.locator.BoundsLocator;
import org.testfx.service.locator.PointLocator;
import org.testfx.service.locator.impl.BoundsLocatorImpl;
import org.testfx.service.locator.impl.PointLocatorImpl;

/**
 * Annotates an image.
 */
public class ImageAnnotation {

    private static final Pos DEFAULT_ANCHOR = Pos.BOTTOM_RIGHT;
    private static final double DEFAULT_OFFSET = 10;

    private static final BoundsLocator BOUNDS_LOCATOR = new BoundsLocatorImpl();
    private static final PointLocator POINT_LOCATOR = new PointLocatorImpl(BOUNDS_LOCATOR);

    private final double x;
    private final double y;
    private final String text;

    public static ImageAnnotation annotate(String text, Node node, Pos anchor, double dx, double dy) {
        Point2D p = POINT_LOCATOR.point(node).onNode(node).atPosition(anchor).atOffset(dx, dy).query();
        p = node.localToScene(node.screenToLocal(p));

        return new ImageAnnotation(p.getX(), p.getY(), text);
    }

    public static ImageAnnotation annotate(Node node) {
        return annotate(null, node, DEFAULT_ANCHOR, DEFAULT_OFFSET, DEFAULT_OFFSET);
    }

    public static ImageAnnotation annotate(String text, Node node) {
        return annotate(text, node, DEFAULT_ANCHOR, DEFAULT_OFFSET, DEFAULT_OFFSET);
    }

    public static ImageAnnotation annotate(Node node, Pos anchor) {
        return annotate(null, node, anchor, 0, 0);
    }

    public static ImageAnnotation annotate(String text, Node node, Pos anchor) {
        return annotate(text, node, anchor, 0, 0);
    }

    public static ImageAnnotation annotate(Node node, Pos anchor, double dx, double dy) {
        return annotate(null, node, anchor, dx, dy);
    }

    public static ImageAnnotation annotate(double x, double y) {
        return new ImageAnnotation(x, y);
    }

    public static ImageAnnotation annotate(String text, double x, double y) {
        return new ImageAnnotation(x, y, text);
    }

    public ImageAnnotation(double x, double y, String text) {
        this.x = x;
        this.y = y;
        this.text = text;
    }

    public ImageAnnotation(double x, double y) {
        this(x, y, null);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getText() {
        return text;
    }
}
