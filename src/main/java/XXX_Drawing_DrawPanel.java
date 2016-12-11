import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by by.dragoon on 11/1/16.
 */
public class XXX_Drawing_DrawPanel extends JPanel {

	private static final int ZOOM_CONST = 8;

	private double fullSizeX;
	private double fullSizeY;

	private double drawStartX;
	private double drawStartY;
	private double drawWidth;
	private double drawHeight;

	private double multCoord;

	private List<XXX_Drawing_Figure> drawFigures;

	public XXX_Drawing_DrawPanel(double fullSizeX, double fullSizeY) {
		this.fullSizeX = fullSizeX;
		this.fullSizeY = fullSizeY;
		this.drawStartX = 0;
		this.drawStartY = 0;
		this.drawWidth = this.fullSizeX;
		this.drawHeight = this.fullSizeY;
		drawFigures = new LinkedList<XXX_Drawing_Figure>();

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				recalcMultCoord();
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				zoomAction(e.getX(), e.getY(), e.getButton() == 1);
			}
		});
	}

	private void recalcMultCoord() {
		Dimension size = getSize();
		double multX = size.getWidth() / (XXX_Drawing_DrawPanel.this.drawWidth + 1);
		double multY = size.getHeight() / (XXX_Drawing_DrawPanel.this.drawHeight + 1);

		multCoord = Math.min(multX, multY);
		XXX_Drawing_DrawPanel.this.repaint();
	}

	private void zoomAction(int x, int y, boolean zoomIn) {
		double cX = x / multCoord;
		double cY = y / multCoord;
		cX += XXX_Drawing_DrawPanel.this.drawStartX;
		cY += XXX_Drawing_DrawPanel.this.drawStartY;
		if (zoomIn) {
			if (drawWidth * ZOOM_CONST > fullSizeX) {
				drawWidth /= 2.;
				drawHeight /= 2.;
			}
			drawStartX = cX - drawWidth / 2.;
			drawStartY = cY - drawHeight / 2.;
			fixDrawWindow();
		} else {
			if (drawWidth == fullSizeX) {
				return;
			}
			drawWidth *= 2.;
			drawHeight *= 2.;
			if (drawWidth + 1. > fullSizeX) {
				drawWidth = fullSizeX;
				drawHeight = fullSizeY;
			}
			drawStartX = cX - drawWidth / 2.;
			drawStartY = cY - drawHeight / 2.;
			fixDrawWindow();
		}
		recalcMultCoord();
		repaint();
	}

	private void fixDrawWindow() {
		if (drawStartX < 0) {
			drawStartX = 0;
		} else if (drawStartX + drawWidth > fullSizeX) {
			drawStartX = fullSizeX - drawWidth;
		}
		if (drawStartY < 0) {
			drawStartY = 0;
		} else if (drawStartY + drawHeight > fullSizeY) {
			drawStartY = fullSizeY - drawHeight;
		}
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		synchronized (this) {
			Collections.sort(drawFigures);
			for (XXX_Drawing_Figure drawFigure : drawFigures) {
				drawFigure.paint(this, g);
			}
		}
		g.setColor(Color.BLACK);
		g.drawString("x" + String.valueOf(Math.round(fullSizeX / drawWidth)), getWidth() - 50, 50);
	}

	public int fixY(double number) {
		number -= this.drawStartY;
		return (int) Math.round(multCoord * number);
	}

	public int fixX(double number) {
		number -= this.drawStartX;
		return (int) Math.round(multCoord * number);
	}

	public int fixLength(double number) {
		return (int) Math.round(multCoord * number);
	}

	public void addFigure(XXX_Drawing_Figure figure) {
		synchronized (this) {
			this.drawFigures.add(figure);
		}
	}

	public void clear() {
		synchronized (this) {
			this.drawFigures.clear();
		}
	}

	public boolean isDrawNeeded(int minX, int maxX, int minY, int maxY) {
		Dimension size = getSize();
		return minX <= size.getWidth() && maxX >= 0 && minY <= size.getHeight() && maxY >= 0;
	}
}
