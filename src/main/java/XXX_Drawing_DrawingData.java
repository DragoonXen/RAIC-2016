import model.Wizard;
import model.World;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dragoon on 11/8/16.
 */
public class XXX_Drawing_DrawingData {

	private Wizard self;
	private World world;
	private double[] maxCastRange;
	private XXX_CurrentAction currentAction;
	private HashMap<Long, Double> projectilesDTL;
	private XXX_EnemyPositionCalc enemyPositionCalc;
	private XXX_BonusesPossibilityCalcs bonusesPossibilityCalcs;
	private XXX_AgressiveNeutralsCalcs agressiveNeutralsCalcs;

	private boolean goToBonusActivated;
	private boolean moveToLineActivated;
	private XXX_BaseLine lastFightLine;
	private XXX_BaseLine currentCalcLine;

	private XXX_Point moveToLinePoint;
	private XXX_Point[] linesFightPoints;

	public XXX_Drawing_DrawingData(Wizard self,
								   World world,
								   double[] maxCastRange,
								   XXX_CurrentAction currentAction,
								   HashMap<Long, Double> projectilesDT,
								   XXX_EnemyPositionCalc enemyPositionCalc,
								   XXX_BonusesPossibilityCalcs bonusesPossibilityCalcs,
								   boolean goToBonusActivated,
								   boolean moveToLineActivated,
								   XXX_BaseLine lastFightLine,
								   XXX_BaseLine currentCalcLine,
								   XXX_Point moveToLinePoint,
								   XXX_Point[] linesFightPoints,
								   XXX_AgressiveNeutralsCalcs agressiveNeutralsCalcs) {
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
		this.linesFightPoints = new XXX_Point[linesFightPoints.length];
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

	public XXX_CurrentAction getCurrentAction() {
		return currentAction;
	}

	public HashMap<Long, Double> getProjectilesDTL() {
		return projectilesDTL;
	}

	public XXX_EnemyPositionCalc getEnemyPositionCalc() {
		return enemyPositionCalc;
	}

	public XXX_BonusesPossibilityCalcs getBonusesPossibilityCalcs() {
		return bonusesPossibilityCalcs;
	}

	public boolean isGoToBonusActivated() {
		return goToBonusActivated;
	}

	public boolean isMoveToLineActivated() {
		return moveToLineActivated;
	}

	public XXX_BaseLine getLastFightLine() {
		return lastFightLine;
	}

	public XXX_BaseLine getCurrentCalcLine() {
		return currentCalcLine;
	}

	public XXX_Point getMoveToLinePoint() {
		return moveToLinePoint;
	}

	public XXX_Point[] getLinesFightPoints() {
		return linesFightPoints;
	}

	public XXX_AgressiveNeutralsCalcs getAgressiveNeutralsCalcs() {
		return agressiveNeutralsCalcs;
	}

	public XXX_Drawing_DrawingData clone() {
		return new XXX_Drawing_DrawingData(self,
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
		return "XXX_Drawing_DrawingData{" +
				String.format("self=(%s, %s)", self.getX(), self.getY()) +
				", " + String.format("world= (tick: %d)", world.getTickIndex()) +
				", currentAction=" + currentAction +
				", goToBonusActivated=" + goToBonusActivated +
				", moveToLineActivated=" + moveToLineActivated +
				'}';
	}
}
