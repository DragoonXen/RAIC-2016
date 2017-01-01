import java.awt.*;

/**
 * Created by by.dragoon on 11/1/16.
 */
public class Drawing_Polygon extends Drawing_Figure {

    private double x[];
    private double y[];

    private boolean fill;
    private Color color;
    private Color borderColor;

    public Drawing_Polygon(double[] x, double[] y, Color color) {
        this(x, y, false, color, null);
    }

    public Drawing_Polygon(double[] x, double[] y, boolean filled, Color color) {
        this(x, y, filled, color, null);
    }

    public Drawing_Polygon(double[] x, double[] y, boolean filled, Color color, Color borderColor) {
        assert x.length == y.length;
        this.x = x;
        this.y = y;
        this.fill = filled;
        this.color = color;
        this.borderColor = borderColor;
    }

    public Drawing_Polygon(double[] x, double[] y, Color color, int priority) {
        this(x, y, false, color, null, priority);
    }

    public Drawing_Polygon(double[] x, double[] y, boolean filled, Color color, int priority) {
        this(x, y, filled, color, null, priority);
    }

    public Drawing_Polygon(double[] x, double[] y, boolean filled, Color color, Color borderColor, int priority) {
        super(priority);
        assert x.length == y.length;
        this.x = x;
        this.y = y;
        this.fill = filled;
        this.color = color;
        this.borderColor = borderColor;
    }

    public void paint(Drawing_DrawPanel where, Graphics g) {
        g.setColor(color);
        int fixedX[] = new int[x.length];
        int fixedY[] = new int[y.length];
        for (int i = 0; i != x.length; ++i) {
            fixedX[i] = where.fixX(x[i]);
            fixedY[i] = where.fixY(y[i]);
        }
        if (fill) {
            g.fillPolygon(fixedX, fixedY, fixedX.length);
            if (borderColor != null) {
                g.setColor(borderColor);
                g.drawPolygon(fixedX, fixedY, fixedX.length);
            }
        } else {
            g.drawPolygon(fixedX, fixedY, fixedX.length);
        }
    }
}
