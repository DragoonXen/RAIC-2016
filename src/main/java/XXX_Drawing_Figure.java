import java.awt.*;

/**
 * Created by by.dragoon on 11/1/16.
 */
public abstract class XXX_Drawing_Figure implements Comparable<XXX_Drawing_Figure> {

	private int priority;

	public XXX_Drawing_Figure() {
		priority = 0;
	}

	public XXX_Drawing_Figure(int priority) {
		this.priority = priority;
	}

	public int compareTo(XXX_Drawing_Figure o) {
		return Integer.compare(priority, o.priority);
	}

	public abstract void paint(XXX_Drawing_DrawPanel where, Graphics g);

}
