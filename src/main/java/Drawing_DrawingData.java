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
    private BuildingPhantom[] buildingPhantoms;

    public Drawing_DrawingData(Wizard self, World world, LaneType myLine, BuildingPhantom[] buildingPhantoms) {
        this.self = self;
        this.world = world;
        this.myLine = myLine;
        this.buildingPhantoms = new BuildingPhantom[buildingPhantoms.length];
        for (int i = 0; i != buildingPhantoms.length; ++i) {
            this.buildingPhantoms[i] = new BuildingPhantom(buildingPhantoms[i], false);
        }
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

    public BuildingPhantom[] getBuildingPhantoms() {
        return buildingPhantoms;
    }
}
