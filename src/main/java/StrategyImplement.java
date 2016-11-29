import model.ActionType;
import model.Bonus;
import model.Building;
import model.CircularUnit;
import model.Faction;
import model.Game;
import model.Minion;
import model.Move;
import model.Projectile;
import model.ProjectileType;
import model.SkillType;
import model.StatusType;
import model.Tree;
import model.Wizard;
import model.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by by.dragoon on 11/8/16.
 */
public class StrategyImplement implements Strategy {

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

	private PriorityQueue<WayPoint> queue = new PriorityQueue<>();

	protected List<Pair<Double, CircularUnit>> missileTargets = new ArrayList<>();
	protected List<Pair<Double, CircularUnit>> staffTargets = new ArrayList<>();
	protected List<Pair<Double, CircularUnit>> iceTargets = new ArrayList<>();
	protected List<Pair<Double, Point>> fireTargets = new LinkedList<>();
	protected List<Pair<Double, CircularUnit>> prevMissileTargets = new ArrayList<>();
	protected List<Pair<Double, CircularUnit>> prevStaffTargets = new ArrayList<>();
	protected List<Pair<Double, CircularUnit>> prevIceTargets = new ArrayList<>();

	protected double minAngle = 0.;
	protected double maxAngle = 0.;
	protected double angle = 0.;
	protected double targetAngle = 0.;

	protected HashMap<Long, Double> projectilesDTL = new HashMap<>(); //store
	protected CurrentAction currentAction = new CurrentAction();
	protected double[] castRange = new double[]{500., 500., 500., 500., 500., 500., 500., 500., 500., 500., 500.};

	protected EnemyPositionCalc enemyPositionCalc = new EnemyPositionCalc();

	protected BonusesPossibilityCalcs bonusesPossibilityCalcs = new BonusesPossibilityCalcs();

	protected AgressiveNeutralsCalcs agressiveNeutralsCalcs = new AgressiveNeutralsCalcs();
	protected UnitScoreCalculation unitScoreCalculation = new UnitScoreCalculation();
	protected TeammateIdsContainer teammateIdsContainer = new TeammateIdsContainer();

	protected boolean treeCut;
	protected boolean goToBonusActivated = false;
	protected boolean moveToLineActivated = false;

	public StrategyImplement(Wizard self) {
		myLineCalc = Constants.getLine(Utils.getDefaultMyLine((int) self.getId()));
		lastFightLine = myLineCalc;
	}

	public void move(Wizard self, World world, Game game, Move move) {
		agressiveNeutralsCalcs.updateMap(world);
		enemyPositionCalc.updatePositions(world);
		bonusesPossibilityCalcs.updateTick(world, enemyPositionCalc);
		teammateIdsContainer.updateTeammatesIds(world);
		SkillsLearning.updateSkills(self, move);
		enemyFound = false;
		treeCut = false;
		moveToPoint = null;
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
										  enemyPositionCalc.getBuildingPhantoms(), teammateIdsContainer);
		updateProjectilesDTL(filteredWorld.getProjectiles());

