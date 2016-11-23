import model.ActionType;
import model.Bonus;
import model.Building;
import model.CircularUnit;
import model.Faction;
import model.Game;
import model.LivingUnit;
import model.Minion;
import model.MinionType;
import model.Move;
import model.Projectile;
import model.ProjectileType;
import model.StatusType;
import model.Tree;
import model.Wizard;
import model.World;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Created by by.dragoon on 11/8/16.
 */
public class StrategyImplement {

	protected World world;
	protected Wizard self;

	protected BaseLine myLineCalc;
	protected BaseLine lastFightLine;
	protected double direction;

	protected FilteredWorld filteredWorld;

	protected final ScanMatrixItem[][] scan_matrix = Utils.createScanMatrix();

	protected int lastTick;

	protected boolean enemyFound;

	protected ScanMatrixItem pointToReach;

	private ScanMatrixItem testScanItem = new ScanMatrixItem(0, 0, 1.);

	protected ArrayList<WayPoint> wayPoints = new ArrayList<>();

	protected Point moveToPoint;

	protected CircularUnit target;
	protected CircularUnit meleeTarget;

	private PriorityQueue<WayPoint> queue = new PriorityQueue<>();

	private List<Map.Entry<Double, CircularUnit>> targets = new ArrayList<>();

	protected double minAngle = 0.;
	protected double maxAngle = 0.;
	protected double angle = 0.;
	protected double targetAngle = 0.;

	protected TreeMap<Long, Double> projectilesDTL = new TreeMap<>(); //store
	protected CurrentAction currentAction = new CurrentAction();
	protected double[] castRange = new double[]{500., 500., 500., 500., 500., 500., 500., 500., 500., 500., 500.};

	protected EnemyPositionCalc enemyPositionCalc = new EnemyPositionCalc();

	protected BonusesPossibilityCalcs bonusesPossibilityCalcs = new BonusesPossibilityCalcs();

	protected boolean treeCut;
	protected boolean goToBonusActivated = false;
	protected boolean moveToLineActivated = false;

	public StrategyImplement(Wizard self) {
		myLineCalc = Constants.getLine(Utils.getDefaultMyLine((int) self.getId()));
		lastFightLine = myLineCalc;
	}

