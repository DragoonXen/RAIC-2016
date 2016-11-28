import model.CircularUnit;
import model.Wizard;
import model.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dragoon on 11/8/16.
 */
public class Drawing_DrawingData {

    private Wizard self;
    private World world;
    private double[] maxCastRange;
    private CurrentAction currentAction;
	private HashMap<Long, Double> projectilesDTL;
	private EnemyPositionCalc enemyPositionCalc;
    private BonusesPossibilityCalcs bonusesPossibilityCalcs;
    private AgressiveNeutralsCalcs agressiveNeutralsCalcs;

    private boolean goToBonusActivated;
    private boolean moveToLineActivated;
    private BaseLine lastFightLine;
    private BaseLine currentCalcLine;
	private List<Pair<Double, CircularUnit>> missileTargets;
	private List<Pair<Double, CircularUnit>> staffTargets;
	private List<Pair<Double, CircularUnit>> iceTargets;

	private TeammateIdsContainer teammateIdsContainer;

	private Point moveToLinePoint;
	private Point[] linesFightPoints;

	public Drawing_DrawingData(Wizard self,
							   World world,
							   double[] maxCastRange,
							   CurrentAction currentAction,
							   HashMap<Long, Double> projectilesDT,
							   EnemyPositionCalc enemyPositionCalc,
							   BonusesPossibilityCalcs bonusesPossibilityCalcs,
							   boolean goToBonusActivated,
							   boolean moveToLineActivated,
							   BaseLine lastFightLine,
							   BaseLine currentCalcLine,
							   Point moveToLinePoint,
							   Point[] linesFightPoints,
							   AgressiveNeutralsCalcs agressiveNeutralsCalcs,
							   List<Pair<Double, CircularUnit>> missileTargets,
							   List<Pair<Double, CircularUnit>> staffTargets,
							   List<Pair<Double, CircularUnit>> iceTargets,
							   TeammateIdsContainer teammateIdsContainer) {
		this.self = self;
		this.world = world;
        this.maxCastRange = Arrays.copyOf(maxCastRange, maxCastRange.length);
        this.currentAction = currentAction.clone();
		this.projectilesDTL = new HashMap<>();
		for (Map.Entry<Long, Double> longDoubleEntry : projectilesDT.entrySet()) {
			this.projectilesDTL.put(longDoubleEntry.getKey(), longDoubleEntry.getValue());
		}
		this.enemyPositionCalc = enemyPositionCalc.clone();
        this.bonusesPossibilityCalcs = bonusesPossibilityCalcs.clone();
        this.goToBonusActivated = goToBonusActivated;
        this.moveToLineActivated = moveToLineActivated;
        this.lastFightLine = lastFightLine;

        this.moveToLinePoint = moveToLinePoint.clonePoint();
        this.currentCalcLine = currentCalcLine;
		this.linesFightPoints = new Point[linesFightPoints.length];
		for (int i = 0; i != linesFightPoints.length; ++i) {
            if (linesFightPoints[i] != null) {
                this.linesFightPoints[i] = linesFightPoints[i].clonePoint();
            }
        }
        this.agressiveNeutralsCalcs = agressiveNeutralsCalcs.makeClone();

		this.iceTargets = new ArrayList<>(iceTargets.size());
		for (Pair<Double, CircularUnit> iceTarget : iceTargets) {
			this.iceTargets.add(new Pair<>(iceTarget));
		}
		this.staffTargets = new ArrayList<>(staffTargets.size());
		for (Pair<Double, CircularUnit> iceTarget : staffTargets) {
			this.staffTargets.add(new Pair<>(iceTarget));
		}
		this.missileTargets = new ArrayList<>(missileTargets.size());
		for (Pair<Double, CircularUnit> iceTarget : missileTargets) {
			this.missileTargets.add(new Pair<>(iceTarget));
		}
		this.teammateIdsContainer = teammateIdsContainer.makeClone();
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

	public HashMap<Long, Double> getProjectilesDTL() {
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

	public TeammateIdsContainer getTeammateIdsContainer() {
		return teammateIdsContainer;
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
									   agressiveNeutralsCalcs,
									   missileTargets,
									   staffTargets,
									   iceTargets,
									   teammateIdsContainer);
	}

	public List<Pair<Double, CircularUnit>> getMissileTargets() {
		return missileTargets;
	}

	public List<Pair<Double, CircularUnit>> getStaffTargets() {
		return staffTargets;
	}

	public List<Pair<Double, CircularUnit>> getIceTargets() {
		return iceTargets;
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
