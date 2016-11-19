import model.LaneType;
import model.Wizard;
import model.World;

import java.util.Arrays;
import java.util.TreeMap;

/**
 * Created by dragoon on 11/8/16.
 */
public class Drawing_DrawingData {

    private Wizard self;
    private World world;
    private LaneType myLine;
    private BuildingPhantom[] buildingPhantoms;
    private double[] maxCastRange;
    private CurrentAction currentAction;
    private TreeMap<Long, Double> projectilesDTL;

    public Drawing_DrawingData(Wizard self,
                               World world,
                               LaneType myLine,
                               BuildingPhantom[] buildingPhantoms,
                               double[] maxCastRange,
                               CurrentAction currentAction,
                               TreeMap<Long, Double> projectilesDT) {
        this.self = self;
        this.world = world;
        this.myLine = myLine;
        this.buildingPhantoms = new BuildingPhantom[buildingPhantoms.length];
        for (int i = 0; i != buildingPhantoms.length; ++i) {
            this.buildingPhantoms[i] = new BuildingPhantom(buildingPhantoms[i], false);
        }
        this.maxCastRange = Arrays.copyOf(maxCastRange, maxCastRange.length);
        this.currentAction = currentAction.clone();
        this.projectilesDTL = new TreeMap<>(projectilesDT);
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

    public double[] getMaxCastRange() {
        return maxCastRange;
    }

    public CurrentAction getCurrentAction() {
        return currentAction;
    }

    public TreeMap<Long, Double> getProjectilesDTL() {
        return projectilesDTL;
    }

    public Drawing_DrawingData clone() {
        return new Drawing_DrawingData(self, world, myLine, buildingPhantoms, maxCastRange, currentAction, projectilesDTL);
    }
}