	public void move(Wizard self, World world, Game game, Move move) {
		enemyPositionCalc.updatePositions(world);
		bonusesPossibilityCalcs.updateTick(world, enemyPositionCalc);
		enemyFound = false;
		treeCut = false;
		moveToPoint = null;
		target = null;
		minAngle = 0.;
		wayPoints.clear();
		maxAngle = 0.;
		angle = 0.;
		targetAngle = 0.;

		Variables.self = self;
		this.world = world;
		this.self = self;
		SpawnPoint.updateTick(world.getTickIndex());
		updateCastRange(world.getWizards());

		for (BaseLine baseLine : Constants.getLines()) {
			baseLine.updateFightPoint(world, enemyPositionCalc);
		}

		myLineCalc = Utils.fightLineSelect(lastFightLine, world, enemyPositionCalc, self);
		lastFightLine = myLineCalc;

		lastTick = world.getTickIndex();

		direction = myLineCalc.getMoveDirection(self);

		filteredWorld = Utils.filterWorld(world,
										  new Point(self.getX() + Math.cos(direction) * Constants.MOVE_SCAN_FIGURE_CENTER,
													self.getY() + Math.sin(direction) * Constants.MOVE_SCAN_FIGURE_CENTER),
										  enemyPositionCalc.getBuildingPhantoms());
		updateProjectilesDTL(filteredWorld.getProjectiles());

		Utils.calcCurrentSkillBonuses(self, filteredWorld);
		currentAction.setActionType(CurrentAction.ActionType.FIGHT); // default state
		evade(move, checkHitByProjectilePossible());

		if (currentAction.getActionType() == CurrentAction.ActionType.FIGHT) {
			int ticksToBonusSpawn = Utils.getTicksToBonusSpawn(world.getTickIndex());
			if (goToBonusActivated) {
				if (PositionMoveLine.INSTANCE.getPositionToMove().getX() > 2000) {
					if ((bonusesPossibilityCalcs.getScore()[1] < .1 && ticksToBonusSpawn > Constants.MAX_TICKS_RUN_TO_BONUS) ||
							!isMeNearestWizard(BonusesPossibilityCalcs.BONUSES_POINTS[1], false)) {
						goToBonusActivated = false;
						moveToLineActivated = true;
					}
				} else {
					if ((bonusesPossibilityCalcs.getScore()[0] < .1 && ticksToBonusSpawn > Constants.MAX_TICKS_RUN_TO_BONUS) ||
							!isMeNearestWizard(BonusesPossibilityCalcs.BONUSES_POINTS[0], false)) {
						goToBonusActivated = false;
						moveToLineActivated = true;
					}
				}
			}

			if (!goToBonusActivated) {
				double distanceToBonusA = FastMath.hypot(self.getX() - BonusesPossibilityCalcs.BONUSES_POINTS[0].getX(),
														 self.getY() - BonusesPossibilityCalcs.BONUSES_POINTS[0].getY()) -
						self.getRadius() -
						game.getBonusRadius();
				double ticksRunToBonusA = distanceToBonusA / (Constants.getGame().getWizardForwardSpeed() * Variables.moveFactor) *
						Constants.TICKS_BUFFER_RUN_TO_BONUS;
				if (ticksRunToBonusA < Constants.MAX_TICKS_RUN_TO_BONUS &&
						(ticksRunToBonusA >= ticksToBonusSpawn || bonusesPossibilityCalcs.getScore()[0] > Constants.BONUS_POSSIBILITY_RUN) &&
						isMeNearestWizard(BonusesPossibilityCalcs.BONUSES_POINTS[0], true)) { // goto bonus 1
					goToBonusActivated = true;
					moveToLineActivated = false;
					currentAction.setActionType(CurrentAction.ActionType.MOVE_TO_POSITION);
					PositionMoveLine.INSTANCE.updatePointToMove(BonusesPossibilityCalcs.BONUSES_POINTS[0]);
				}

				double distanceToBonusB = FastMath.hypot(self.getX() - BonusesPossibilityCalcs.BONUSES_POINTS[1].getX(),
														 self.getY() - BonusesPossibilityCalcs.BONUSES_POINTS[1].getY()) -
						self.getRadius() -
						game.getBonusRadius();
				double ticksRunToBonusB = distanceToBonusB / (Constants.getGame().getWizardForwardSpeed() * Variables.moveFactor) *
						Constants.TICKS_BUFFER_RUN_TO_BONUS;

				if (ticksRunToBonusB < Constants.MAX_TICKS_RUN_TO_BONUS &&
						(ticksRunToBonusB >= ticksToBonusSpawn || bonusesPossibilityCalcs.getScore()[1] > Constants.BONUS_POSSIBILITY_RUN) &&
						isMeNearestWizard(BonusesPossibilityCalcs.BONUSES_POINTS[1], true)) { // goto bonus 1
					goToBonusActivated = true;
					moveToLineActivated = false;
					if (!(currentAction.getActionType() == CurrentAction.ActionType.MOVE_TO_POSITION && ticksRunToBonusA < ticksRunToBonusB)) {
						PositionMoveLine.INSTANCE.updatePointToMove(BonusesPossibilityCalcs.BONUSES_POINTS[1]);
					}
					currentAction.setActionType(CurrentAction.ActionType.MOVE_TO_POSITION);
				}
			}

			if (goToBonusActivated) {
				currentAction.setActionType(CurrentAction.ActionType.MOVE_TO_POSITION);
			} else if (!moveToLineActivated) {
				if (myLineCalc.getDistanceTo(self) > 300.) {
					moveToLineActivated = true;
				}
			}

			if (moveToLineActivated) {
				myLineCalc = Utils.fightLineSelect(lastFightLine, world, enemyPositionCalc, self);
				lastFightLine = myLineCalc;
				if (FastMath.hypot(myLineCalc.getFightPoint().getX() - self.getX(), myLineCalc.getFightPoint().getY() - self.getY()) > 500. &&
						myLineCalc.getDistanceTo(self) > Constants.getTopLine().getLineDistance()) {
					currentAction.setActionType(CurrentAction.ActionType.MOVE_TO_POSITION);
					PositionMoveLine.INSTANCE.updatePointToMove(myLineCalc.getPreFightPoint());
				} else {
					moveToLineActivated = false;
				}
			}

			if (currentAction.getActionType() == CurrentAction.ActionType.MOVE_TO_POSITION) {
				myLineCalc = PositionMoveLine.INSTANCE;
				direction = myLineCalc.getMoveDirection(self);

				filteredWorld = Utils.filterWorld(world,
												  new Point(self.getX() + Math.cos(direction) * Constants.MOVE_SCAN_FIGURE_CENTER,
															self.getY() + Math.sin(direction) * Constants.MOVE_SCAN_FIGURE_CENTER),
												  enemyPositionCalc.getBuildingPhantoms());
			}
		}

		if (currentAction.getActionType().moveCalc) {
			enemyFound = Utils.hasEnemy(filteredWorld.getMinions()) ||
					Utils.hasEnemy(filteredWorld.getWizards()) ||
					Utils.hasEnemy(filteredWorld.getBuildings());

			calcMatrixDanger(game);
			findAWay();
			if (!wayPoints.isEmpty()) {
				int lastPointGoTo = 1;
				double dangerAtStart = wayPoints.get(0).getPoint().getAllDangers() * Constants.DANGER_AT_START_MULT_RUN;
				while (lastPointGoTo < wayPoints.size() &&
						wayPoints.get(lastPointGoTo).getPoint().getAllDangers() >= dangerAtStart) {
					++lastPointGoTo;
				}
				boolean run = lastPointGoTo != wayPoints.size();
				if (lastPointGoTo == wayPoints.size()) {
					--lastPointGoTo;
				}

				if (testPointDirectAvailable(lastPointGoTo)) {
					moveTo(lastPointGoTo, move, run);
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
					moveTo(whichPointGoTo, move, run);
				}
			}
		}
		findTargets();
		shotAndTurn(move);
	}

