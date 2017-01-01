import java.awt.*;

/**
 * Created by by.dragoon on 11/1/16.
 */
public abstract class Drawing_Figure implements Comparable<Drawing_Figure>{

    private int priority;

    public Drawing_Figure() {
        priority = 0;
    }

    public Drawing_Figure(int priority) {
        this.priority = priority;
    }

    public int compareTo(Drawing_Figure o) {
        return Integer.compare(priority, o.priority);
    }

    public abstract void paint(Drawing_DrawPanel where, Graphics g);

}
