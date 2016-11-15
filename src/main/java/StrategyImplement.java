import model.ActionType;
import model.Building;
import model.BuildingType;
import model.CircularUnit;
import model.Faction;
import model.Game;
import model.LaneType;
import model.LivingUnit;
import model.Message;
import model.Minion;
import model.MinionType;
import model.Move;
import model.Tree;
import model.Wizard;
import model.World;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Created by by.dragoon on 11/8/16.
 */
public class StrategyImplement {

	protected int[] score = new int[3];

	protected World world;
	protected Wizard self;

	protected LaneType myLine;
	protected BaseLine myLineCalc;
	protected double direction;

	protected FilteredWorld filteredWorld;

	protected final ScanMatrixItem[][] scan_matrix = Utils.createScanMatrix();

	protected int lastTick;

	protected boolean enemyFound;

	protected ScanMatrixItem pointToReach;

	protected ArrayList<WayPoint> wayPoints = new ArrayList<>();

	protected Point moveToPoint;

	protected CircularUnit target;

	private PriorityQueue<WayPoint> queue = new PriorityQueue<>();

	private List<Map.Entry<Double, CircularUnit>> targets = new ArrayList<>();

	public void move(Wizard self, World world, Game game, Move move) {
		Variables.self = self;
		Utils.prepareNewStep();
		enemyFound = false;
		target = null;
		this.world = world;
		this.self = self;

		switch (world.getTickIndex()) {
			case 0:
				myLine = Utils.getDefaultMyLine((int) self.getId());
				break;
			case 1:
				for (Message message : self.getMessages()) {
					if (message.getLane() != null) {
						myLine = message.getLane();
					}
				}
				break;
		}

		if (world.getTickIndex() > lastTick + 1) { //I'm was dead
			calcLinesScore();
			int currentLine = 0;
			if (score[1] > score[currentLine]) {
				currentLine = 1;
			}
			if (score[2] > score[currentLine]) {
				currentLine = 2;
			}
			myLine = Constants.WHICH_LINE_NO[currentLine];
		}
		lastTick = world.getTickIndex();

		myLineCalc = Constants.getLine(myLine);
		direction = myLineCalc.getMoveDirection(self);


		filteredWorld = Utils.filterWorld(world,
										  new Point(self.getX() + Math.cos(direction) * Constants.MOVE_SCAN_FIGURE_CENTER,
													self.getY() + Math.sin(direction) * Constants.MOVE_SCAN_FIGURE_CENTER));
		enemyFound = Utils.hasEnemy(filteredWorld.getMinions()) ||
				Utils.hasEnemy(filteredWorld.getWizards()) ||
				Utils.hasEnemy(filteredWorld.getBuildings());

		calcMatrixDanger(game);
		findAWay();
		if (!wayPoints.isEmpty()) {
			int lastPointGoTo = 1;
			double dangerAtStart = wayPoints.get(0).getPoint().getAllDangers();
			while (lastPointGoTo < wayPoints.size() &&
					wayPoints.get(lastPointGoTo).getPoint().getAllDangers() >= dangerAtStart * Constants.DANGER_AT_START_MULT_RUN) {
				++lastPointGoTo;
			}
			if (lastPointGoTo == wayPoints.size()) {
				--lastPointGoTo;
			}
			if (testPointDirectAvailable(lastPointGoTo)) {
				moveTo(wayPoints.get(lastPointGoTo), move);
			} else {
				int whichPointGoTo = 0;
				while (whichPointGoTo + 1 < lastPointGoTo) {
					int currentPointGoTo = (lastPointGoTo + whichPointGoTo) / 2;
					if (testPointDirectAvailable(currentPointGoTo)) {
						whichPointGoTo = currentPointGoTo;
					} else {
						lastPointGoTo = currentPointGoTo;
					}
				}
				moveTo(wayPoints.get(whichPointGoTo), move);
			}
		}

		findTargets();
		shotAndTurn(move);
	}

