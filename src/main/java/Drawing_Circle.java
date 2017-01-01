import java.awt.*;

/**
 * Created by by.dragoon on 11/1/16.
 */
public class Drawing_Circle extends Drawing_Figure {

    private double x;
    private double y;

    private double radius;

    private boolean filled;

    private Color color;
    private Color borderColor;

    public Drawing_Circle(double x, double y, double radius, boolean filled, Color color, Color borderColor) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.filled = filled;
        this.color = color;
        this.borderColor = borderColor;
    }

    public Drawing_Circle(double x, double y, double radius, Color color) {
        this(x, y, radius, false, color, null);
    }

    public Drawing_Circle(double x, double y, Color color) {
        this(x, y, 1.f, color);
    }

    public Drawing_Circle(double x, double y, double radius, boolean filled, Color color) {
        this(x, y, radius, filled, color, null);
    }

    public Drawing_Circle(double x, double y, double radius, boolean filled, Color color, Color borderColor, int priority) {
        super(priority);
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.filled = filled;
        this.color = color;
        this.borderColor = borderColor;
    }

    public Drawing_Circle(double x, double y, double radius, Color color, int priority) {
        this(x, y, radius, false, color, null, priority);
    }

    public Drawing_Circle(double x, double y, Color color, int priority) {
        this(x, y, 1.f, color, priority);
    }

    public Drawing_Circle(double x, double y, double radius, boolean filled, Color color, int priority) {
        this(x, y, radius, filled, color, null, priority);
    }

    public void paint(Drawing_DrawPanel where, Graphics g) {
        g.setColor(color);
        if (filled) {
            g.fillOval(where.fixX(x - radius),
                       where.fixY(y - radius),
                       where.fixLength(radius * 2),
                       where.fixLength(radius * 2));
            if (borderColor != null) {
                g.setColor(borderColor);
                g.drawOval(where.fixX(x - radius),
                           where.fixY(y - radius),
                           where.fixLength(radius * 2),
                           where.fixLength(radius * 2));
            }
        } else {
            g.drawOval(where.fixX(x - radius),
                       where.fixY(y - radius),
                       where.fixLength(radius * 2),
                       where.fixLength(radius * 2));
        }
    }
}