	private void updateCastRange(Wizard[] wizards) {
		for (Wizard wizard : wizards) {
			castRange[(int) wizard.getId()] = wizard.getCastRange();
		}
	}

	private void updateProjectilesDTL(Projectile[] projectiles) {
		Variables.projectiles.clear();
		for (Projectile projectile : projectiles) {
			Variables.projectiles.add(projectile.getId());
		}
		for (Long projectileId : new ArrayList<>(projectilesDTL.keySet())) {
			if (!Variables.projectiles.contains(projectileId)) {
				projectilesDTL.remove(projectileId);
			}
		}

		for (Projectile projectile : projectiles) {
			if (projectilesDTL.containsKey(projectile.getId())) {
				projectilesDTL.put(projectile.getId(),
								   projectilesDTL.get(projectile.getId()) - Utils.getProjectileSpeed(projectile));
				continue;
			}
			long castUnit = projectile.getOwnerUnitId();
			double castRange = castUnit <= 10 ?
					this.castRange[(int) castUnit] - Utils.getProjectileSpeed(projectile)
					: Constants.getGame().getFetishBlowdartAttackRange() - Utils.getProjectileSpeed(projectile);
			projectilesDTL.put(projectile.getId(), castRange);
		}
	}

	protected double checkHitByProjectilePossible() {
		double maxStep = Variables.moveFactor * Constants.getGame().getWizardForwardSpeed();
		Point self = new Point(this.self.getX(), this.self.getY());
		double distance;
		double sumDamage = 0.;
		for (Projectile projectile : filteredWorld.getProjectiles()) {
			Point projectileStart = new Point(projectile.getX(), projectile.getY());
			Point projectileDestination = new Point(projectile.getSpeedX(), projectile.getSpeedY());
			projectileDestination.fixVectorLength(projectilesDTL.get(projectile.getId()));
			projectileDestination.add(projectileStart);
			distance = Utils.distancePointToSegment(self, projectileStart, projectileDestination);
			if (distance < maxStep + this.self.getRadius() + projectile.getRadius()) {
				sumDamage += Utils.getProjectileDamage(projectile);
			}
		}
		return sumDamage;
	}

	protected void evade(Move move, double maxDamageToRecieve) {
		if (maxDamageToRecieve <= .001) {
			return;
		}
		int bestActionType = -1; // stay
		double bestScore = 0.;
		double bestDamage = 0.;
		double bestDangerOnWay = 0.;
		Point position = new Point(self.getX(), self.getY());
		Point bestPosition = position;

		Utils.fillProjectilesSim(filteredWorld, projectilesDTL);
		int ticks = 0;
		while (!Variables.projectilesSim.isEmpty()) {
			testScanItem.setPoint(self.getX(), self.getY());
			Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, ticks);
			bestScore = testScanItem.getTotalScore(self);
			bestDangerOnWay += testScanItem.getAllDangers();
			bestDamage += Utils.checkProjectiveCollistion(position, ticks++);
		}