	private void shotAndTurn(Move move) {
		if (target == null) {
			turnTo(moveToPoint, move);
			return;
		}
		double turnAngle = self.getAngleTo(target);
		double maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle();
		int turnCount = (int) ((maxTurnAngle + Math.abs(turnAngle)) / maxTurnAngle);

		if (self.getRemainingCooldownTicksByAction()[ActionType.MAGIC_MISSILE.ordinal()] > turnCount + 3) {
			if (turnTo(moveToPoint, move)) {
				return;
			}
		} else {
			if (self.getRemainingCooldownTicksByAction()[ActionType.MAGIC_MISSILE.ordinal()] == 0 &&
					Math.abs(turnAngle) <= Constants.MAX_SHOOT_ANGLE) {
				move.setCastAngle(turnAngle);
				move.setAction(ActionType.MAGIC_MISSILE);
				if (target instanceof Tree) {
					move.setMinCastDistance(self.getCastRange() - .01);
				} else {
					move.setMinCastDistance(self.getDistanceTo(target) - target.getRadius());
				}
				if (turnTo(moveToPoint, move)) { // turn to move direction to move faster
					return;
				}
			}
		}
		turnTo(turnAngle, maxTurnAngle, move);
	}

	private boolean turnTo(Point point, Move move) {
		if (point == null) {
			return false;
		}
		if (FastMath.hypot(point.getX() - self.getX(), point.getY() - self.getY()) < Constants.getGame().getWizardStrafeSpeed()) {
			point = pointToReach;
		}
		if (FastMath.hypot(point.getX() - self.getX(), point.getY() - self.getY()) < Constants.getGame().getWizardStrafeSpeed()) {
			return false;
		}
		turnTo(self.getAngleTo(point.getX(), point.getY()), Constants.getGame().getWizardMaxTurnAngle(), move);
		return true;
	}

	private void turnTo(double angle, double maxAngle, Move move) {
		if (Math.abs(angle) > maxAngle) {
			angle /= Math.abs(angle) / maxAngle;
		}
		move.setTurn(angle);
	}

	private void findTargets() {
		targets.clear();
		boolean treeCut = myLineCalc.getDistanceTo(self) > Constants.getTopLine().getLineDistance();
		for (LivingUnit livingUnit : filteredWorld.getAimsList()) {
			if (livingUnit.getFaction() != Constants.getEnemyFaction() &&
					(livingUnit.getFaction() != Faction.NEUTRAL || livingUnit.getLife() >= livingUnit.getMaxLife()) &&
					livingUnit.getFaction() != Faction.OTHER) {
				continue;
			}
			if (livingUnit instanceof Tree) {
				if (treeCut) {
					targets.add(new AbstractMap.SimpleEntry<>(-Utils.calcLineDistanceOtherDanger(livingUnit, myLineCalc) * .0001, livingUnit));
				}
				continue;
			}
			double score = Constants.LOW_AIM_SCORE;
			double tmp = (livingUnit.getMaxLife() - livingUnit.getLife()) / livingUnit.getMaxLife();
			score += tmp * tmp;
			if (livingUnit instanceof Minion) {
				if (((Minion) livingUnit).getType() == MinionType.FETISH_BLOWDART) {
					score *= Constants.FETISH_AIM_PROIRITY;
				} else {
					score *= Constants.ORC_AIM_PROIRITY;
				}
				if (livingUnit.getFaction() == Faction.NEUTRAL) {
					score *= Constants.NEUTRAL_FACTION_AIM_PROIRITY;
				}
			} else if (livingUnit instanceof Wizard) {
				score *= Constants.WIZARD_AIM_PROIRITY;
			} else if (livingUnit instanceof Building) {
				score *= Constants.BUILDING_AIM_PROIRITY;
			}
			targets.add(new AbstractMap.SimpleEntry<>(score, livingUnit));
		}
		Collections.sort(targets, Utils.AIM_SORT_COMPARATOR);
		for (Map.Entry<Double, CircularUnit> doubleCircularUnitEntry : targets) {
			target = doubleCircularUnitEntry.getValue();
			Point pointA = new Point(self.getX(), self.getY());
			Point pointB = new Point(target.getX(), target.getY());

			for (Tree tree : filteredWorld.getTrees()) {
				double distance = Utils.distancePointToSegment(new Point(tree.getX(), tree.getY()), pointA, pointB);
				if (distance < tree.getRadius() + Constants.getGame().getMagicMissileRadius()) {
					target = null;
					break;
				}
			}
			if (target != null) {
				break;
			}
		}
	}

