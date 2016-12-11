import model.Wizard;
import model.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dragoon on 11/8/16.
 */
public class YYY_Drawing_DrawingData {

	private Wizard self;
	private World world;
	private YYY_CurrentAction currentAction;
	private HashMap<Long, Double> projectilesDTL;
	private YYY_EnemyPositionCalc enemyPositionCalc;
	private YYY_BonusesPossibilityCalcs bonusesPossibilityCalcs;
	private YYY_AgressiveNeutralsCalcs agressiveNeutralsCalcs;
	private YYY_WizardsInfo wizardsInfo;

	private boolean goToBonusActivated;
	private boolean moveToLineActivated;
	private YYY_BaseLine lastFightLine;
	private YYY_BaseLine currentCalcLine;
	private YYY_Point prevPointToReach;
	private YYY_TargetFinder targetFinder;

	private YYY_TeammateIdsContainer teammateIdsContainer;

	private YYY_Point moveToLinePoint;
	private YYY_Point[] linesFightPoints;

	private int stuck;
	private YYY_Point prevPoint;

	public YYY_Drawing_DrawingData(Wizard self,
								   World world,
								   YYY_CurrentAction currentAction,
								   HashMap<Long, Double> projectilesDT,
								   YYY_EnemyPositionCalc enemyPositionCalc,
								   YYY_BonusesPossibilityCalcs bonusesPossibilityCalcs,
								   boolean goToBonusActivated,
								   boolean moveToLineActivated,
								   YYY_BaseLine lastFightLine,
								   YYY_BaseLine currentCalcLine,
								   YYY_Point moveToLinePoint,
								   YYY_Point[] linesFightPoints,
								   YYY_AgressiveNeutralsCalcs agressiveNeutralsCalcs,
								   YYY_TeammateIdsContainer teammateIdsContainer,
								   YYY_WizardsInfo wizardsInfo,
								   YYY_TargetFinder targetFinder,
								   YYY_Point prevPointToReach,
								   int stuck,
								   YYY_Point prevPoint) {
		this.self = self;
		this.world = world;
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
		this.linesFightPoints = new YYY_Point[linesFightPoints.length];
		for (int i = 0; i != linesFightPoints.length; ++i) {
			if (linesFightPoints[i] != null) {
				this.linesFightPoints[i] = linesFightPoints[i].clonePoint();
			}
		}
		this.agressiveNeutralsCalcs = agressiveNeutralsCalcs.makeClone();

		this.teammateIdsContainer = teammateIdsContainer.makeClone();
		this.wizardsInfo = wizardsInfo.makeClone();
		this.targetFinder = targetFinder.makeClone();
		this.prevPointToReach = prevPointToReach == null ? null : prevPointToReach.clonePoint();
		this.stuck = stuck;
		this.prevPoint = prevPoint.clonePoint();
	}

	public Wizard getSelf() {
		return self;
	}

	public World getWorld() {
		return world;
	}

	public YYY_CurrentAction getCurrentAction() {
		return currentAction;
	}

	public HashMap<Long, Double> getProjectilesDTL() {
		return projectilesDTL;
	}

	public YYY_EnemyPositionCalc getEnemyPositionCalc() {
		return enemyPositionCalc;
	}

	public YYY_BonusesPossibilityCalcs getBonusesPossibilityCalcs() {
		return bonusesPossibilityCalcs;
	}

	public boolean isGoToBonusActivated() {
		return goToBonusActivated;
	}

	public boolean isMoveToLineActivated() {
		return moveToLineActivated;
	}

	public YYY_BaseLine getLastFightLine() {
		return lastFightLine;
	}

	public YYY_BaseLine getCurrentCalcLine() {
		return currentCalcLine;
	}

	public YYY_Point getMoveToLinePoint() {
		return moveToLinePoint;
	}

	public YYY_Point[] getLinesFightPoints() {
		return linesFightPoints;
	}

	public YYY_AgressiveNeutralsCalcs getAgressiveNeutralsCalcs() {
		return agressiveNeutralsCalcs;
	}

	public YYY_TeammateIdsContainer getTeammateIdsContainer() {
		return teammateIdsContainer;
	}

	public YYY_TargetFinder getTargetFinder() {
		return targetFinder;
	}

	public YYY_Point getPrevPointToReach() {
		return prevPointToReach;
	}

	public int getStuck() {
		return stuck;
	}

	public YYY_Point getPrevPoint() {
		return prevPoint;
	}

	public YYY_Drawing_DrawingData clone() {
		return new YYY_Drawing_DrawingData(self,
										   world,
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
										   teammateIdsContainer,
										   wizardsInfo,
										   targetFinder,
										   prevPointToReach,
										   stuck,
										   prevPoint);
	}

	public YYY_WizardsInfo getWizardsInfo() {
		return wizardsInfo;
	}

	@Override
	public String toString() {
		return "YYY_Drawing_DrawingData{" +
				String.format("self=(%s, %s)", self.getX(), self.getY()) +
				", " + String.format("world= (tick: %d)", world.getTickIndex()) +
				", currentAction=" + currentAction +
				", goToBonusActivated=" + goToBonusActivated +
				", moveToLineActivated=" + moveToLineActivated +
				'}';
	}
}
