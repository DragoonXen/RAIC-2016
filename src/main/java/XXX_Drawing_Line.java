import java.awt.*;

/**
 * Created by by.dragoon on 11/1/16.
 */
public class XXX_Drawing_Line extends XXX_Drawing_Figure {

	private double x1;
	private double y1;

	private double x2;
	private double y2;

	private Color color;

	public XXX_Drawing_Line(double x1, double y1, double x2, double y2, Color color) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.color = color;
	}

	public XXX_Drawing_Line(double x1, double y1, double x2, double y2, Color color, int priority) {
		super(priority);
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.color = color;
	}

	public void paint(XXX_Drawing_DrawPanel where, Graphics g) {
		g.setColor(color);
		g.drawLine(where.fixX(x1), where.fixY(y1), where.fixX(x2), where.fixY(y2));
	}
}