		double currScore;
		double currDamage;
		double currDangerOnWay;
		int hastenedTicks = Utils.wizardStatusTicks(self, StatusType.HASTENED);
		int currHastenedTicks;
		double moveFactor = Variables.moveFactor;
		double moveAngle;
		Point moveVector;
		boolean stuck;
		for (int i = 0; i != Constants.EVADE_CALCULATIONS_COUNT; ++i) {
			currScore = 0.;
			currDamage = 0.;
			currDangerOnWay = 0.;
			position = new Point(self.getX(), self.getY());
			Utils.fillProjectilesSim(filteredWorld, projectilesDTL);
			moveAngle = Utils.normalizeAngle(self.getAngle() + i * Constants.EVADE_DEGREE_STEP);
			currHastenedTicks = hastenedTicks;
			moveVector = new Point(Math.cos(moveAngle) * moveFactor * Constants.getGame().getWizardStrafeSpeed(),
								   Math.sin(moveAngle) * moveFactor * Constants.getGame().getWizardStrafeSpeed());
			ticks = 0;
			stuck = false;
			while (!Variables.projectilesSim.isEmpty()) {
				if (!stuck) {
					position.add(moveVector);
					if (--currHastenedTicks == -1) {
						moveVector.fixVectorLength((moveFactor - Constants.getGame().getHastenedMovementBonusFactor()) * Constants.getGame().getWizardStrafeSpeed());
					}
					testScanItem.setPoint(position.getX(), position.getY());
					Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, ticks);
					if (!testScanItem.isAvailable()) {
						stuck = true;
						position.negate(moveVector);
					}
					if (!stuck) {
						currScore = testScanItem.getTotalScore(self);
						currDangerOnWay += testScanItem.getAllDangers();
					}
				}
				currDamage += Utils.checkProjectiveCollistion(position, ticks++);
			}
			if (bestDamage < currDamage) {
				continue;
			}
			if (bestDamage == currDamage) {
				if (currDangerOnWay > bestDangerOnWay) {
					continue;
				}
				if (currDangerOnWay == bestDangerOnWay) {
					if (currScore <= bestScore) {
						continue;
					}
				}
			}
			bestActionType = i;
			bestDamage = currDamage;
			bestDangerOnWay = currDangerOnWay;
			bestScore = currScore;
			bestPosition = position;
		}
		if (bestDamage > 0.) {
			double bestEvadeDamage = bestDamage;
			double curentAngle;
			double currMoveFactor;
			AccAndSpeedWithFix accAndSpeed;
			Point positionChange;
			for (int i = 0; i != Constants.EVADE_CALCULATIONS_COUNT; ++i) {
				currScore = 0.;
				currDamage = 0.;
				currDangerOnWay = 0.;
				position = new Point(self.getX(), self.getY());
				Utils.fillProjectilesSim(filteredWorld, projectilesDTL);
				moveAngle = Utils.normalizeAngle(self.getAngle() + i * Constants.EVADE_DEGREE_STEP);
				curentAngle = Utils.normalizeAngle(self.getAngle());
				currHastenedTicks = hastenedTicks;
				currMoveFactor = moveFactor;
				ticks = 0;
				while (!Variables.projectilesSim.isEmpty()) {
					accAndSpeed = getAccAndSpeedByAngle(Utils.normalizeAngle(moveAngle - curentAngle), 100., currMoveFactor);
					positionChange = accAndSpeed.getCoordChange(curentAngle);
					position.add(positionChange);
					curentAngle += Utils.updateMaxModule(Utils.normalizeAngle(moveAngle - curentAngle), // angle to turn
														 currHastenedTicks >= 0. ?
																 Variables.turnFactor * Constants.getGame().getWizardMaxTurnAngle() :
																 Constants.getGame().getWizardMaxTurnAngle());
					if (--currHastenedTicks == -1) {
						currMoveFactor -= Constants.getGame().getHastenedMovementBonusFactor();
					}
					testScanItem.setPoint(position.getX(), position.getY());
					Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, ticks);
					if (testScanItem.isAvailable()) {
						currScore = testScanItem.getTotalScore(self);
						currDangerOnWay += testScanItem.getAllDangers();
					} else {
						position.negate(positionChange);
					}
					currDamage += Utils.checkProjectiveCollistion(position, ticks++);
				}
				if (bestDamage < currDamage || currDamage >= bestEvadeDamage) {
					continue;
				}
				if (bestDamage == currDamage) {
					if (currDangerOnWay > bestDangerOnWay) {
						continue;
					}
					if (currDangerOnWay == bestDangerOnWay) {
						if (currScore <= bestScore) {
							continue;
						}
					}
				}
				bestActionType = i + Constants.EVADE_CALCULATIONS_COUNT;
				bestDamage = currDamage;
				bestDangerOnWay = currDangerOnWay;
				bestScore = currScore;
				bestPosition = position;
			}
		}
		if (bestDamage < maxDamageToRecieve) { // escape from projectile
			moveAngle = self.getAngleTo(bestPosition.getX(), bestPosition.getY());
			AccAndSpeedWithFix accAndSpeedByAngle = getAccAndSpeedByAngle(moveAngle,
																		  FastMath.hypot(self.getX() - bestPosition.getX(), self.getY() - bestPosition.getY()));
			move.setSpeed(accAndSpeedByAngle.getSpeed());
			move.setStrafeSpeed(accAndSpeedByAngle.getStrafe());
			if (bestActionType < Constants.EVADE_CALCULATIONS_COUNT) {
				currentAction.setActionType(CurrentAction.ActionType.EVADE_PROJECTILE); // block move
			} else {
				turnTo(moveAngle, Constants.getGame().getWizardMaxTurnAngle() * Variables.turnFactor, move);
				currentAction.setActionType(CurrentAction.ActionType.RUN_FROM_PROJECTILE); // block move and turn
			}
		}
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

	private void turnTo(Point point, Move move) {
		if (currentAction.getActionType() == CurrentAction.ActionType.RUN_FROM_PROJECTILE) {
			return;
		}
		if (point == null) {
			return;
		}
		if (FastMath.hypot(point.getX() - self.getX(), point.getY() - self.getY()) < Constants.getGame().getWizardStrafeSpeed()) {
			point = pointToReach;
		}
		if (FastMath.hypot(point.getX() - self.getX(), point.getY() - self.getY()) < Constants.getGame().getWizardStrafeSpeed()) {
			return;
		}
		turnTo(self.getAngleTo(point.getX(), point.getY()), Constants.getGame().getWizardMaxTurnAngle() * Variables.turnFactor, move);
	}

	private void turnTo(double angle, double maxAngle, Move move) {
		if (currentAction.getActionType() == CurrentAction.ActionType.RUN_FROM_PROJECTILE) {
			return;
		}
		move.setTurn(Utils.updateMaxModule(angle, maxAngle));
	}

	private void findTargets() {
		targets.clear();
		double missileDamage = Utils.getSelfProjectileDamage(ProjectileType.MAGIC_MISSILE);
		treeCut = Utils.unitsCountAtDistance(filteredWorld.getTrees(),
											 self,
											 Constants.TREES_DISTANCE_TO_CUT) >= Constants.TREES_COUNT_TO_CUT || // too much trees around
				Utils.unitsCountCloseToDestination(filteredWorld.getAllBlocksList(), pointToReach) >= 2 && // can't go throught obstacles
						Utils.unitsCountCloseToDestination(filteredWorld.getTrees(), pointToReach) > 0; // one of them - tree
		for (LivingUnit livingUnit : filteredWorld.getAimsList()) {
			if (livingUnit.getFaction() != Constants.getEnemyFaction() &&
					(livingUnit.getFaction() != Faction.NEUTRAL || livingUnit.getLife() >= livingUnit.getMaxLife()) &&
					livingUnit.getFaction() != Faction.OTHER) {
				continue;
			}
			double score;
			if (livingUnit instanceof Tree) {
				if (treeCut) {
					// distance to destination
					// distance to me
					score = Constants.CUT_REACH_POINT_DISTANCE_PTIORITY / FastMath.hypot(pointToReach.getX() - livingUnit.getX(),
																						 pointToReach.getY() - livingUnit.getY());
					score += Constants.CUT_SELF_DISTANCE_PRIORITY / FastMath.hypot(self.getX() - livingUnit.getX(), self.getY() - livingUnit.getY());
					score /= (livingUnit.getLife() + missileDamage - 1) / missileDamage;
					targets.add(new AbstractMap.SimpleEntry<>(score - Constants.CUT_REACH_POINT_DISTANCE_PTIORITY, livingUnit));
				}
				continue;
			}
			score = Constants.LOW_AIM_SCORE;
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

	protected void moveTo(int pointIdx, Move move, boolean run) {
		if (wayPoints.size() < 2) {
			return;
		}
		ScanMatrixItem point = wayPoints.get(pointIdx).getPoint();
		moveToPoint = point;
		double distance = FastMath.hypot(self, point.getX(), point.getY());
		angle = self.getAngleTo(point.getX(), point.getY());

		AccAndSpeedWithFix accAndStrafe = getAccAndSpeedByAngle(angle, distance);
		move.setSpeed(accAndStrafe.getSpeed());
		move.setStrafeSpeed(accAndStrafe.getStrafe());

		minAngle = 0.;
		maxAngle = 0.;
		targetAngle = 0.;

		if (run) { // look forward
			int idx = pointIdx + 1;
			while (idx < wayPoints.size() && pointIdx + 5 >= idx) {
				Point wayPoint = wayPoints.get(idx++).getPoint();
				targetAngle = Utils.normalizeAngle(self.getAngleTo(wayPoint.getX(), wayPoint.getY()) - angle);
				if (targetAngle < minAngle) {
					minAngle = targetAngle;
				} else if (targetAngle > maxAngle) {
					maxAngle = targetAngle;
				}
			}
			minAngle = Math.max(-Math.PI, minAngle - Constants.RUN_ANGLE_EXPAND);
			maxAngle = Math.min(Math.PI, maxAngle + Constants.RUN_ANGLE_EXPAND);
			targetAngle = Utils.normalizeAngle(targetAngle + angle);
		} else { // look on path
			int step = Math.max(pointIdx / 5, 1);
			int idx = pointIdx - step;
			while (idx > 0) {
				Point wayPoint = wayPoints.get(idx).getPoint();
				targetAngle = Utils.normalizeAngle(self.getAngleTo(wayPoint.getX(), wayPoint.getY()) - angle);
				if (targetAngle < minAngle) {
					minAngle = targetAngle;
				} else if (targetAngle > maxAngle) {
					maxAngle = targetAngle;
				}
				idx -= step;
			}
			targetAngle = angle;
		}
		Point changePosition;
		if (Math.abs(Utils.normalizeAngle(maxAngle - minAngle)) > Constants.MOVE_ANGLE_PRECISE) {
			changePosition = accAndStrafe.getCoordChange(self.getAngle());
			testScanItem.setPoint(self.getX() + changePosition.getX(), self.getY() + changePosition.getY());
			Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self);

			double bestDanger = testScanItem.getAllDangers();
			double bestScore = testScanItem.getTotalScore(self);
			double newAngle;
			double closestAngle = Math.abs(Utils.normalizeAngle(angle - targetAngle));
			Point bestPosition = testScanItem.clonePoint();
			AccAndSpeedWithFix bestMove = accAndStrafe;
			double itAngle = minAngle;
			for (; maxAngle - itAngle > Constants.MOVE_ANGLE_PRECISE; itAngle += Constants.MOVE_ANGLE_PRECISE) {
				newAngle = Utils.normalizeAngle(angle + itAngle);
				accAndStrafe = getAccAndSpeedByAngle(newAngle, 100.);
				changePosition = accAndStrafe.getCoordChange(self.getAngle());
				testScanItem.setPoint(self.getX() + changePosition.getX(), self.getY() + changePosition.getY());
				Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self);
				if (!testScanItem.isAvailable() || bestDanger < testScanItem.getAllDangers() || run) {
					continue;
				}
				if (testScanItem.getAllDangers() == bestDanger) {
					if (bestScore > testScanItem.getTotalScore(self)) {
						continue;
					}
					if (bestScore == testScanItem.getTotalScore(self) && Math.abs(Utils.normalizeAngle(newAngle - targetAngle)) > closestAngle) {
						continue;
					}
				}
				bestPosition = testScanItem.clonePoint();
				bestMove = accAndStrafe;
				bestDanger = testScanItem.getAllDangers();
				bestScore = testScanItem.getTotalScore(self);
			}
			moveToPoint = bestPosition;
			move.setSpeed(bestMove.getSpeed());
			move.setStrafeSpeed(bestMove.getStrafe());
		}
	}

	public AccAndSpeedWithFix getAccAndSpeedByAngle(double angle, double distance) {
		return getAccAndSpeedByAngle(angle, distance, Variables.moveFactor);
	}

	public AccAndSpeedWithFix getAccAndSpeedByAngle(double angle, double distance, double moveFactor) {
		double strafe = Math.sin(angle) * distance;
		double acc = Math.cos(angle) * distance;
		double fwdLimit = (acc > 0 ? Constants.getGame().getWizardForwardSpeed() : Constants.getGame().getWizardBackwardSpeed()) * moveFactor;

		double fix = Math.hypot(acc / fwdLimit, strafe / (Constants.getGame().getWizardStrafeSpeed() * moveFactor));
		if (fix > 1.) {
			return new AccAndSpeedWithFix(acc / fix, strafe / fix, fix);
		} else {
			return new AccAndSpeedWithFix(acc, strafe, fix);
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

				double distanceTo = myLineCalc.calcLineDistanceOtherDanger(item);
				if (distanceTo > 0.) {
					item.addOtherDanger(distanceTo);
				}

				if (SpawnPoint.checkSpawnPoints()) {
					for (SpawnPoint spawnPoint : Constants.SPAWN_POINTS) {
						if (spawnPoint.isPointInDanger(item.getX(), item.getY())) {
							item.addOtherDanger(Constants.SPAWN_POINT_DANGER);
						}
					}
				}

				if (currentAction.getActionType() == CurrentAction.ActionType.MOVE_TO_POSITION) {
					item.setOtherBonus(item.getForwardDistanceDivision() * 70); //up to 2
				} else if (!enemyFound) {
					item.setOtherBonus(item.getForwardDistanceDivision() * .0001);
				}
			}
		}

		ScoreCalcStructure structure = new ScoreCalcStructure();

		for (Bonus bonus : filteredWorld.getBonuses()) {
			structure.clear();
			if (FastMath.hypot(self.getX() - bonus.getX(), self.getY() - bonus.getY()) > Constants.getFightDistanceFilter()) {
				continue;
			}

			ScoreCalcStructure.OTHER_BONUS_APPLYER.setScore(200.);
			ScoreCalcStructure.OTHER_BONUS_APPLYER.setDistance(self.getRadius() + bonus.getRadius());
			structure.putItem(ScoreCalcStructure.OTHER_BONUS_APPLYER);

			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new Point(bonus.getX(), bonus.getY()));
			}
		}

		if (Utils.getTicksToBonusSpawn(world.getTickIndex()) < 250) {
			for (Point bonusesPoint : BonusesPossibilityCalcs.BONUSES_POINTS) {
				structure.clear();
				if (FastMath.hypot(self.getX() - bonusesPoint.getX(), self.getY() - bonusesPoint.getY()) > Constants.getFightDistanceFilter()) {
					continue;
				}

				ScoreCalcStructure.MINION_DANGER_APPLYER.setScore(100.);
				ScoreCalcStructure.MINION_DANGER_APPLYER.setDistance(self.getRadius() + game.getBonusRadius() + .1);
				structure.putItem(ScoreCalcStructure.MINION_DANGER_APPLYER);

				ScoreCalcStructure.OTHER_BONUS_APPLYER.setScore((350 - Utils.getTicksToBonusSpawn(world.getTickIndex())) * .75);
				ScoreCalcStructure.OTHER_BONUS_APPLYER.setDistance(self.getRadius() + game.getBonusRadius() +
																		   Constants.getGame().getWizardBackwardSpeed() * Variables.moveFactor);
				structure.putItem(ScoreCalcStructure.OTHER_BONUS_APPLYER);
				for (int i = 0; i != scan_matrix.length; ++i) {
					applyScoreForLine(scan_matrix[i], structure, bonusesPoint);
				}
			}
		}

		if (!enemyFound) {
			return;
		}
		double myDamage = 12.;
		if (Utils.wizardHasStatus(self, StatusType.EMPOWERED)) {
			myDamage *= Constants.getGame().getEmpoweredDamageFactor();
		}
		double shieldBonus = Utils.wizardHasStatus(self, StatusType.SHIELDED) ? (1. - Constants.getGame().getShieldedDirectDamageAbsorptionFactor()) : 1.;

		ScoreCalcStructure.EXP_BONUS_APPLYER.setDistance(Constants.EXPERIENCE_DISTANCE);

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
					ScoreCalcStructure.MINION_DANGER_APPLYER.setScore(minion.getDamage() * shieldBonus * .5);
					ScoreCalcStructure.MINION_DANGER_APPLYER.setDistance(Utils.cooldownDistanceCalculation(game.getOrcWoodcutterAttackRange() + self.getRadius(),
																										   minion.getRemainingActionCooldownTicks()));
					structure.putItem(ScoreCalcStructure.MINION_DANGER_APPLYER);
					break;
				case FETISH_BLOWDART:
					ScoreCalcStructure.MINION_DANGER_APPLYER.setScore(game.getDartDirectDamage() * shieldBonus * .5);
					ScoreCalcStructure.MINION_DANGER_APPLYER.setDistance(Utils.cooldownDistanceCalculation(game.getFetishBlowdartAttackRange() + self.getRadius(),
																										   minion.getRemainingActionCooldownTicks()));
					structure.putItem(ScoreCalcStructure.MINION_DANGER_APPLYER);
					break;
			}
			ScoreCalcStructure.MINION_DANGER_APPLYER_SECOND.setScore(ScoreCalcStructure.MINION_DANGER_APPLYER.getScore());
			ScoreCalcStructure.MINION_DANGER_APPLYER_SECOND.setDistance(Utils.getDistanceToNearestAlly(minion, filteredWorld, minion.getVisionRange()));
			structure.putItem(ScoreCalcStructure.MINION_DANGER_APPLYER_SECOND);

			ScoreCalcStructure.ATTACK_BONUS_APPLYER.setScore(myDamage * Constants.MINION_ATTACK_FACTOR);
			ScoreCalcStructure.ATTACK_BONUS_APPLYER.setDistance(self.getCastRange());
			structure.putItem(ScoreCalcStructure.ATTACK_BONUS_APPLYER);

			ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER.setScore(myDamage * Constants.MINION_ATTACK_FACTOR);
			ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER.setDistance(Constants.getGame().getStaffRange() + minion.getRadius());
			structure.putItem(ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER);

			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new Point(minion.getX(), minion.getY()));
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
				wizardDamage *= Constants.getGame().getEmpoweredDamageFactor();
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

