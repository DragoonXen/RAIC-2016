import model.LaneType;
import model.Wizard;
import model.World;

/**
 * Created by dragoon on 11/8/16.
 */
public class Drawing_DrawingData {

    private Wizard self;
    private World world;
    private LaneType myLine;

    public Drawing_DrawingData(Wizard self, World world, LaneType myLine) {
        this.self = self;
        this.world = world;
        this.myLine = myLine;
    }

    public Wizard getSelf() {
        return self;
    }

    public World getWorld() {
        return world;
    }

    public LaneType getMyLine() {
        return myLine;
    }
}
