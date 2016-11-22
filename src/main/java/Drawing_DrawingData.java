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
    private double[] maxCastRange;
    private CurrentAction currentAction;
    private TreeMap<Long, Double> projectilesDTL;
    private EnemyPositionCalc enemyPositionCalc;
    private BonusesPossibilityCalcs bonusesPossibilityCalcs;

    public Drawing_DrawingData(Wizard self,
                               World world,
                               LaneType myLine,
                               double[] maxCastRange,
                               CurrentAction currentAction,
                               TreeMap<Long, Double> projectilesDT,
                               EnemyPositionCalc enemyPositionCalc,
                               BonusesPossibilityCalcs bonusesPossibilityCalcs) {
        this.self = self;
        this.world = world;
        this.myLine = myLine;
        this.maxCastRange = Arrays.copyOf(maxCastRange, maxCastRange.length);
        this.currentAction = currentAction.clone();
        this.projectilesDTL = new TreeMap<>(projectilesDT);
        this.enemyPositionCalc = enemyPositionCalc.clone();
        this.bonusesPossibilityCalcs = bonusesPossibilityCalcs.clone();
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

    public double[] getMaxCastRange() {
        return maxCastRange;
    }

    public CurrentAction getCurrentAction() {
        return currentAction;
    }

    public TreeMap<Long, Double> getProjectilesDTL() {
        return projectilesDTL;
    }

    public EnemyPositionCalc getEnemyPositionCalc() {
        return enemyPositionCalc;
    }

    public BonusesPossibilityCalcs getBonusesPossibilityCalcs() {
        return bonusesPossibilityCalcs;
    }

    public Drawing_DrawingData clone() {
        return new Drawing_DrawingData(self, world, myLine, maxCastRange, currentAction, projectilesDTL, enemyPositionCalc, bonusesPossibilityCalcs);
    }
}