		Utils.calcCurrentSkillBonuses(self, filteredWorld);
		currentAction.setActionType(CurrentAction.ActionType.FIGHT); // default state
		enemyFound = Utils.hasEnemy(filteredWorld.getMinions(), agressiveNeutralsCalcs) ||
				Utils.hasEnemy(filteredWorld.getWizards()) ||
				Utils.hasEnemy(filteredWorld.getBuildings());
		unitScoreCalculation.updateScores(filteredWorld, self, enemyFound, agressiveNeutralsCalcs);
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
												  enemyPositionCalc.getBuildingPhantoms(), teammateIdsContainer);
				enemyFound = Utils.hasEnemy(filteredWorld.getMinions(), agressiveNeutralsCalcs) ||
						Utils.hasEnemy(filteredWorld.getWizards()) ||
						Utils.hasEnemy(filteredWorld.getBuildings());
				unitScoreCalculation.updateScores(filteredWorld, self, enemyFound, agressiveNeutralsCalcs);
			}

			if (goToBonusActivated && FastMath.hypot(self, PositionMoveLine.INSTANCE.getPositionToMove()) <= self.getRadius() +
					game.getBonusRadius() +
					game.getWizardForwardSpeed() * Variables.moveFactor + .1) {
				boolean bonusOnPlace = bonusesPossibilityCalcs.getScore()[PositionMoveLine.INSTANCE.getPositionToMove().getX() > 2000 ? 1 : 0] > .9;
				if (!bonusOnPlace) {
					Point movePoint = PositionMoveLine.INSTANCE.getPositionToMove().negateCopy(self);
					movePoint.update(-movePoint.getX(), -movePoint.getY());
					currentAction.setActionType(CurrentAction.ActionType.EVADE_PROJECTILE);
					double needDistance = game.getBonusRadius() + self.getRadius() - .5;
					if (ticksToBonusSpawn < 2) {
						needDistance += 1;
					}
					movePoint.fixVectorLength(needDistance);
					movePoint.add(PositionMoveLine.INSTANCE.getPositionToMove());
					AccAndSpeedWithFix accAndSpeedByAngle = AccAndSpeedWithFix.getAccAndSpeedByAngle(Utils.normalizeAngle(self.getAngleTo(movePoint.getX(),
																																		  movePoint.getY())),
																									 self.getDistanceTo(movePoint.getX(), movePoint.getY()),
																									 Variables.moveFactor);
					move.setSpeed(accAndSpeedByAngle.getSpeed());
					move.setStrafeSpeed(accAndSpeedByAngle.getStrafe());
					moveToPoint = movePoint;
				}
			}
		}

		if (currentAction.getActionType().moveCalc) {
			calcMatrixDanger();
			Variables.maxDangerMatrixScore = Double.NEGATIVE_INFINITY;
			for (int i = 0; i != scan_matrix.length; ++i) {
				for (int j = 0; j != scan_matrix[0].length; ++j) {
					if (!scan_matrix[i][j].isAvailable()) {
						continue;
					}
					Variables.maxDangerMatrixScore = Math.max(scan_matrix[i][j].getTotalScore(self), Variables.maxDangerMatrixScore);
				}
			}
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
								   projectilesDTL.get(projectile.getId()) - Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()]);
				continue;
			}
			long castUnit = projectile.getOwnerUnitId();
			double castRange = castUnit <= 10 ?
					this.castRange[(int) castUnit] - Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()]
					: Constants.getGame().getFetishBlowdartAttackRange() - Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()];
			projectilesDTL.put(projectile.getId(), castRange);
		}
	}

	protected double checkHitByProjectilePossible() {
		double maxStep = Variables.moveFactor * Constants.getGame().getWizardForwardSpeed();
		Point self = new Point(this.self.getX(), this.self.getY());
		double distance;
		double sumDamage = 0.;
		double projectileRadius;
		for (Projectile projectile : filteredWorld.getProjectiles()) {
			Point projectileStart = new Point(projectile.getX(), projectile.getY());
			Point projectileDestination = new Point(projectile.getSpeedX(), projectile.getSpeedY());
			projectileDestination.fixVectorLength(projectilesDTL.get(projectile.getId()));
			projectileDestination.add(projectileStart);
			distance = Utils.distancePointToSegment(self, projectileStart, projectileDestination);
			projectileRadius = projectile.getRadius();
			if (projectile.getType() == ProjectileType.FIREBALL) {
				projectileRadius = Constants.getGame().getFireballExplosionMinDamageRange();
			}
			if (distance < maxStep + this.self.getRadius() + projectileRadius) {
				sumDamage += Utils.getProjectileDamage(projectile, distance);
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

		UnitScoreCalculationTickSupport unitScoreCalculationTickSupport = new UnitScoreCalculationTickSupport(unitScoreCalculation);
		Utils.fillProjectilesSim(filteredWorld, projectilesDTL);
		int ticks = 0;
		while (!Variables.projectilesSim.isEmpty()) {
			testScanItem.setPoint(self.getX(), self.getY());
			Utils.calcTileScore(testScanItem,
								filteredWorld,
								myLineCalc,
								self,
								unitScoreCalculationTickSupport.getScores(filteredWorld, self, enemyFound, agressiveNeutralsCalcs, ticks),
								enemyFound);
			bestScore = testScanItem.getTotalScore(self);
			bestDangerOnWay += testScanItem.getAllDangers();
			bestDamage += Utils.checkProjectiveCollision(position, ticks++);
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
					Utils.calcTileScore(testScanItem,
										filteredWorld,
										myLineCalc,
										self,
										unitScoreCalculationTickSupport.getScores(filteredWorld, self, enemyFound, agressiveNeutralsCalcs, ticks),
										enemyFound);
					if (!testScanItem.isAvailable()) {
						stuck = true;
						position.negate(moveVector);
					}
					if (!stuck) {
						currScore = testScanItem.getTotalScore(self);
						currDangerOnWay += testScanItem.getAllDangers();
					}
				}
				currDamage += Utils.checkProjectiveCollision(position, ticks++);
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
					accAndSpeed = AccAndSpeedWithFix.getAccAndSpeedByAngle(Utils.normalizeAngle(moveAngle - curentAngle), 100., currMoveFactor);
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
					Utils.calcTileScore(testScanItem,
										filteredWorld,
										myLineCalc,
										self,
										unitScoreCalculationTickSupport.getScores(filteredWorld, self, enemyFound, agressiveNeutralsCalcs, ticks),
										enemyFound);
					if (testScanItem.isAvailable()) {
						currScore = testScanItem.getTotalScore(self);
						currDangerOnWay += testScanItem.getAllDangers();
					} else {
						position.negate(positionChange);
					}
					currDamage += Utils.checkProjectiveCollision(position, ticks++);
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
			AccAndSpeedWithFix accAndSpeedByAngle = AccAndSpeedWithFix.getAccAndSpeedByAngle(moveAngle,
																							 FastMath.hypot(self.getX() - bestPosition.getX(),
																											self.getY() - bestPosition.getY()));
			move.setSpeed(accAndSpeedByAngle.getSpeed());
			move.setStrafeSpeed(accAndSpeedByAngle.getStrafe());
			if (bestActionType < Constants.EVADE_CALCULATIONS_COUNT) {
				currentAction.setActionType(CurrentAction.ActionType.EVADE_PROJECTILE); // block move
			} else {
				turnTo(moveAngle, move);
				currentAction.setActionType(CurrentAction.ActionType.RUN_FROM_PROJECTILE); // block move and turn
			}
		}
	}

	private void shotAndTurn(Move move) {
		if (missileTargets.isEmpty()) {
			turnTo(moveToPoint, move);
			return;
		}
		CircularUnit target = missileTargets.get(0).getSecond();
		Point targetShootPoint = Utils.getShootPoint(target, self, Utils.PROJECTIVE_RADIUS[ProjectileType.MAGIC_MISSILE.ordinal()]);
		CircularUnit frostTarget = iceTargets.isEmpty() ? null : iceTargets.get(0).getSecond();
		Point frostTargetShootPoint = frostTarget != null ?
				Utils.getShootPoint(frostTarget, self, Utils.PROJECTIVE_RADIUS[ProjectileType.FROST_BOLT.ordinal()]) :
				null;
		CircularUnit meleeTarget = staffTargets.isEmpty() ? null : staffTargets.get(0).getSecond();

		Pair<Double, Point> fireTarget = fireTargets.isEmpty() ? null : fireTargets.get(0);
		if (fireTarget != null && fireTarget.getFirst() < 40) {
			fireTarget = null;
		}

		if (fireTarget != null) {
			if (fireTarget.getFirst() > 90. || self.getMana() > self.getMaxMana() * .9) {
				if (applyTargetAction(ActionType.FIREBALL, FastMath.hypot(self, fireTarget.getSecond()), fireTarget.getSecond(), move)) {
					return;
				}
			}
		}

		if (frostTarget != null) {
			if (frostTarget instanceof Wizard || self.getMana() > self.getMaxMana() * .9 || self.getLife() < self.getMaxLife() * .5) {
				if (applyTargetAction(ActionType.FROST_BOLT, FastMath.hypot(self, target) - target.getRadius(), frostTargetShootPoint, move)) {
					return;
				}
			}
		}

		if (fireTarget == null || fireTarget.getFirst() < 70. || self.getMana() > self.getMaxMana() * .9) {
			if (applyTargetAction(ActionType.MAGIC_MISSILE, target instanceof Tree ?
										  1000. :
										  FastMath.hypot(self, target) - target.getRadius(),
								  targetShootPoint, move)) {
				return;
			}
		}

		if (meleeTarget != null) {
			if (applyMeleeAction(meleeTarget, move)) {
				return;
			}
		}

		turnTo(moveToPoint, move);
	}

	private boolean applyMeleeAction(CircularUnit target, Move move) {
		double turnAngle = self.getAngleTo(target.getX(), target.getY());
		double maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle() * Variables.turnFactor;
		int turnTicksCount = getTurnCount(turnAngle, maxTurnAngle, Constants.MAX_SHOOT_ANGLE);

		int hastenedTicksRemain = Utils.wizardStatusTicks(self, StatusType.HASTENED);
		if (hastenedTicksRemain > -1 && turnTicksCount > hastenedTicksRemain) {
			maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle();
			turnTicksCount = getTurnCount(turnAngle, maxTurnAngle, Constants.MAX_SHOOT_ANGLE);
		}

		if (waitTimeForAction(ActionType.STAFF) <= turnTicksCount + 2) {
			if (checkHit(turnAngle, target, move)) {
				turnTo(moveToPoint, move);
				return true;
			}
			turnTo(turnAngle, move);
			return true;
		}
		return false;
	}

	private boolean applyTargetAction(ActionType actionType, double minCastRange, Point target, Move move) {
		double turnAngle = self.getAngleTo(target.getX(), target.getY());

		double maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle() * Variables.turnFactor;
		int turnTicksCount = getTurnCount(turnAngle, maxTurnAngle, Constants.MAX_SHOOT_ANGLE);

		int hastenedTicksRemain = Utils.wizardStatusTicks(self, StatusType.HASTENED);
		if (hastenedTicksRemain > -1 && turnTicksCount > hastenedTicksRemain) {
			maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle();
			turnTicksCount = getTurnCount(turnAngle, maxTurnAngle, Constants.MAX_SHOOT_ANGLE);
		}

		if (waitTimeForAction(actionType) <= turnTicksCount + 2) {
			// если уже можем попасть - атакуем и бежим дальше
			if (checkShot(turnAngle, target, minCastRange, move, actionType)) {
				turnTo(moveToPoint, move);
				return true;
			}
			// если не можем попасть - доворачиваем на цель
			turnTo(turnAngle, move);
			return true;
		}
		return false;
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

	private boolean checkShot(double angle, Point point, double minCastRange, Move move, ActionType actionType) {
		if (Math.abs(angle) > Constants.MAX_SHOOT_ANGLE) {
			return false;
		}
		if (FastMath.hypot(self, point) < self.getCastRange() && waitTimeForAction(actionType) == 0) {
			move.setCastAngle(angle);
			move.setAction(actionType);
			move.setMinCastDistance(minCastRange);//self.getCastRange() - .01
			if (actionType == ActionType.FIREBALL) {
				move.setMaxCastDistance(minCastRange);
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
		Wizard nearestEnemyWizard = null;
		double minDistance = 1e6;
		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() != Constants.getEnemyFaction()) {
				continue;
			}
			double distance = FastMath.hypot(wizard, self);
			if (distance < minDistance) {
				nearestEnemyWizard = wizard;
				minDistance = distance;
			}
		}

		if (minDistance < 600 && minDistance > 400.) {
			//turn to side
			double nearestWizardAngle = self.getAngleTo(nearestEnemyWizard);
			boolean leftSide;
			if (Math.abs(nearestWizardAngle) < Constants.getGame().getWizardMaxTurnAngle() * Variables.turnFactor * .5) {
				double prefferedAngle = nearestWizardAngle;
				if (point != null && FastMath.hypot(self, point) > Variables.moveFactor * Constants.getGame().getWizardStrafeSpeed()) {
					prefferedAngle = self.getAngleTo(point.getX(), point.getY());
				}
				leftSide = prefferedAngle > 0;
			} else {
				leftSide = nearestWizardAngle < 0;
			}
			nearestWizardAngle = Math.abs(nearestWizardAngle);
			double turnAngle = Math.PI * .5 - nearestWizardAngle;
			turnTo(leftSide ? turnAngle : turnAngle * -1, move);
		} else {
			if (point == null) {
				return;
			}
			if (FastMath.hypot(point.getX() - self.getX(), point.getY() - self.getY()) < Constants.getGame().getWizardStrafeSpeed()) {
				point = pointToReach;
			}
			if (FastMath.hypot(point.getX() - self.getX(), point.getY() - self.getY()) < Constants.getGame().getWizardStrafeSpeed()) {
				return;
			}
			turnTo(self.getAngleTo(point.getX(), point.getY()), move);
		}
	}

	private void turnTo(double angle, Move move) {
		if (currentAction.getActionType() == CurrentAction.ActionType.RUN_FROM_PROJECTILE) {
			return;
		}
		move.setTurn(Utils.updateMaxModule(angle, Constants.getGame().getWizardMaxTurnAngle() * Variables.turnFactor));
	}

	private void findTargets() {
		prevIceTargets = iceTargets;
		prevMissileTargets = missileTargets;
		prevStaffTargets = staffTargets;
		iceTargets = new ArrayList<>();
		missileTargets = new ArrayList<>();
		staffTargets = new ArrayList<>();
		int missileDamage = Utils.getSelfProjectileDamage(ProjectileType.MAGIC_MISSILE);
		int frostBoltDamage = Utils.SKILLS_LEARNED.contains(SkillType.FROST_BOLT) ? Utils.getSelfProjectileDamage(ProjectileType.FROST_BOLT) : 0;
		treeCut = (myLineCalc == PositionMoveLine.INSTANCE &&
				(Utils.unitsCountCloseToDestination(filteredWorld.getTrees(), new Point(self.getX(), self.getY())) > 0 ||
						Utils.unitsCountAtDistance(filteredWorld.getTrees(), self, Constants.TREES_DISTANCE_TO_CUT) >= 3)) ||
				Utils.unitsCountAtDistance(filteredWorld.getTrees(),
										   self,
										   Constants.TREES_DISTANCE_TO_CUT) >= Constants.TREES_COUNT_TO_CUT || // too much trees around
				Utils.unitsCountCloseToDestination(filteredWorld.getAllBlocksList(), pointToReach) >= 2 && // can't go throught obstacles
						Utils.unitsCountCloseToDestination(filteredWorld.getTrees(), pointToReach) > 0; // one of them - tree
		double score;
		double distanceToTarget;

		int staffDamage = Variables.staffDamage;
		if (Utils.wizardHasStatus(self, StatusType.EMPOWERED)) {
			staffDamage *= Constants.getGame().getEmpoweredDamageFactor();
		}

		if (treeCut) {
			for (Tree tree : filteredWorld.getTrees()) {

				// distance to destination
				// distance to me
				score = Constants.CUT_REACH_POINT_DISTANCE_PTIORITY / FastMath.hypot(pointToReach.getX() - tree.getX(),
																					 pointToReach.getY() - tree.getY());
				distanceToTarget = FastMath.hypot(self, tree);
				score += Constants.CUT_SELF_DISTANCE_PRIORITY / distanceToTarget;

				score *= (tree.getRadius() + self.getRadius()) * .02;
				missileTargets.add(new Pair<>(score / Utils.getHitsToKill(tree.getLife(), missileDamage) -
													  Constants.CUT_REACH_POINT_DISTANCE_PTIORITY,
											  tree));

				if (distanceToTarget < Constants.getGame().getStaffRange() + tree.getRadius() + 50) {
					distanceToTarget -= Constants.getGame().getStaffRange() + tree.getRadius();
					if (distanceToTarget > 0) {
						score *= 1 - distanceToTarget * .01; // divizion by 100
					}
					staffTargets.add(new Pair<>(score / Utils.getHitsToKill(tree.getLife(), staffDamage)
														- Constants.CUT_REACH_POINT_DISTANCE_PTIORITY,
												tree));
				}
			}
		}

		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}
			if (minion.getFaction() == Faction.NEUTRAL && !agressiveNeutralsCalcs.isMinionAgressive(minion.getId())) {
				continue;
			}

			score = Utils.getMinionAttackPriority(minion, missileDamage, self);
			missileTargets.add(new Pair<>(score, minion));
			Utils.appendStaffTarget(staffTargets, minion, self, Utils.getMinionAttackPriority(minion, staffDamage, self));
			if (frostBoltDamage > 0) {
				score = Utils.getMinionAttackPriority(minion, frostBoltDamage, self);
				iceTargets.add(new Pair<>(score, minion));
			}
		}

		for (Building building : filteredWorld.getBuildings()) {
			if (building.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}
			score = Constants.LOW_AIM_SCORE;
			double tmp = (building.getMaxLife() - building.getLife()) / (double) building.getMaxLife();
			score += tmp * tmp;
			score *= Constants.BUILDING_AIM_PROIRITY;
			missileTargets.add(new Pair<>(score, building));
			Utils.appendStaffTarget(staffTargets, building, self, score);
		}

		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}
			score = Constants.LOW_AIM_SCORE;
			double tmp = (wizard.getMaxLife() - wizard.getLife()) / (double) wizard.getMaxLife();
			score += tmp * tmp;
			score *= Constants.WIZARD_AIM_PROIRITY;
			if (Utils.wizardHasStatus(wizard, StatusType.SHIELDED)) {
				score *= Constants.SHIELDENED_AIM_PRIORITY;
			}
			if (Utils.wizardHasStatus(wizard, StatusType.EMPOWERED)) {
				score *= Constants.EMPOWERED_AIM_PRIORITY;
			}
			Utils.appendStaffTarget(staffTargets, wizard, self, score);
			missileTargets.add(new Pair<>(score, wizard));
			if (frostBoltDamage > 0) {
				iceTargets.add(new Pair<>(score * Constants.FROST_WIZARD_AIM_PROIRITY, wizard));
			}
		}

		Utils.applyPreviousContainedModifier(staffTargets, prevStaffTargets);
		Utils.applyPreviousContainedModifier(missileTargets, prevMissileTargets);
		Utils.applyPreviousContainedModifier(iceTargets, prevIceTargets);

		Collections.sort(staffTargets, Utils.AIM_SORT_COMPARATOR);
		Collections.sort(missileTargets, Utils.AIM_SORT_COMPARATOR);
		Collections.sort(iceTargets, Utils.AIM_SORT_COMPARATOR);

		staffTargets = staffTargets.subList(0, Math.min(staffTargets.size(), 3));
		Utils.filterTargets(missileTargets, ProjectileType.MAGIC_MISSILE, self, filteredWorld);
		Utils.filterTargets(iceTargets, ProjectileType.FROST_BOLT, self, filteredWorld);

		fireTargets.clear();

		if (Utils.wizardHasSkill(self, SkillType.FIREBALL)) {
			for (Minion minion : filteredWorld.getMinions()) {
				if (minion.getFaction() == Constants.getCurrentFaction()) {
					continue;
				}
				if (minion.getFaction() == Faction.NEUTRAL && !agressiveNeutralsCalcs.isMinionAgressive(minion.getId())) {
					continue;
				}
				if (minion.getSpeedX() != 0. || minion.getSpeedY() != 0) {
					continue;
				}
				Point checkPoint = new Point(minion.getX(), minion.getY());
				addFireTarget(checkDistances(checkPoint,
											 minion.getRadius() + Constants.getGame().getFireballExplosionMaxDamage() - .1));
				addFireTarget(checkDistances(new Point(minion.getX(), minion.getY()),
											 minion.getRadius() + Constants.getGame().getFireballExplosionMinDamage() - .1));
			}

			for (Building building : filteredWorld.getBuildings()) {
				if (building.getFaction() == Constants.getCurrentFaction()) {
					continue;
				}
				Point checkPoint = new Point(building.getX(), building.getY());
				addFireTarget(checkDistances(checkPoint, building.getRadius() + Constants.getGame().getFireballExplosionMaxDamage() - .1));
				addFireTarget(checkDistances(checkPoint, building.getRadius() + Constants.getGame().getFireballExplosionMinDamage() - .1));
			}

			for (Wizard wizard : filteredWorld.getWizards()) {
				if (wizard.getFaction() == Constants.getCurrentFaction()) {
					continue;
				}

				Point checkPoint = new Point(wizard.getX(), wizard.getY());
				int ticks = Utils.getTicksToFly(FastMath.hypot(self, wizard), Utils.PROJECTIVE_SPEED[ProjectileType.FIREBALL.ordinal()]);
				ticks = Math.min(ticks - 1, ShootEvasionMatrix.EVASION_MATRIX[0].length - 1);
				if (FastMath.hypot(self, wizard) < self.getCastRange()) {
					Point wizardPoint = new Point(wizard.getX(), wizard.getY());
					double damage = checkFireballDamage(wizardPoint);
					fireTargets.add(new Pair<Double, Point>(damage, wizardPoint));
				}
				double checkDistance = wizard.getRadius() +
						Constants.getGame().getFireballExplosionMaxDamage() -
						ShootEvasionMatrix.EVASION_MATRIX[0][ticks] - .1;
				if (checkDistance > 0.) {
					addFireTarget(checkDistances(checkPoint, checkDistance));
				}
				checkDistance = wizard.getRadius() +
						Constants.getGame().getFireballExplosionMinDamage() -
						ShootEvasionMatrix.EVASION_MATRIX[0][ticks] - .1;
				addFireTarget(checkDistances(checkPoint, checkDistance));
			}
			Collections.sort(fireTargets, Utils.POINT_AIM_SORT_COMPARATOR);
			for (Iterator<Pair<Double, Point>> iterator = fireTargets.iterator(); iterator.hasNext(); ) {
				Pair<Double, Point> fireTarget = iterator.next();
				if (Utils.noTreesOnWay(fireTarget.getSecond(), self, ProjectileType.FIREBALL, filteredWorld)) {
					if (iterator.hasNext()) {
						iterator.next();
					}
					while (iterator.hasNext()) {
						iterator.next();
						iterator.remove();
					}
					break;
				} else {
					iterator.remove();
				}
			}
		}
	}

	private void addFireTarget(Pair<Double, Point> target) {
		if (target != null) {
			fireTargets.add(target);
		}
	}


	private final static int angleCheck = 20;
	private final static int checkCount = 360 / angleCheck;
	private final static double angleCheckRadians = Math.PI / 180. * angleCheck;

	private Pair<Double, Point> checkDistances(Point point, double distance) {
		double maxPriority = 0.;
		Point bestPoint = null;
		for (int i = 0; i != checkCount; ++i) {
			Point checkPoint = new Point(point.getX() + distance * Math.cos(angleCheckRadians * i), point.getY() + distance * Math.sin(angleCheckRadians * i));
			if (FastMath.hypot(self, checkPoint) > self.getCastRange()) {
				continue;
			}
			double temp = checkFireballDamage(checkPoint);
			if (temp > maxPriority) {
				maxPriority = temp;
				bestPoint = checkPoint;
			}
		}
		if (bestPoint == null) {
			return null;
		}
		return new Pair<Double, Point>(maxPriority, bestPoint);
	}

	public double checkFireballDamage(Point where) {
		int ticksToFly = Utils.getTicksToFly(FastMath.hypot(self, where), Utils.PROJECTIVE_SPEED[ProjectileType.FIREBALL.ordinal()]);
		double totalDamage = 0.;
		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}
			if (minion.getFaction() == Faction.NEUTRAL && !agressiveNeutralsCalcs.isMinionAgressive(minion.getId())) {
				continue;
			}

			Point checkPoint = new Point(minion.getX() + minion.getSpeedY() * ticksToFly, minion.getY() + minion.getSpeedY() * ticksToFly);
			double distance = FastMath.hypot(checkPoint, where) - minion.getRadius();
			if (distance <= Constants.getGame().getFireballExplosionMinDamageRange()) {
				double damage;
				if (distance <= Constants.getGame().getFireballExplosionMaxDamageRange()) {
					damage = Constants.getGame().getFireballExplosionMaxDamage();
				} else {
					damage = Constants.getGame().getFireballExplosionMinDamage();
				}
				totalDamage += Math.min(damage + Constants.getGame().getBurningSummaryDamage() / 2, minion.getLife());
			}
		}

		for (Building building : filteredWorld.getBuildings()) {
			if (building.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}

			double distance = FastMath.hypot(building, where) - building.getRadius();
			if (distance <= Constants.getGame().getFireballExplosionMinDamageRange()) {
				double damage;
				if (distance <= Constants.getGame().getFireballExplosionMaxDamageRange()) {
					damage = Constants.getGame().getFireballExplosionMaxDamage();
				} else {
					damage = Constants.getGame().getFireballExplosionMinDamage();
				}
				totalDamage += Math.min(damage + Constants.getGame().getBurningSummaryDamage(), building.getLife()) * 1.5;
			}
		}

		for (Wizard wizard : filteredWorld.getWizards()) {
			totalDamage += checkFirePointsWizard(wizard, where, ticksToFly);
		}
		totalDamage += checkFirePointsWizard(self, where, ticksToFly);
		return totalDamage;
	}

	private double checkFirePointsWizard(Wizard wizard, Point where, int ticksToFly) {
		ticksToFly = Math.min(ticksToFly - 1, ShootEvasionMatrix.EVASION_MATRIX[0].length - 1);
		double distance = FastMath.hypot(wizard, where) - wizard.getRadius();
		if (wizard.isMe()) {
			distance += ShootEvasionMatrix.EVASION_MATRIX[0][ticksToFly] * Variables.moveFactor;
		} else {
			distance += ShootEvasionMatrix.EVASION_MATRIX[0][ticksToFly];
		}

		if (distance <= Constants.getGame().getFireballExplosionMinDamageRange()) {
			double score;
			if (distance <= Constants.getGame().getFireballExplosionMaxDamageRange()) {
				score = Constants.getGame().getFireballExplosionMaxDamage();
			} else {
				score = Constants.getGame().getFireballExplosionMinDamage();
			}
			score = Math.min(score + Constants.getGame().getBurningSummaryDamage(), wizard.getLife()) * 3.;
			if (wizard.getLife() < score + Constants.getGame().getBurningSummaryDamage() * .5 &&
					wizard.getFaction() == Constants.getEnemyFaction()) {
				score += 65.;
			}
			if (wizard.getFaction() == Constants.getCurrentFaction()) {
				if (wizard.isMe()) {
					return -score * 5;
				} else {
					return -score;
				}
			} else {
				return score;
			}
		}
		return 0;
	}

	protected void moveTo(int pointIdx, Move move, boolean run) {
		if (wayPoints.size() < 2) {

			return;
		}
		ScanMatrixItem point = wayPoints.get(pointIdx).getPoint();
		moveToPoint = point;
		double distance = FastMath.hypot(self, point.getX(), point.getY());
		angle = self.getAngleTo(point.getX(), point.getY());

		AccAndSpeedWithFix accAndStrafe = AccAndSpeedWithFix.getAccAndSpeedByAngle(angle, distance);
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
			minAngle = Math.max(-Math.PI, minAngle - Constants.MOVE_ANGLE_EXPAND);
			maxAngle = Math.min(Math.PI, maxAngle + Constants.MOVE_ANGLE_EXPAND);
			targetAngle = angle;
		}
		Point changePosition;
		if (Math.abs(Utils.normalizeAngle(maxAngle - minAngle)) > Constants.MOVE_ANGLE_PRECISE) {
			changePosition = accAndStrafe.getCoordChange(self.getAngle());
			testScanItem.setPoint(self.getX() + changePosition.getX(), self.getY() + changePosition.getY());
			Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, unitScoreCalculation, enemyFound);

			double bestDanger = testScanItem.getAllDangers();
			double bestScore = testScanItem.getTotalScore(self);
			double newAngle;
			double closestAngle = Math.abs(Utils.normalizeAngle(angle - targetAngle));
			Point bestPosition = testScanItem.clonePoint();
			AccAndSpeedWithFix bestMove = accAndStrafe;
			double itAngle = minAngle;
			for (; maxAngle - itAngle > Constants.MOVE_ANGLE_PRECISE; itAngle += Constants.MOVE_ANGLE_PRECISE) {
				newAngle = Utils.normalizeAngle(angle + itAngle);
				accAndStrafe = AccAndSpeedWithFix.getAccAndSpeedByAngle(newAngle, 100.);
				changePosition = accAndStrafe.getCoordChange(self.getAngle());
				testScanItem.setPoint(self.getX() + changePosition.getX(), self.getY() + changePosition.getY());
				Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, unitScoreCalculation, enemyFound);
				if (!testScanItem.isAvailable() || bestDanger < testScanItem.getAllDangers()) { //run???
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
				wayPoints.add(new WayPoint(1, scan_matrix[Constants.CURRENT_PT_X][Constants.CURRENT_PT_Y], null));
				wayPoints.add(new WayPoint(2, item, wayPoints.get(0)));
				return;
			}
		}
		queue.add(new WayPoint(1, scan_matrix[Constants.CURRENT_PT_X][Constants.CURRENT_PT_Y], null));
		int nextDistanceFromStart;
		double newScoresOnWay;
		double newDangerOnWay;
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
				newScoresOnWay = scanMatrixItem.getTotalScore(self) - Variables.maxDangerMatrixScore + currentPoint.getScoresOnWay();

				if (newScoresOnWay == wayPointToCompare.getScoresOnWay()) {
					if (wayPointToCompare.getDistanceFromStart() == nextDistanceFromStart) {
						newDangerOnWay = scanMatrixItem.getAllDangers() + currentPoint.getDangerOnWay();
						if (newDangerOnWay < wayPointToCompare.getDangerOnWay()) {
							queue.add(new WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
						}
					} else if (nextDistanceFromStart < wayPointToCompare.getDistanceFromStart()) {
						queue.add(new WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
					}
				} else if (newScoresOnWay > wayPointToCompare.getDangerOnWay()) {
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
					score = newScanMatrixItem.getTotalScore(self) +
							newScanMatrixItem.getWayPoint().getScoresOnWay() / newScanMatrixItem.getWayPoint().getDistanceFromStart() + Variables.maxDangerMatrixScore;
					continue;
				}
				tmpScore = newScanMatrixItem.getTotalScore(self) +
						newScanMatrixItem.getWayPoint().getScoresOnWay() / newScanMatrixItem.getWayPoint().getDistanceFromStart()
						+ Variables.maxDangerMatrixScore;
				if (tmpScore > score || (tmpScore == score && best.getDistanceFromSelf() < 6.)) {
//					if (newScanMatrixItem.getDistanceFromSelf() < 6.) {
//						tmpScore -= (6. - newScanMatrixItem.getDistanceFromSelf()) / 100.;
//					}
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

	private void calcMatrixDanger() {
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
					if (FastMath.hypot(self.getX() - myLineCalc.getFightPoint().getX(), self.getY() - myLineCalc.getFightPoint().getY()) > 200) {
						item.addOtherBonus(item.getForwardDistanceDivision() * 140);
					}
				} else if (!enemyFound) {
					item.addOtherBonus(item.getForwardDistanceDivision() * .01);
				}
			}
		}


		for (Bonus bonus : filteredWorld.getBonuses()) {
			if (FastMath.hypot(self.getX() - bonus.getX(), self.getY() - bonus.getY()) > Constants.getFightDistanceFilter()) {
				continue;
			}
			ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(bonus.getId());

			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new Point(bonus.getX(), bonus.getY()));
			}
		}

		if (Utils.getTicksToBonusSpawn(world.getTickIndex()) < 250) {
			for (int i = 0; i != BonusesPossibilityCalcs.BONUSES_POINTS.length; ++i) {
				if (FastMath.hypot(self, BonusesPossibilityCalcs.BONUSES_POINTS[i]) > Constants.getFightDistanceFilter()) {
					continue;
				}

				ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(i - 5);

				for (int j = 0; j != scan_matrix.length; ++j) {
					applyScoreForLine(scan_matrix[j], structure, BonusesPossibilityCalcs.BONUSES_POINTS[i]);
				}
			}
		}

		if (!enemyFound) {
			return;
		}

		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}

			ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(minion.getId());

			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new Point(minion.getX(), minion.getY()));
			}
		}

		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}
			ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(wizard.getId());
			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new Point(wizard.getX(), wizard.getY()));
			}
		}

		for (Building building : filteredWorld.getBuildings()) {
			if (building.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}
			ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(building.getId());

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
