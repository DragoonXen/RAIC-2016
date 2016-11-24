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
    private double[] maxCastRange;
    private CurrentAction currentAction;
    private TreeMap<Long, Double> projectilesDTL;
    private EnemyPositionCalc enemyPositionCalc;
    private BonusesPossibilityCalcs bonusesPossibilityCalcs;
    private AgressiveNeutralsCalcs agressiveNeutralsCalcs;

    private boolean goToBonusActivated;
    private boolean moveToLineActivated;
    private BaseLine lastFightLine;
    private BaseLine currentCalcLine;

    private Point moveToLinePoint;
    private Point[] linesFightPoints;

    public Drawing_DrawingData(Wizard self,
                               World world,
                               double[] maxCastRange,
                               CurrentAction currentAction,
                               TreeMap<Long, Double> projectilesDT,
                               EnemyPositionCalc enemyPositionCalc,
                               BonusesPossibilityCalcs bonusesPossibilityCalcs,
                               boolean goToBonusActivated,
                               boolean moveToLineActivated,
                               BaseLine lastFightLine,
                               BaseLine currentCalcLine,
                               Point moveToLinePoint,
                               Point[] linesFightPoints,
                               AgressiveNeutralsCalcs agressiveNeutralsCalcs) {
        this.self = self;
        this.world = world;
        this.maxCastRange = Arrays.copyOf(maxCastRange, maxCastRange.length);
        this.currentAction = currentAction.clone();
        this.projectilesDTL = new TreeMap<>(projectilesDT);
        this.enemyPositionCalc = enemyPositionCalc.clone();
        this.bonusesPossibilityCalcs = bonusesPossibilityCalcs.clone();
        this.goToBonusActivated = goToBonusActivated;
        this.moveToLineActivated = moveToLineActivated;
        this.lastFightLine = lastFightLine;

        this.moveToLinePoint = moveToLinePoint.clonePoint();
        this.linesFightPoints = new Point[linesFightPoints.length];
        this.currentCalcLine = currentCalcLine;
        for (int i = 0; i != linesFightPoints.length; ++i) {
            if (linesFightPoints[i] != null) {
                this.linesFightPoints[i] = linesFightPoints[i].clonePoint();
            }
        }
        this.agressiveNeutralsCalcs = agressiveNeutralsCalcs.makeClone();
    }

    public Wizard getSelf() {
        return self;
    }

    public World getWorld() {
        return world;
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

    public boolean isGoToBonusActivated() {
        return goToBonusActivated;
    }

    public boolean isMoveToLineActivated() {
        return moveToLineActivated;
    }

    public BaseLine getLastFightLine() {
        return lastFightLine;
    }

    public BaseLine getCurrentCalcLine() {
        return currentCalcLine;
    }

    public Point getMoveToLinePoint() {
        return moveToLinePoint;
    }

    public Point[] getLinesFightPoints() {
        return linesFightPoints;
    }

    public AgressiveNeutralsCalcs getAgressiveNeutralsCalcs() {
        return agressiveNeutralsCalcs;
    }

    public Drawing_DrawingData clone() {
        return new Drawing_DrawingData(self,
                                       world,
                                       maxCastRange,
                                       currentAction,
                                       projectilesDTL,
                                       enemyPositionCalc,
                                       bonusesPossibilityCalcs,
                                       goToBonusActivated,
                                       moveToLineActivated,
                                       lastFightLine,
                                       currentCalcLine,
                                       moveToLinePoint,
                                       linesFightPoints,
                                       agressiveNeutralsCalcs);
    }

    @Override
    public String toString() {
        return "Drawing_DrawingData{" +
                String.format("self=(%s, %s)", self.getX(), self.getY()) +
                ", " + String.format("world= (tick: %d)", world.getTickIndex()) +
                ", currentAction=" + currentAction +
                ", goToBonusActivated=" + goToBonusActivated +
                ", moveToLineActivated=" + moveToLineActivated +
                '}';
    }
}
