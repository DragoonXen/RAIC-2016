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
				move.setMinCastDistance(self.getDistanceTo(target) - target.getRadius());
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
		for (LivingUnit livingUnit : filteredWorld.getAimsList()) {
			if (livingUnit.getFaction() != Constants.getEnemyFaction() &&
					(livingUnit.getFaction() != Faction.NEUTRAL || livingUnit.getLife() >= livingUnit.getMaxLife())) {
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
			} else {
				score *= 0.; // it's a tree
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

	private boolean testPointDirectAvailable(int point) {
		double distance;
		Point from = new Point(self.getX(), self.getY());
		ScanMatrixItem wayPoint = wayPoints.get(point).getPoint();
		Point to = new Point(wayPoint.getX(), wayPoint.getY());
		for (CircularUnit circularUnit : filteredWorld.getAllBlocksList()) {
			distance = Utils.distancePointToSegment(new Point(circularUnit.getX(), circularUnit.getY()), from, to);
			if (distance < self.getRadius() + circularUnit.getRadius() + .001) {
				return false;
			}
		}
		return true;
	}

	private void findAWay() {
		wayPoints.clear();
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

		for (int i = 0; i != scan_matrix.length; ++i) {
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
				ScanMatrixItem item = scan_matrix[i][j];
				item.setPoint(self.getX() + dxFwd * (i - Constants.CURRENT_PT_X) + dxSide * (Constants.CURRENT_PT_Y - j),
							  self.getY() + dyFwd * (i - Constants.CURRENT_PT_X) + dySide * (Constants.CURRENT_PT_Y - j));

				boolean available = Utils.isAvailableTile(filteredWorld.getAllBlocksList(), item.getX(), item.getY());
				item.setAvailable(available);
				if (!available) {
					continue;
				}

				double distanceTo = myLineCalc.getDistanceTo(item.getX(), item.getY()) -
						Constants.getTopLine().getLineDistance();
				if (distanceTo > 0.) {
					distanceTo /= Constants.getTopLine().getLineDistance();
					item.addOtherDanger(distanceTo * distanceTo * distanceTo);
				}

				if (!enemyFound) {
					item.setOtherBonus((scan_matrix.length + scan_matrix[0].length - getIntDistanceFromForwardPoint(i, j)) * .0001);
					continue;
				}

				for (Minion minion : filteredWorld.getMinions()) {
					if (minion.getFaction() != Constants.getEnemyFaction() &&
							(minion.getFaction() != Faction.NEUTRAL || minion.getLife() >= minion.getMaxLife())) {
						continue;
					}
					double distance = FastMath.hypot(minion, item.getX(), item.getY());
					if (distance < Constants.EXPERIENCE_DISTANCE) {
						item.addExpBonus(minion.getLife(), minion.getMaxLife());
						switch (minion.getType()) {
							case ORC_WOODCUTTER:
								if (distance < game.getOrcWoodcutterAttackRange() + self.getRadius() + game.getMinionSpeed() + 1.) {
									item.addMinionsDanger(minion.getDamage());
								}
								break;
							case FETISH_BLOWDART:
								if (distance < game.getFetishBlowdartAttackRange() + self.getRadius()) {
									item.addMinionsDanger(game.getDartDirectDamage());
								}
								break;
						}
						if (distance < self.getCastRange()) {
							item.putAttackBonus(3.);
						}
					}
				}

				for (Wizard wizard : filteredWorld.getWizards()) {
					if (wizard.getFaction() == Constants.getCurrentFaction()) {
						continue;
					}
					double distance = FastMath.hypot(wizard, item.getX(), item.getY());
					if (distance + game.getWizardForwardSpeed() < Constants.EXPERIENCE_DISTANCE) {
						item.addExpBonus(wizard.getLife(), wizard.getMaxLife(), 4.);
						if (distance < wizard.getCastRange() + self.getRadius()) {
							item.addWizardsDanger(12.);
						}
						if (distance < self.getCastRange()) {
							item.putAttackBonus(12.);
						}
					}
				}

				for (Building building : filteredWorld.getBuildings()) {
					if (building.getFaction() == Constants.getCurrentFaction()) {
						continue;
					}
					double distance = FastMath.hypot(building, item.getX(), item.getY());

					if (distance < Constants.EXPERIENCE_DISTANCE) {
						item.addExpBonus(building.getLife(), building.getMaxLife());
					}
					if (distance < self.getCastRange() + building.getRadius()) {
						item.putAttackBonus(12.);
					}
					if (distance < building.getAttackRange() - Math.max(building.getRemainingActionCooldownTicks() - 5, 0) * 1.5) {
						item.addBuildingsDanger(building.getDamage());
					}
				}
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