//			ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER.setScore(myDamage);
//			ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER.setDistance(Constants.getGame().getStaffRange() + wizard.getRadius());
//			structure.putItem(ScoreCalcStructure.MELEE_ATTACK_BONUS_APPLYER);
//
			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new Point(wizard.getX(), wizard.getY()));
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
				applyScoreForLine(scan_matrix[i], structure, new Point(building.getX(), building.getY()));
			}
		}
	}

	public void applyScoreForLine(ScanMatrixItem[] items, ScoreCalcStructure structure, Point point) {
		double distance = Utils.distancePointToSegment(point, items[0], items[items.length - 1]);
		if (distance > structure.getMaxScoreDistance()) {
			return;
		}
		double itemDistance;
		for (int i = 0; i != items.length; ++i) {
			if (!items[i].isAvailable()) {
				continue;
			}
			itemDistance = FastMath.hypot(items[i].getX() - point.getX(), items[i].getY() - point.getY());
			if (itemDistance <= structure.getMaxScoreDistance()) {
				structure.applyScores(items[i], itemDistance);
			}
		}
	}

	public boolean isMeNearestWizard(Point point, boolean includeEnemies) {
		double distanceToMe = FastMath.hypot(self.getX() - point.getX(), self.getY() - point.getY()) * Constants.NEAREST_TO_BONUS_CALCULATION_OTHER_MULT;
		if (includeEnemies) {
			for (WizardPhantom phantom : enemyPositionCalc.getDetectedWizards().values()) {
				double distance = FastMath.hypot(phantom.getPosition().getX() - point.getX(), phantom.getPosition().getY() - point.getY());
				if (!phantom.isUpdated()) {
					distance -= (world.getTickIndex() - phantom.getLastSeenTick()) * Constants.MAX_WIZARDS_FORWARD_SPEED;
				}
				if (distanceToMe > distance) {
					return false;
				}
			}
		}

		for (Wizard wizard : world.getWizards()) {
			if (wizard.getFaction() != Constants.getCurrentFaction()) {
				continue;
			}
			if (FastMath.hypot(wizard.getX() - point.getX(), wizard.getY() - point.getY()) < distanceToMe) {
				return false;
			}
		}
		return true;
	}


}