	protected void moveTo(WayPoint wayPoint, Move move) {
		moveToPoint = wayPoint.getPoint();
		ScanMatrixItem point = wayPoint.getPoint();
		double distance = FastMath.hypot(self, point.getX(), point.getY());
		double angle = self.getAngleTo(point.getX(), point.getY());
		double strafe = Math.sin(angle) * distance;
		double acc = Math.cos(angle) * distance;
		double fwdLimit = acc > 0 ? Constants.getGame().getWizardForwardSpeed() : Constants.getGame().getWizardBackwardSpeed();
		fwdLimit = Math.abs(acc / fwdLimit);
		fwdLimit = Math.max(fwdLimit, Math.abs(strafe / Constants.getGame().getWizardStrafeSpeed()));
		if (fwdLimit > 1.) {
			strafe /= fwdLimit;
			acc /= fwdLimit;
		}
		move.setStrafeSpeed(strafe);
		move.setSpeed(acc);
	}

	private boolean testPointDirectAvailable(Point point) {
		double distance;
		Point from = new Point(self.getX(), self.getY());
		Point to = new Point(point.getX(), point.getY());
		for (CircularUnit circularUnit : filteredWorld.getAllBlocksList()) {
			distance = Utils.distancePointToSegment(new Point(circularUnit.getX(), circularUnit.getY()), from, to);
			if (distance < self.getRadius() + circularUnit.getRadius() + .001) {
				return false;
			}
		}
		return true;
	}

	private boolean testPointDirectAvailable(int point) {
		return testPointDirectAvailable(wayPoints.get(point).getPoint());
	}

