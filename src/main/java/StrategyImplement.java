import model.ActionType;
import model.Building;
import model.BuildingType;
import model.CircularUnit;
import model.Faction;
import model.Game;
import model.LaneType;
import model.LivingUnit;
import model.Minion;
import model.MinionType;
import model.Move;
import model.StatusType;
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
	protected CircularUnit meleeTarget;

	private PriorityQueue<WayPoint> queue = new PriorityQueue<>();

	private List<Map.Entry<Double, CircularUnit>> targets = new ArrayList<>();

	protected BuildingPhantom[] BUILDING_PHANTOMS = new BuildingPhantom[0];

	public void move(Wizard self, World world, Game game, Move move) {
		Variables.self = self;
		enemyFound = false;
		target = null;
		this.world = world;
		this.self = self;

		switch (world.getTickIndex()) {
			case 0:
				myLine = Utils.getDefaultMyLine((int) self.getId());
				int phantomIdx = 0;
				BUILDING_PHANTOMS = new BuildingPhantom[14];
				for (Building building : world.getBuildings()) {
					BUILDING_PHANTOMS[phantomIdx++] = new BuildingPhantom(building, false);
					BUILDING_PHANTOMS[phantomIdx++] = new BuildingPhantom(building, true);
				}
				break;
		}

		BUILDING_PHANTOMS = Utils.updateBuildingPhantoms(world, BUILDING_PHANTOMS);

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
													self.getY() + Math.sin(direction) * Constants.MOVE_SCAN_FIGURE_CENTER),
										  BUILDING_PHANTOMS);

		Utils.calcCurrentSkillBonuses(self, filteredWorld);
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
				if (whichPointGoTo == 0 && wayPoints.size() > 1) {
					whichPointGoTo = 1;
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

		double maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle() * Variables.turnFactor;
		int turnTicksCount = getTurnCount(turnAngle, maxTurnAngle, Constants.MAX_SHOOT_ANGLE);

		int hastenedTicksRemain = Utils.wizardStatusTicks(self, StatusType.HASTENED);
		if (hastenedTicksRemain > -1 && turnTicksCount > hastenedTicksRemain) {
			maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle();
			turnTicksCount = getTurnCount(turnAngle, maxTurnAngle, Constants.MAX_SHOOT_ANGLE);
		}

		//если уже надо бы поворачиваться дла атаки
		if (waitTimeForAction(ActionType.MAGIC_MISSILE) <= turnTicksCount + 2) {
			// если уже можем попасть - атакуем и бежим дальше
			if (checkShot(turnAngle, target, move)) {
				turnTo(moveToPoint, move);
				return;
			}
			// если не можем попасть - доворачиваем на цель
			turnTo(turnAngle, maxTurnAngle, move);
			return;
		}
		if (meleeTarget != null) {

			maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle() * Variables.turnFactor;
			int meleeTurnTicksCount = getTurnCount(turnAngle, maxTurnAngle, Constants.getStaffHitSector());

			if (hastenedTicksRemain > -1 && turnTicksCount > hastenedTicksRemain) {
				maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle();
				meleeTurnTicksCount = getTurnCount(turnAngle, maxTurnAngle, Constants.getStaffHitSector());
			}
			// милишная цель есть
			double meleeTurnAngle = self.getAngleTo(meleeTarget);

			//если уже надо бы поворачиваться дла атаки
			if (waitTimeForAction(ActionType.STAFF) <= meleeTurnTicksCount + 2) {
				// если уже можем попасть - атакуем и бежим дальше
				if (checkHit(meleeTurnAngle, meleeTarget, move)) {
					turnTo(moveToPoint, move);
					return;
				}
				// если не можем попасть - доворачиваем на цель
				turnTo(meleeTurnAngle, maxTurnAngle, move);
				return;
			}
		}
		// целей нет или же мы успеем к ним повернуться
		turnTo(moveToPoint, move);
	}

	private int waitTimeForAction(ActionType actionType) {
		return Math.max(self.getRemainingCooldownTicksByAction()[actionType.ordinal()], self.getRemainingActionCooldownTicks());
	}

	protected boolean checkHit(double angle, CircularUnit target, Move move) {
		if (Math.abs(angle) > Constants.getStaffHitSector()) {
			return false;
		}
		if (FastMath.hypot(self.getX() - target.getX(), self.getY() - target.getY()) < target.getRadius() + Constants.getGame().getStaffRange()
				&& waitTimeForAction(ActionType.STAFF) == 0) {
			move.setAction(ActionType.STAFF);
			return true;
		}
		return false;
	}

	private boolean checkShot(double angle, CircularUnit target, Move move) {
		if (checkHit(angle, target, move)) {
			return true;
		}
		if (Math.abs(angle) > Constants.MAX_SHOOT_ANGLE) {
			return false;
		}
		if (FastMath.hypot(self.getX() - target.getX(), self.getY() - target.getY()) < target.getRadius() + self.getCastRange()
				&& waitTimeForAction(ActionType.MAGIC_MISSILE) == 0) {
			move.setCastAngle(angle);
			move.setAction(ActionType.MAGIC_MISSILE);
			if (target instanceof Tree) {
				move.setMinCastDistance(self.getCastRange() - .01);
			} else {
				move.setMinCastDistance(self.getDistanceTo(target) - target.getRadius());
			}
			return true;
		}
		return false;
	}

	private int getTurnCount(double currentAngle, double maxTurnAngle, double maxAttackAngle) {
		if (Math.abs(currentAngle) < maxAttackAngle) {
			currentAngle = 0.;
		} else {
			if (currentAngle < 0.) {
				currentAngle += maxAttackAngle;
			} else {
				currentAngle -= maxAttackAngle;
			}
		}

		return (int) ((maxTurnAngle - .001 + Math.abs(currentAngle)) / maxTurnAngle);
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
		turnTo(self.getAngleTo(point.getX(), point.getY()), Constants.getGame().getWizardMaxTurnAngle() * Variables.turnFactor, move);
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
		boolean treeCut = myLineCalc.getDistanceTo(self) > (Constants.getTopLine().getLineDistance() * 1.5);
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
			double tmp = (livingUnit.getMaxLife() - livingUnit.getLife()) / (double) livingUnit.getMaxLife();
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
				if (Utils.wizardHasStatus((Wizard) livingUnit, StatusType.SHIELDED)) {
					score *= Constants.SHIELDENED_AIM_PRIORITY;
				}
				if (Utils.wizardHasStatus((Wizard) livingUnit, StatusType.EMPOWERED)) {
					score *= Constants.EMPOWERED_AIM_PRIORITY;
				}
				if (Utils.wizardHasStatus((Wizard) livingUnit, StatusType.HASTENED)) {
					score *= Constants.HASTENED_AIM_PRIORITY;
				}
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
				if (tree == target) {
					continue;
				}
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

		double distance;
		for (Map.Entry<Double, CircularUnit> doubleCircularUnitEntry : targets) {
			meleeTarget = doubleCircularUnitEntry.getValue();
			distance = FastMath.hypot(self.getX() - meleeTarget.getX(), self.getY() - meleeTarget.getY());
			if (distance + .001 < meleeTarget.getRadius() + Constants.getGame().getStaffRange()) {
				break;
			}
			meleeTarget = null;
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
		fwdLimit *= Variables.moveFactor;

		double fix = Math.hypot(acc / fwdLimit, strafe / Constants.getGame().getWizardStrafeSpeed() * Variables.moveFactor);
		if (fix > 1.) {
			move.setStrafeSpeed(strafe / fix);
			move.setSpeed(acc / fix);
		} else {
			move.setStrafeSpeed(strafe);
			move.setSpeed(acc);
		}
	}

	private boolean testPointDirectAvailable(Point point) {
		double distance;
		Point from = new Point(self.getX(), self.getY());
		Point to = new Point(point.getX(), point.getY());
		for (CircularUnit circularUnit : filteredWorld.getAllBlocksList()) {
			distance = Utils.distancePointToSegment(new Point(circularUnit.getX(), circularUnit.getY()), from, to);
			if (distance < self.getRadius() + circularUnit.getRadius() + Constants.STUCK_FIX_RADIUS_ADD) {
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
		double myDamage = 12.;
		if (Utils.wizardHasStatus(self, StatusType.EMPOWERED)) {
			myDamage *= 2;
		}
		double shieldBonus = Utils.wizardHasStatus(self, StatusType.SHIELDED) ? Constants.getGame().getShieldedDirectDamageAbsorptionFactor() : 1.;
		ScoreCalcStructure structure = new ScoreCalcStructure();

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
					ScoreCalcStructure.MINION_DANGER_APPLYER.setScore(minion.getDamage() * shieldBonus);
					ScoreCalcStructure.MINION_DANGER_APPLYER.setDistance(game.getOrcWoodcutterAttackRange() + self.getRadius() + game.getMinionSpeed() + 1);
					structure.putItem(ScoreCalcStructure.MINION_DANGER_APPLYER);
					break;
				case FETISH_BLOWDART:
					ScoreCalcStructure.MINION_DANGER_APPLYER.setScore(game.getDartDirectDamage() * shieldBonus);
					ScoreCalcStructure.MINION_DANGER_APPLYER.setDistance(game.getFetishBlowdartAttackRange() + game.getMinionSpeed());
					structure.putItem(ScoreCalcStructure.MINION_DANGER_APPLYER);
					break;
			}
			ScoreCalcStructure.ATTACK_BONUS_APPLYER.setScore(myDamage * Constants.MINION_ATTACK_FACTOR);
			ScoreCalcStructure.ATTACK_BONUS_APPLYER.setDistance(self.getCastRange());
			structure.putItem(ScoreCalcStructure.ATTACK_BONUS_APPLYER);

			ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER.setScore(myDamage * Constants.MINION_ATTACK_FACTOR);
			ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER.setDistance(Constants.getGame().getStaffRange() + minion.getRadius());
			structure.putItem(ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER);

			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, minion);
			}
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
			double wizardDamage = 12.;
			if (Utils.wizardHasStatus(wizard, StatusType.EMPOWERED)) {
				wizardDamage *= 2;
			}
			if (self.getLife() < self.getMaxLife() * Constants.ENEMY_WIZARD_ATTACK_LIFE) {
				ScoreCalcStructure.WIZARDS_DANGER_BONUS_APPLYER.setDistance(wizard.getCastRange() + self.getRadius() + game.getWizardForwardSpeed() * 2);
				ScoreCalcStructure.WIZARDS_DANGER_BONUS_APPLYER.setScore(wizardDamage * 3. * shieldBonus);
			} else {
				ScoreCalcStructure.WIZARDS_DANGER_BONUS_APPLYER.setDistance(
						wizard.getCastRange() +
								game.getWizardForwardSpeed() * Math.min(2, -wizard.getRemainingActionCooldownTicks() + 4));
				ScoreCalcStructure.WIZARDS_DANGER_BONUS_APPLYER.setScore(wizardDamage * shieldBonus);
			}

			structure.putItem(ScoreCalcStructure.WIZARDS_DANGER_BONUS_APPLYER);

			ScoreCalcStructure.ATTACK_BONUS_APPLYER.setDistance(self.getCastRange());
			ScoreCalcStructure.ATTACK_BONUS_APPLYER.setScore(myDamage);
			structure.putItem(ScoreCalcStructure.ATTACK_BONUS_APPLYER);

			ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER.setScore(myDamage);
			ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER.setDistance(Constants.getGame().getStaffRange() + wizard.getRadius());
			structure.putItem(ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER);

			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, wizard);
			}
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

			ScoreCalcStructure.ATTACK_BONUS_APPLYER.setScore(myDamage);
			ScoreCalcStructure.ATTACK_BONUS_APPLYER.setDistance(self.getCastRange() + building.getRadius());
			structure.putItem(ScoreCalcStructure.ATTACK_BONUS_APPLYER);

			ScoreCalcStructure.BUILDING_DANGER_BONUS_APPLYER.setScore(building.getDamage() * shieldBonus);
			ScoreCalcStructure.BUILDING_DANGER_BONUS_APPLYER
					.setDistance(building.getAttackRange() + Math.min(2, -building.getRemainingActionCooldownTicks() + 4) * 1.5);
			structure.putItem(ScoreCalcStructure.BUILDING_DANGER_BONUS_APPLYER);

			ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER.setScore(myDamage);
			ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER.setDistance(Constants.getGame().getStaffRange() + building.getRadius());
			structure.putItem(ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER);

			for (int i = 0; i != scan_matrix.length; ++i) {
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
