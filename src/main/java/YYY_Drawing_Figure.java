import java.awt.*;

/**
 * Created by by.dragoon on 11/1/16.
 */
public abstract class YYY_Drawing_Figure implements Comparable<YYY_Drawing_Figure> {

	private int priority;

	public YYY_Drawing_Figure() {
		priority = 0;
	}

	public YYY_Drawing_Figure(int priority) {
		this.priority = priority;
	}

	public int compareTo(YYY_Drawing_Figure o) {
		return Integer.compare(priority, o.priority);
	}

	public abstract void paint(YYY_Drawing_DrawPanel where, Graphics g);

}