	private void findAWay() {
		wayPoints.clear();
		if (!enemyFound) {
			ScanMatrixItem item = null;
			for (int i = 0; i != scan_matrix.length; ++i) {
				for (int j = 0; j != scan_matrix[0].length; ++j) {
					ScanMatrixItem scanMatrixItem = scan_matrix[i][j];
					if (!scanMatrixItem.isAvailable()) {
						continue;
					}
					if (item == null || item.getTotalScore(self) < scanMatrixItem.getTotalScore(self)) {
						item = scanMatrixItem;
					}
				}
			}
			if (testPointDirectAvailable(item)) {
				wayPoints.add(new WayPoint(0, scan_matrix[Constants.CURRENT_PT_X][Constants.CURRENT_PT_Y], null));
				wayPoints.add(new WayPoint(1, item, wayPoints.get(0)));
				return;
			}
		}
		queue.add(new WayPoint(0, scan_matrix[Constants.CURRENT_PT_X][Constants.CURRENT_PT_Y], null));
		int nextDistanceFromStart;
		double newDangerOnWay;
		double scoresOnWay;
		while (!queue.isEmpty()) {
			WayPoint currentPoint = queue.poll();
			if (currentPoint.getPoint().getWayPoint() != currentPoint) {
				continue;
			}
			nextDistanceFromStart = currentPoint.getDistanceFromStart() + 1;
			for (ScanMatrixItem scanMatrixItem : currentPoint.getPoint().getNeighbours()) {
				if (!scanMatrixItem.isAvailable()) {
					continue;
				}
				if (scanMatrixItem.getWayPoint() == null) {
					queue.add(new WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
					continue;
				}
				WayPoint wayPointToCompare = scanMatrixItem.getWayPoint();
				newDangerOnWay = scanMatrixItem.getAllDangers() + currentPoint.getDangerOnWay();

				if (newDangerOnWay == wayPointToCompare.getDangerOnWay()) {
					if (wayPointToCompare.getDistanceFromStart() == nextDistanceFromStart) {
						scoresOnWay = scanMatrixItem.getTotalScore(self) + currentPoint.getScoresOnWay();
						if (scoresOnWay < wayPointToCompare.getScoresOnWay()) {
							queue.add(new WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
						}
					} else if (nextDistanceFromStart < wayPointToCompare.getDistanceFromStart()) {
						queue.add(new WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
					}
				} else if (newDangerOnWay < wayPointToCompare.getDangerOnWay()) {
					queue.add(new WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
				}
			}
		}
		getBestMovePoint();
	}

	protected void getBestMovePoint() {
		ScanMatrixItem best = null;
		double score = 0;
		double tmpScore;

		for (int i = scan_matrix.length - 1; i != -1; --i) {
			for (int j = 0; j != scan_matrix[0].length; ++j) {
				ScanMatrixItem newScanMatrixItem = scan_matrix[i][j];
				if (newScanMatrixItem.getWayPoint() == null) {
					continue;
				}
				if (best == null) {
					best = newScanMatrixItem;
					score = newScanMatrixItem.getTotalScore(self) - newScanMatrixItem.getWayPoint().getDangerOnWay() * .02;
					continue;
				}
				tmpScore = newScanMatrixItem.getTotalScore(self) - newScanMatrixItem.getWayPoint().getDangerOnWay() * .02;
				if (tmpScore > score) {
					score = tmpScore;
					best = newScanMatrixItem;
				}
			}
		}
		pointToReach = best;
		finalizeQueue(pointToReach.getWayPoint());
	}

	private void finalizeQueue(WayPoint wayPoint) {
		do {
			wayPoints.add(wayPoint);
			wayPoint = wayPoint.getPrev();
		} while (wayPoint != null);
		Collections.reverse(wayPoints);
	}

	private void calcMatrixDanger(Game game) {
		double dxFwd = Math.cos(direction) * Constants.MOVE_SCAN_STEP;
		double dyFwd = Math.sin(direction) * Constants.MOVE_SCAN_STEP;

		double dxSide = Math.cos(direction - Math.PI * .5) * Constants.MOVE_SCAN_STEP;
		double dySide = Math.sin(direction - Math.PI * .5) * Constants.MOVE_SCAN_STEP;
		for (int i = 0; i != scan_matrix.length; ++i) {
			for (int j = 0; j != scan_matrix[0].length; ++j) {
				scan_matrix[i][j].setPoint(self.getX() + dxFwd * (i - Constants.CURRENT_PT_X) + dxSide * (Constants.CURRENT_PT_Y - j),
										   self.getY() + dyFwd * (i - Constants.CURRENT_PT_X) + dySide * (Constants.CURRENT_PT_Y - j));
			}
			Utils.calcTilesAvailable(filteredWorld.getAllBlocksList(), scan_matrix[i]);
			for (int j = 0; j != scan_matrix[0].length; ++j) {
				ScanMatrixItem item = scan_matrix[i][j];
				if (!item.isAvailable()) {
					continue;
				}

				double distanceTo = Utils.calcLineDistanceOtherDanger(item, myLineCalc);
				if (distanceTo > 0.) {
					item.addOtherDanger(distanceTo);
				}

				if (!enemyFound) {
					item.setOtherBonus((scan_matrix.length + scan_matrix[0].length - getIntDistanceFromForwardPoint(i, j)) * .0001);
				}
			}
		}

		if (!enemyFound) {
			return;
		}
		ScoreCalcStructure.ATTACK_BONUS_APPLYER.setDistance(self.getCastRange());

		ScoreCalcStructure structure = new ScoreCalcStructure();
		for (int i = 0; i != scan_matrix.length; ++i) {
			for (Minion minion : filteredWorld.getMinions()) {
				if (minion.getFaction() != Constants.getEnemyFaction() &&
						(minion.getFaction() != Faction.NEUTRAL || minion.getLife() >= minion.getMaxLife())) {
					continue;
				}
				structure.clear();
				double expBonus = ScanMatrixItem.calcExpBonus(minion.getLife(), minion.getMaxLife(), 1.);
				if (expBonus > 0.) {
					ScoreCalcStructure.EXP_BONUS_APPLYER.setScore(expBonus);
					structure.putItem(ScoreCalcStructure.EXP_BONUS_APPLYER);
				}
				switch (minion.getType()) {
					case ORC_WOODCUTTER:
						ScoreCalcStructure.MINION_DANGER_APPLYER.setScore(minion.getDamage());
						ScoreCalcStructure.MINION_DANGER_APPLYER.setDistance(game.getOrcWoodcutterAttackRange() + self.getRadius() + game.getMinionSpeed() + 1);
						structure.putItem(ScoreCalcStructure.MINION_DANGER_APPLYER);
						break;
					case FETISH_BLOWDART:
						ScoreCalcStructure.MINION_DANGER_APPLYER.setScore(game.getDartDirectDamage());
						ScoreCalcStructure.MINION_DANGER_APPLYER.setDistance(game.getFetishBlowdartAttackRange());
						structure.putItem(ScoreCalcStructure.MINION_DANGER_APPLYER);
						break;
				}
				ScoreCalcStructure.ATTACK_BONUS_APPLYER.setScore(3.);
				structure.putItem(ScoreCalcStructure.ATTACK_BONUS_APPLYER);
				applyScoreForLine(scan_matrix[i], structure, minion);
			}

			for (Wizard wizard : filteredWorld.getWizards()) {
				if (wizard.getFaction() == Constants.getCurrentFaction()) {
					continue;
				}
				structure.clear();
				double expBonus = ScanMatrixItem.calcExpBonus(wizard.getLife(), wizard.getMaxLife(), 4.);
				if (expBonus > 0.) {
					ScoreCalcStructure.EXP_BONUS_APPLYER.setScore(expBonus);
					structure.putItem(ScoreCalcStructure.EXP_BONUS_APPLYER);
				}
				ScoreCalcStructure.WIZARDS_DANGER_BONUS_APPLYER.setDistance(wizard.getCastRange() + game.getWizardForwardSpeed() * Math.min(1,
																																			-wizard.getRemainingActionCooldownTicks() + 3));
				ScoreCalcStructure.WIZARDS_DANGER_BONUS_APPLYER.setScore(12.);
				structure.putItem(ScoreCalcStructure.WIZARDS_DANGER_BONUS_APPLYER);

				ScoreCalcStructure.ATTACK_BONUS_APPLYER.setDistance(self.getCastRange());
				ScoreCalcStructure.ATTACK_BONUS_APPLYER.setScore(12.);
				structure.putItem(ScoreCalcStructure.ATTACK_BONUS_APPLYER);
				applyScoreForLine(scan_matrix[i], structure, wizard);
			}

			for (Building building : filteredWorld.getBuildings()) {
				if (building.getFaction() == Constants.getCurrentFaction()) {
					continue;
				}
				structure.clear();
				double expBonus = ScanMatrixItem.calcExpBonus(building.getLife(), building.getMaxLife(), 1.);
				if (expBonus > 0.) {
					ScoreCalcStructure.EXP_BONUS_APPLYER.setScore(expBonus);
					structure.putItem(ScoreCalcStructure.EXP_BONUS_APPLYER);
				}

				ScoreCalcStructure.ATTACK_BONUS_APPLYER.setScore(12.);
				ScoreCalcStructure.ATTACK_BONUS_APPLYER.setDistance(self.getCastRange() + building.getRadius());
				structure.putItem(ScoreCalcStructure.ATTACK_BONUS_APPLYER);

				ScoreCalcStructure.BUILDING_DANGER_BONUS_APPLYER.setScore(building.getDamage());
				ScoreCalcStructure.BUILDING_DANGER_BONUS_APPLYER
						.setDistance(building.getAttackRange() - Math.max(building.getRemainingActionCooldownTicks() - 5, 0) * 1.5);
				structure.putItem(ScoreCalcStructure.BUILDING_DANGER_BONUS_APPLYER);

				applyScoreForLine(scan_matrix[i], structure, building);
			}
		}
	}

	public void applyScoreForLine(ScanMatrixItem[] items, ScoreCalcStructure structure, CircularUnit unit) {
		double distance = Utils.distancePointToSegment(new Point(unit.getX(), unit.getY()), items[0], items[items.length - 1]);
		if (distance > structure.getMaxScoreDistance()) {
			return;
		}
		double itemDistance;
		for (int i = 0; i != items.length; ++i) {
			if (!items[i].isAvailable()) {
				continue;
			}
			itemDistance = FastMath.hypot(items[i].getX() - unit.getX(), items[i].getY() - unit.getY());
			if (itemDistance <= structure.getMaxScoreDistance()) {
				structure.applyScores(items[i], itemDistance);
			}
		}
	}

	public int getIntDistanceFromForwardPoint(int x, int y) {
		return scan_matrix.length - 1 - x + Math.abs(y - Constants.CURRENT_PT_Y);
	}

	private void calcLinesScore() {
		Arrays.fill(score, 0);

		for (Minion minion : world.getMinions()) {
			int line = Utils.whichLine(minion);
			if (minion.getFaction() == Constants.getCurrentFaction()) {
				score[line] -= Constants.minionLineScore;
			} else if (minion.getFaction() == Constants.getEnemyFaction()) {
				score[line] += Constants.minionLineScore;
			}
		}

		for (Wizard wizard : world.getWizards()) {
			if (wizard.getId() == self.getId()) {
				continue;
			}
			int line = Utils.whichLine(wizard);
			if (wizard.getFaction() == Constants.getCurrentFaction()) {
				score[line] -= Constants.wizardLineScore;
			} else if (wizard.getFaction() == Constants.getEnemyFaction()) {
				score[line] += Constants.wizardLineScore;
			}
		}

		// towers get only negative score
		for (Building building : world.getBuildings()) {
			if (building.getType() == BuildingType.FACTION_BASE ||
					building.getFaction() != Constants.getCurrentFaction()) {
				continue;
			}
			int line = Utils.whichLine(building);
			score[line] -= Constants.towerLineScore;
		}
	}
}
