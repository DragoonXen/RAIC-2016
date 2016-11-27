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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Created by by.dragoon on 11/8/16.
 */
public class XXX_StrategyImplement implements Strategy {

	protected World world;
	protected Wizard self;

	protected XXX_BaseLine myLineCalc;
	protected XXX_BaseLine lastFightLine;
	protected double direction;

	protected XXX_FilteredWorld filteredWorld;

	protected final XXX_ScanMatrixItem[][] scan_matrix = XXX_Utils.createScanMatrix();

	protected int lastTick;

	protected boolean enemyFound;

	protected XXX_ScanMatrixItem pointToReach;

	private XXX_ScanMatrixItem testScanItem = new XXX_ScanMatrixItem(0, 0, 1.);

	protected ArrayList<XXX_WayPoint> wayPoints = new ArrayList<>();

	protected XXX_Point moveToPoint;

	protected CircularUnit target;
	protected CircularUnit meleeTarget;

	private PriorityQueue<XXX_WayPoint> queue = new PriorityQueue<>();

	private List<Map.Entry<Double, CircularUnit>> targets = new ArrayList<>();

	protected double minAngle = 0.;
	protected double maxAngle = 0.;
	protected double angle = 0.;
	protected double targetAngle = 0.;

	protected HashMap<Long, Double> projectilesDTL = new HashMap<>(); //store
	protected XXX_CurrentAction currentAction = new XXX_CurrentAction();
	protected double[] castRange = new double[]{500., 500., 500., 500., 500., 500., 500., 500., 500., 500., 500.};

	protected XXX_EnemyPositionCalc enemyPositionCalc = new XXX_EnemyPositionCalc();

	protected XXX_BonusesPossibilityCalcs bonusesPossibilityCalcs = new XXX_BonusesPossibilityCalcs();

	protected XXX_AgressiveNeutralsCalcs agressiveNeutralsCalcs = new XXX_AgressiveNeutralsCalcs();
	protected XXX_UnitScoreCalculation unitScoreCalculation = new XXX_UnitScoreCalculation();

	protected boolean treeCut;
	protected boolean goToBonusActivated = false;
	protected boolean moveToLineActivated = false;

	public XXX_StrategyImplement(Wizard self) {
		myLineCalc = XXX_Constants.getLine(XXX_Utils.getDefaultMyLine((int) self.getId()));
		lastFightLine = myLineCalc;
	}

	public void move(Wizard self, World world, Game game, Move move) {
		agressiveNeutralsCalcs.updateMap(world);
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

		XXX_Variables.self = self;
		this.world = world;
		this.self = self;
		XXX_SpawnPoint.updateTick(world.getTickIndex());
		updateCastRange(world.getWizards());

		for (XXX_BaseLine baseLine : XXX_Constants.getLines()) {
			baseLine.updateFightPoint(world, enemyPositionCalc);
		}

		myLineCalc = XXX_Utils.fightLineSelect(lastFightLine, world, enemyPositionCalc, self);
		lastFightLine = myLineCalc;

		lastTick = world.getTickIndex();

		direction = myLineCalc.getMoveDirection(self);

		filteredWorld = XXX_Utils.filterWorld(world,
											  new XXX_Point(self.getX() + Math.cos(direction) * XXX_Constants.MOVE_SCAN_FIGURE_CENTER,
															self.getY() + Math.sin(direction) * XXX_Constants.MOVE_SCAN_FIGURE_CENTER),
											  enemyPositionCalc.getBuildingPhantoms());
		updateProjectilesDTL(filteredWorld.getProjectiles());

		XXX_Utils.calcCurrentSkillBonuses(self, filteredWorld);
		currentAction.setActionType(XXX_CurrentAction.ActionType.FIGHT); // default state
		enemyFound = XXX_Utils.hasEnemy(filteredWorld.getMinions(), agressiveNeutralsCalcs) ||
				XXX_Utils.hasEnemy(filteredWorld.getWizards()) ||
				XXX_Utils.hasEnemy(filteredWorld.getBuildings());
		unitScoreCalculation.updateScores(filteredWorld, self, enemyFound, agressiveNeutralsCalcs);
		evade(move, checkHitByProjectilePossible());

		if (currentAction.getActionType() == XXX_CurrentAction.ActionType.FIGHT) {
			int ticksToBonusSpawn = XXX_Utils.getTicksToBonusSpawn(world.getTickIndex());
			if (goToBonusActivated) {
				if (XXX_PositionMoveLine.INSTANCE.getPositionToMove().getX() > 2000) {
					if ((bonusesPossibilityCalcs.getScore()[1] < .1 && ticksToBonusSpawn > XXX_Constants.MAX_TICKS_RUN_TO_BONUS) ||
							!isMeNearestWizard(XXX_BonusesPossibilityCalcs.BONUSES_POINTS[1], false)) {
						goToBonusActivated = false;
						moveToLineActivated = true;
					}
				} else {
					if ((bonusesPossibilityCalcs.getScore()[0] < .1 && ticksToBonusSpawn > XXX_Constants.MAX_TICKS_RUN_TO_BONUS) ||
							!isMeNearestWizard(XXX_BonusesPossibilityCalcs.BONUSES_POINTS[0], false)) {
						goToBonusActivated = false;
						moveToLineActivated = true;
					}
				}
			}

			if (!goToBonusActivated) {
				double distanceToBonusA = XXX_FastMath.hypot(self.getX() - XXX_BonusesPossibilityCalcs.BONUSES_POINTS[0].getX(),
															 self.getY() - XXX_BonusesPossibilityCalcs.BONUSES_POINTS[0].getY()) -
						self.getRadius() -
						game.getBonusRadius();
				double ticksRunToBonusA = distanceToBonusA / (XXX_Constants.getGame().getWizardForwardSpeed() * XXX_Variables.moveFactor) *
						XXX_Constants.TICKS_BUFFER_RUN_TO_BONUS;
				if (ticksRunToBonusA < XXX_Constants.MAX_TICKS_RUN_TO_BONUS &&
						(ticksRunToBonusA >= ticksToBonusSpawn || bonusesPossibilityCalcs.getScore()[0] > XXX_Constants.BONUS_POSSIBILITY_RUN) &&
						isMeNearestWizard(XXX_BonusesPossibilityCalcs.BONUSES_POINTS[0], true)) { // goto bonus 1
					goToBonusActivated = true;
					moveToLineActivated = false;
					currentAction.setActionType(XXX_CurrentAction.ActionType.MOVE_TO_POSITION);
					XXX_PositionMoveLine.INSTANCE.updatePointToMove(XXX_BonusesPossibilityCalcs.BONUSES_POINTS[0]);
				}

				double distanceToBonusB = XXX_FastMath.hypot(self.getX() - XXX_BonusesPossibilityCalcs.BONUSES_POINTS[1].getX(),
															 self.getY() - XXX_BonusesPossibilityCalcs.BONUSES_POINTS[1].getY()) -
						self.getRadius() -
						game.getBonusRadius();
				double ticksRunToBonusB = distanceToBonusB / (XXX_Constants.getGame().getWizardForwardSpeed() * XXX_Variables.moveFactor) *
						XXX_Constants.TICKS_BUFFER_RUN_TO_BONUS;

				if (ticksRunToBonusB < XXX_Constants.MAX_TICKS_RUN_TO_BONUS &&
						(ticksRunToBonusB >= ticksToBonusSpawn || bonusesPossibilityCalcs.getScore()[1] > XXX_Constants.BONUS_POSSIBILITY_RUN) &&
						isMeNearestWizard(XXX_BonusesPossibilityCalcs.BONUSES_POINTS[1], true)) { // goto bonus 1
					goToBonusActivated = true;
					moveToLineActivated = false;
					if (!(currentAction.getActionType() == XXX_CurrentAction.ActionType.MOVE_TO_POSITION && ticksRunToBonusA < ticksRunToBonusB)) {
						XXX_PositionMoveLine.INSTANCE.updatePointToMove(XXX_BonusesPossibilityCalcs.BONUSES_POINTS[1]);
					}
					currentAction.setActionType(XXX_CurrentAction.ActionType.MOVE_TO_POSITION);
				}
			}

			if (goToBonusActivated) {
				currentAction.setActionType(XXX_CurrentAction.ActionType.MOVE_TO_POSITION);
			} else if (!moveToLineActivated) {
				if (myLineCalc.getDistanceTo(self) > 300.) {
					moveToLineActivated = true;
				}
			}

			if (moveToLineActivated) {
				myLineCalc = XXX_Utils.fightLineSelect(lastFightLine, world, enemyPositionCalc, self);
				lastFightLine = myLineCalc;
				if (XXX_FastMath.hypot(myLineCalc.getFightPoint().getX() - self.getX(), myLineCalc.getFightPoint().getY() - self.getY()) > 500. &&
						myLineCalc.getDistanceTo(self) > XXX_Constants.getTopLine().getLineDistance()) {
					currentAction.setActionType(XXX_CurrentAction.ActionType.MOVE_TO_POSITION);
					XXX_PositionMoveLine.INSTANCE.updatePointToMove(myLineCalc.getPreFightPoint());
				} else {
					moveToLineActivated = false;
				}
			}

			if (currentAction.getActionType() == XXX_CurrentAction.ActionType.MOVE_TO_POSITION) {
				myLineCalc = XXX_PositionMoveLine.INSTANCE;
				direction = myLineCalc.getMoveDirection(self);

				filteredWorld = XXX_Utils.filterWorld(world,
													  new XXX_Point(self.getX() + Math.cos(direction) * XXX_Constants.MOVE_SCAN_FIGURE_CENTER,
																	self.getY() + Math.sin(direction) * XXX_Constants.MOVE_SCAN_FIGURE_CENTER),
													  enemyPositionCalc.getBuildingPhantoms());
				enemyFound = XXX_Utils.hasEnemy(filteredWorld.getMinions(), agressiveNeutralsCalcs) ||
						XXX_Utils.hasEnemy(filteredWorld.getWizards()) ||
						XXX_Utils.hasEnemy(filteredWorld.getBuildings());
				unitScoreCalculation.updateScores(filteredWorld, self, enemyFound, agressiveNeutralsCalcs);
			}

			if (goToBonusActivated && XXX_FastMath.hypot(self, XXX_PositionMoveLine.INSTANCE.getPositionToMove()) <= self.getRadius() +
					game.getBonusRadius() +
					game.getWizardForwardSpeed() * XXX_Variables.moveFactor + .1) {
				boolean bonusOnPlace = bonusesPossibilityCalcs.getScore()[XXX_PositionMoveLine.INSTANCE.getPositionToMove().getX() > 2000 ? 1 : 0] > .9;
				if (!bonusOnPlace) {
					XXX_Point movePoint = XXX_PositionMoveLine.INSTANCE.getPositionToMove().negateCopy(self);
					movePoint.update(-movePoint.getX(), -movePoint.getY());
					currentAction.setActionType(XXX_CurrentAction.ActionType.EVADE_PROJECTILE);
					double needDistance = game.getBonusRadius() + self.getRadius() - .5;
					if (ticksToBonusSpawn < 2) {
						needDistance += 1;
					}
					movePoint.fixVectorLength(needDistance);
					movePoint.add(XXX_PositionMoveLine.INSTANCE.getPositionToMove());
					XXX_AccAndSpeedWithFix accAndSpeedByAngle = XXX_AccAndSpeedWithFix.getAccAndSpeedByAngle(XXX_Utils.normalizeAngle(self.getAngleTo(movePoint.getX(),
																																					  movePoint.getY())),
																											 self.getDistanceTo(movePoint.getX(),
																																movePoint.getY()),
																											 XXX_Variables.moveFactor);
					move.setSpeed(accAndSpeedByAngle.getSpeed());
					move.setStrafeSpeed(accAndSpeedByAngle.getStrafe());
					moveToPoint = movePoint;
				}
			}
		}

		if (currentAction.getActionType().moveCalc) {
			calcMatrixDanger();
			XXX_Variables.maxDangerMatrixScore = Double.NEGATIVE_INFINITY;
			for (int i = 0; i != scan_matrix.length; ++i) {
				for (int j = 0; j != scan_matrix[0].length; ++j) {
					if (!scan_matrix[i][j].isAvailable()) {
						continue;
					}
					XXX_Variables.maxDangerMatrixScore = Math.max(scan_matrix[i][j].getTotalScore(self), XXX_Variables.maxDangerMatrixScore);
				}
			}
			findAWay();
			if (!wayPoints.isEmpty()) {
				int lastPointGoTo = 1;
				double dangerAtStart = wayPoints.get(0).getPoint().getAllDangers() * XXX_Constants.DANGER_AT_START_MULT_RUN;
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
		XXX_Variables.projectiles.clear();
		for (Projectile projectile : projectiles) {
			XXX_Variables.projectiles.add(projectile.getId());
		}
		for (Long projectileId : new ArrayList<>(projectilesDTL.keySet())) {
			if (!XXX_Variables.projectiles.contains(projectileId)) {
				projectilesDTL.remove(projectileId);
			}
		}

		for (Projectile projectile : projectiles) {
			if (projectilesDTL.containsKey(projectile.getId())) {
				projectilesDTL.put(projectile.getId(),
								   projectilesDTL.get(projectile.getId()) - XXX_Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()]);
				continue;
			}
			long castUnit = projectile.getOwnerUnitId();
			double castRange = castUnit <= 10 ?
					this.castRange[(int) castUnit] - XXX_Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()]
					: XXX_Constants.getGame().getFetishBlowdartAttackRange() - XXX_Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()];
			projectilesDTL.put(projectile.getId(), castRange);
		}
	}

	protected double checkHitByProjectilePossible() {
		double maxStep = XXX_Variables.moveFactor * XXX_Constants.getGame().getWizardForwardSpeed();
		XXX_Point self = new XXX_Point(this.self.getX(), this.self.getY());
		double distance;
		double sumDamage = 0.;
		for (Projectile projectile : filteredWorld.getProjectiles()) {
			XXX_Point projectileStart = new XXX_Point(projectile.getX(), projectile.getY());
			XXX_Point projectileDestination = new XXX_Point(projectile.getSpeedX(), projectile.getSpeedY());
			projectileDestination.fixVectorLength(projectilesDTL.get(projectile.getId()));
			projectileDestination.add(projectileStart);
			distance = XXX_Utils.distancePointToSegment(self, projectileStart, projectileDestination);
			if (distance < maxStep + this.self.getRadius() + projectile.getRadius()) {
				sumDamage += XXX_Utils.getProjectileDamage(projectile);
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
		XXX_Point position = new XXX_Point(self.getX(), self.getY());
		XXX_Point bestPosition = position;

		XXX_UnitScoreCalculationTickSupport unitScoreCalculationTickSupport = new XXX_UnitScoreCalculationTickSupport(unitScoreCalculation);
		XXX_Utils.fillProjectilesSim(filteredWorld, projectilesDTL);
		int ticks = 0;
		while (!XXX_Variables.projectilesSim.isEmpty()) {
			testScanItem.setPoint(self.getX(), self.getY());
			XXX_Utils.calcTileScore(testScanItem,
									filteredWorld,
									myLineCalc,
									self,
									unitScoreCalculationTickSupport.getScores(filteredWorld, self, enemyFound, agressiveNeutralsCalcs, ticks),
									enemyFound);
			bestScore = testScanItem.getTotalScore(self);
			bestDangerOnWay += testScanItem.getAllDangers();
			bestDamage += XXX_Utils.checkProjectiveCollision(position, ticks++);
		}

		double currScore;
		double currDamage;
		double currDangerOnWay;
		int hastenedTicks = XXX_Utils.wizardStatusTicks(self, StatusType.HASTENED);
		int currHastenedTicks;
		double moveFactor = XXX_Variables.moveFactor;
		double moveAngle;
		XXX_Point moveVector;
		boolean stuck;
		for (int i = 0; i != XXX_Constants.EVADE_CALCULATIONS_COUNT; ++i) {
			currScore = 0.;
			currDamage = 0.;
			currDangerOnWay = 0.;
			position = new XXX_Point(self.getX(), self.getY());
			XXX_Utils.fillProjectilesSim(filteredWorld, projectilesDTL);
			moveAngle = XXX_Utils.normalizeAngle(self.getAngle() + i * XXX_Constants.EVADE_DEGREE_STEP);
			currHastenedTicks = hastenedTicks;
			moveVector = new XXX_Point(Math.cos(moveAngle) * moveFactor * XXX_Constants.getGame().getWizardStrafeSpeed(),
									   Math.sin(moveAngle) * moveFactor * XXX_Constants.getGame().getWizardStrafeSpeed());
			ticks = 0;
			stuck = false;
			while (!XXX_Variables.projectilesSim.isEmpty()) {
				if (!stuck) {
					position.add(moveVector);
					if (--currHastenedTicks == -1) {
						moveVector.fixVectorLength((moveFactor - XXX_Constants.getGame().getHastenedMovementBonusFactor()) * XXX_Constants.getGame().getWizardStrafeSpeed());
					}
					testScanItem.setPoint(position.getX(), position.getY());
					XXX_Utils.calcTileScore(testScanItem,
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
				currDamage += XXX_Utils.checkProjectiveCollision(position, ticks++);
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
			XXX_AccAndSpeedWithFix accAndSpeed;
			XXX_Point positionChange;
			for (int i = 0; i != XXX_Constants.EVADE_CALCULATIONS_COUNT; ++i) {
				currScore = 0.;
				currDamage = 0.;
				currDangerOnWay = 0.;
				position = new XXX_Point(self.getX(), self.getY());
				XXX_Utils.fillProjectilesSim(filteredWorld, projectilesDTL);
				moveAngle = XXX_Utils.normalizeAngle(self.getAngle() + i * XXX_Constants.EVADE_DEGREE_STEP);
				curentAngle = XXX_Utils.normalizeAngle(self.getAngle());
				currHastenedTicks = hastenedTicks;
				currMoveFactor = moveFactor;
				ticks = 0;
				while (!XXX_Variables.projectilesSim.isEmpty()) {
					accAndSpeed = XXX_AccAndSpeedWithFix.getAccAndSpeedByAngle(XXX_Utils.normalizeAngle(moveAngle - curentAngle), 100., currMoveFactor);
					positionChange = accAndSpeed.getCoordChange(curentAngle);
					position.add(positionChange);
					curentAngle += XXX_Utils.updateMaxModule(XXX_Utils.normalizeAngle(moveAngle - curentAngle), // angle to turn
															 currHastenedTicks >= 0. ?
																	 XXX_Variables.turnFactor * XXX_Constants.getGame().getWizardMaxTurnAngle() :
																	 XXX_Constants.getGame().getWizardMaxTurnAngle());
					if (--currHastenedTicks == -1) {
						currMoveFactor -= XXX_Constants.getGame().getHastenedMovementBonusFactor();
					}
					testScanItem.setPoint(position.getX(), position.getY());
					XXX_Utils.calcTileScore(testScanItem,
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
					currDamage += XXX_Utils.checkProjectiveCollision(position, ticks++);
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
				bestActionType = i + XXX_Constants.EVADE_CALCULATIONS_COUNT;
				bestDamage = currDamage;
				bestDangerOnWay = currDangerOnWay;
				bestScore = currScore;
				bestPosition = position;
			}
		}
		if (bestDamage < maxDamageToRecieve) { // escape from projectile
			moveAngle = self.getAngleTo(bestPosition.getX(), bestPosition.getY());
			XXX_AccAndSpeedWithFix accAndSpeedByAngle = XXX_AccAndSpeedWithFix.getAccAndSpeedByAngle(moveAngle,
																									 XXX_FastMath.hypot(self.getX() - bestPosition.getX(),
																														self.getY() - bestPosition.getY()));
			move.setSpeed(accAndSpeedByAngle.getSpeed());
			move.setStrafeSpeed(accAndSpeedByAngle.getStrafe());
			if (bestActionType < XXX_Constants.EVADE_CALCULATIONS_COUNT) {
				currentAction.setActionType(XXX_CurrentAction.ActionType.EVADE_PROJECTILE); // block move
			} else {
				turnTo(moveAngle, XXX_Constants.getGame().getWizardMaxTurnAngle() * XXX_Variables.turnFactor, move);
				currentAction.setActionType(XXX_CurrentAction.ActionType.RUN_FROM_PROJECTILE); // block move and turn
			}
		}
	}

	private void shotAndTurn(Move move) {
		if (target == null) {
			turnTo(moveToPoint, move);
			return;
		}

		double turnAngle = self.getAngleTo(target);

		double maxTurnAngle = XXX_Constants.getGame().getWizardMaxTurnAngle() * XXX_Variables.turnFactor;
		int turnTicksCount = getTurnCount(turnAngle, maxTurnAngle, XXX_Constants.MAX_SHOOT_ANGLE);

		int hastenedTicksRemain = XXX_Utils.wizardStatusTicks(self, StatusType.HASTENED);
		if (hastenedTicksRemain > -1 && turnTicksCount > hastenedTicksRemain) {
			maxTurnAngle = XXX_Constants.getGame().getWizardMaxTurnAngle();
			turnTicksCount = getTurnCount(turnAngle, maxTurnAngle, XXX_Constants.MAX_SHOOT_ANGLE);
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

			maxTurnAngle = XXX_Constants.getGame().getWizardMaxTurnAngle() * XXX_Variables.turnFactor;
			int meleeTurnTicksCount = getTurnCount(turnAngle, maxTurnAngle, XXX_Constants.getStaffHitSector());

			if (hastenedTicksRemain > -1 && turnTicksCount > hastenedTicksRemain) {
				maxTurnAngle = XXX_Constants.getGame().getWizardMaxTurnAngle();
				meleeTurnTicksCount = getTurnCount(turnAngle, maxTurnAngle, XXX_Constants.getStaffHitSector());
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
		if (Math.abs(angle) > XXX_Constants.getStaffHitSector()) {
			return false;
		}
		if (XXX_FastMath.hypot(self.getX() - target.getX(), self.getY() - target.getY()) < target.getRadius() + XXX_Constants.getGame().getStaffRange()
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
		if (Math.abs(angle) > XXX_Constants.MAX_SHOOT_ANGLE) {
			return false;
		}
		if (XXX_FastMath.hypot(self.getX() - target.getX(), self.getY() - target.getY()) < target.getRadius() + self.getCastRange()
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

	private void turnTo(XXX_Point point, Move move) {
		if (currentAction.getActionType() == XXX_CurrentAction.ActionType.RUN_FROM_PROJECTILE) {
			return;
		}
		if (point == null) {
			return;
		}
		if (XXX_FastMath.hypot(point.getX() - self.getX(), point.getY() - self.getY()) < XXX_Constants.getGame().getWizardStrafeSpeed()) {
			point = pointToReach;
		}
		if (XXX_FastMath.hypot(point.getX() - self.getX(), point.getY() - self.getY()) < XXX_Constants.getGame().getWizardStrafeSpeed()) {
			return;
		}
		turnTo(self.getAngleTo(point.getX(), point.getY()), XXX_Constants.getGame().getWizardMaxTurnAngle() * XXX_Variables.turnFactor, move);
	}

	private void turnTo(double angle, double maxAngle, Move move) {
		if (currentAction.getActionType() == XXX_CurrentAction.ActionType.RUN_FROM_PROJECTILE) {
			return;
		}
		move.setTurn(XXX_Utils.updateMaxModule(angle, maxAngle));
	}

	private void findTargets() {
		targets.clear();
		double missileDamage = XXX_Utils.getSelfProjectileDamage(ProjectileType.MAGIC_MISSILE);
		treeCut = (myLineCalc == XXX_PositionMoveLine.INSTANCE &&
				XXX_Utils.unitsCountCloseToDestination(filteredWorld.getTrees(), new XXX_Point(self.getX(), self.getY())) > 0) ||
				XXX_Utils.unitsCountAtDistance(filteredWorld.getTrees(),
											   self,
											   XXX_Constants.TREES_DISTANCE_TO_CUT) >= XXX_Constants.TREES_COUNT_TO_CUT || // too much trees around
				XXX_Utils.unitsCountCloseToDestination(filteredWorld.getAllBlocksList(), pointToReach) >= 2 && // can't go throught obstacles
						XXX_Utils.unitsCountCloseToDestination(filteredWorld.getTrees(), pointToReach) > 0; // one of them - tree
		for (LivingUnit livingUnit : filteredWorld.getAimsList()) {
			if (livingUnit.getFaction() != XXX_Constants.getEnemyFaction() &&
					(livingUnit.getFaction() != Faction.NEUTRAL || !agressiveNeutralsCalcs.isMinionAgressive(livingUnit.getId())) &&
					livingUnit.getFaction() != Faction.OTHER) {
				continue;
			}
			double score;
			if (livingUnit instanceof Tree) {
				if (treeCut) {
					// distance to destination
					// distance to me
					score = XXX_Constants.CUT_REACH_POINT_DISTANCE_PTIORITY / XXX_FastMath.hypot(pointToReach.getX() - livingUnit.getX(),
																								 pointToReach.getY() - livingUnit.getY());
					score += XXX_Constants.CUT_SELF_DISTANCE_PRIORITY / XXX_FastMath.hypot(self.getX() - livingUnit.getX(), self.getY() - livingUnit.getY());
					score /= (livingUnit.getLife() + missileDamage - 1) / missileDamage;
					targets.add(new AbstractMap.SimpleEntry<>(score - XXX_Constants.CUT_REACH_POINT_DISTANCE_PTIORITY, livingUnit));
				}
				continue;
			}
			score = XXX_Constants.LOW_AIM_SCORE;
			double tmp = (livingUnit.getMaxLife() - livingUnit.getLife()) / (double) livingUnit.getMaxLife();
			score += tmp * tmp;
			if (livingUnit instanceof Minion) {
				if (((Minion) livingUnit).getType() == MinionType.FETISH_BLOWDART) {
					score *= XXX_Constants.FETISH_AIM_PROIRITY;
				} else {
					score *= XXX_Constants.ORC_AIM_PROIRITY;
				}
				if (livingUnit.getFaction() == Faction.NEUTRAL) {
					score *= XXX_Constants.NEUTRAL_FACTION_AIM_PROIRITY;
				}
			} else if (livingUnit instanceof Wizard) {
				score *= XXX_Constants.WIZARD_AIM_PROIRITY;
				if (XXX_Utils.wizardHasStatus((Wizard) livingUnit, StatusType.SHIELDED)) {
					score *= XXX_Constants.SHIELDENED_AIM_PRIORITY;
				}
				if (XXX_Utils.wizardHasStatus((Wizard) livingUnit, StatusType.EMPOWERED)) {
					score *= XXX_Constants.EMPOWERED_AIM_PRIORITY;
				}
				if (XXX_Utils.wizardHasStatus((Wizard) livingUnit, StatusType.HASTENED)) {
					score *= XXX_Constants.HASTENED_AIM_PRIORITY;
				}
			} else if (livingUnit instanceof Building) {
				score *= XXX_Constants.BUILDING_AIM_PROIRITY;
			}
			targets.add(new AbstractMap.SimpleEntry<>(score, livingUnit));
		}
		Collections.sort(targets, XXX_Utils.AIM_SORT_COMPARATOR);
		for (Map.Entry<Double, CircularUnit> doubleCircularUnitEntry : targets) {
			target = doubleCircularUnitEntry.getValue();
			XXX_Point pointA = new XXX_Point(self.getX(), self.getY());
			XXX_Point pointB = new XXX_Point(target.getX(), target.getY());

			for (Tree tree : filteredWorld.getTrees()) {
				if (tree == target) {
					continue;
				}
				double distance = XXX_Utils.distancePointToSegment(new XXX_Point(tree.getX(), tree.getY()), pointA, pointB);
				if (distance < tree.getRadius() + XXX_Constants.getGame().getMagicMissileRadius()) {
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
			distance = XXX_FastMath.hypot(self.getX() - meleeTarget.getX(), self.getY() - meleeTarget.getY());
			if (distance + .001 < meleeTarget.getRadius() + XXX_Constants.getGame().getStaffRange()) {
				break;
			}
			meleeTarget = null;
		}
	}

	protected void moveTo(int pointIdx, Move move, boolean run) {
		if (wayPoints.size() < 2) {
			return;
		}
		XXX_ScanMatrixItem point = wayPoints.get(pointIdx).getPoint();
		moveToPoint = point;
		double distance = XXX_FastMath.hypot(self, point.getX(), point.getY());
		angle = self.getAngleTo(point.getX(), point.getY());

		XXX_AccAndSpeedWithFix accAndStrafe = XXX_AccAndSpeedWithFix.getAccAndSpeedByAngle(angle, distance);
		move.setSpeed(accAndStrafe.getSpeed());
		move.setStrafeSpeed(accAndStrafe.getStrafe());

		minAngle = 0.;
		maxAngle = 0.;
		targetAngle = 0.;

		if (run) { // look forward
			int idx = pointIdx + 1;
			while (idx < wayPoints.size() && pointIdx + 5 >= idx) {
				XXX_Point wayPoint = wayPoints.get(idx++).getPoint();
				targetAngle = XXX_Utils.normalizeAngle(self.getAngleTo(wayPoint.getX(), wayPoint.getY()) - angle);
				if (targetAngle < minAngle) {
					minAngle = targetAngle;
				} else if (targetAngle > maxAngle) {
					maxAngle = targetAngle;
				}
			}
			minAngle = Math.max(-Math.PI, minAngle - XXX_Constants.RUN_ANGLE_EXPAND);
			maxAngle = Math.min(Math.PI, maxAngle + XXX_Constants.RUN_ANGLE_EXPAND);
			targetAngle = XXX_Utils.normalizeAngle(targetAngle + angle);
		} else { // look on path
			int step = Math.max(pointIdx / 5, 1);
			int idx = pointIdx - step;
			while (idx > 0) {
				XXX_Point wayPoint = wayPoints.get(idx).getPoint();
				targetAngle = XXX_Utils.normalizeAngle(self.getAngleTo(wayPoint.getX(), wayPoint.getY()) - angle);
				if (targetAngle < minAngle) {
					minAngle = targetAngle;
				} else if (targetAngle > maxAngle) {
					maxAngle = targetAngle;
				}
				idx -= step;
			}
			targetAngle = angle;
		}
		XXX_Point changePosition;
		if (Math.abs(XXX_Utils.normalizeAngle(maxAngle - minAngle)) > XXX_Constants.MOVE_ANGLE_PRECISE) {
			changePosition = accAndStrafe.getCoordChange(self.getAngle());
			testScanItem.setPoint(self.getX() + changePosition.getX(), self.getY() + changePosition.getY());
			XXX_Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, unitScoreCalculation, enemyFound);

			double bestDanger = testScanItem.getAllDangers();
			double bestScore = testScanItem.getTotalScore(self);
			double newAngle;
			double closestAngle = Math.abs(XXX_Utils.normalizeAngle(angle - targetAngle));
			XXX_Point bestPosition = testScanItem.clonePoint();
			XXX_AccAndSpeedWithFix bestMove = accAndStrafe;
			double itAngle = minAngle;
			for (; maxAngle - itAngle > XXX_Constants.MOVE_ANGLE_PRECISE; itAngle += XXX_Constants.MOVE_ANGLE_PRECISE) {
				newAngle = XXX_Utils.normalizeAngle(angle + itAngle);
				accAndStrafe = XXX_AccAndSpeedWithFix.getAccAndSpeedByAngle(newAngle, 100.);
				changePosition = accAndStrafe.getCoordChange(self.getAngle());
				testScanItem.setPoint(self.getX() + changePosition.getX(), self.getY() + changePosition.getY());
				XXX_Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, unitScoreCalculation, enemyFound);
				if (!testScanItem.isAvailable() || bestDanger < testScanItem.getAllDangers()) { //run???
					continue;
				}
				if (testScanItem.getAllDangers() == bestDanger) {
					if (bestScore > testScanItem.getTotalScore(self)) {
						continue;
					}
					if (bestScore == testScanItem.getTotalScore(self) && Math.abs(XXX_Utils.normalizeAngle(newAngle - targetAngle)) > closestAngle) {
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

	private boolean testPointDirectAvailable(XXX_Point point) {
		double distance;
		XXX_Point from = new XXX_Point(self.getX(), self.getY());
		XXX_Point to = new XXX_Point(point.getX(), point.getY());
		for (CircularUnit circularUnit : filteredWorld.getAllBlocksList()) {
			distance = XXX_Utils.distancePointToSegment(new XXX_Point(circularUnit.getX(), circularUnit.getY()), from, to);
			if (distance < self.getRadius() + circularUnit.getRadius() + XXX_Constants.STUCK_FIX_RADIUS_ADD) {
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
			XXX_ScanMatrixItem item = null;
			for (int i = 0; i != scan_matrix.length; ++i) {
				for (int j = 0; j != scan_matrix[0].length; ++j) {
					XXX_ScanMatrixItem scanMatrixItem = scan_matrix[i][j];
					if (!scanMatrixItem.isAvailable()) {
						continue;
					}
					if (item == null || item.getTotalScore(self) < scanMatrixItem.getTotalScore(self)) {
						item = scanMatrixItem;
					}
				}
			}
			if (testPointDirectAvailable(item)) {
				wayPoints.add(new XXX_WayPoint(1, scan_matrix[XXX_Constants.CURRENT_PT_X][XXX_Constants.CURRENT_PT_Y], null));
				wayPoints.add(new XXX_WayPoint(2, item, wayPoints.get(0)));
				return;
			}
		}
		queue.add(new XXX_WayPoint(1, scan_matrix[XXX_Constants.CURRENT_PT_X][XXX_Constants.CURRENT_PT_Y], null));
		int nextDistanceFromStart;
		double newScoresOnWay;
		double newDangerOnWay;
		while (!queue.isEmpty()) {
			XXX_WayPoint currentPoint = queue.poll();
			if (currentPoint.getPoint().getWayPoint() != currentPoint) {
				continue;
			}
			nextDistanceFromStart = currentPoint.getDistanceFromStart() + 1;
			for (XXX_ScanMatrixItem scanMatrixItem : currentPoint.getPoint().getNeighbours()) {
				if (!scanMatrixItem.isAvailable()) {
					continue;
				}
				if (scanMatrixItem.getWayPoint() == null) {
					queue.add(new XXX_WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
					continue;
				}
				XXX_WayPoint wayPointToCompare = scanMatrixItem.getWayPoint();
				newScoresOnWay = scanMatrixItem.getTotalScore(self) - XXX_Variables.maxDangerMatrixScore + currentPoint.getScoresOnWay();

				if (newScoresOnWay == wayPointToCompare.getScoresOnWay()) {
					if (wayPointToCompare.getDistanceFromStart() == nextDistanceFromStart) {
						newDangerOnWay = scanMatrixItem.getAllDangers() + currentPoint.getDangerOnWay();
						if (newDangerOnWay < wayPointToCompare.getDangerOnWay()) {
							queue.add(new XXX_WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
						}
					} else if (nextDistanceFromStart < wayPointToCompare.getDistanceFromStart()) {
						queue.add(new XXX_WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
					}
				} else if (newScoresOnWay > wayPointToCompare.getDangerOnWay()) {
					queue.add(new XXX_WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
				}
			}
		}
		getBestMovePoint();
	}

	protected void getBestMovePoint() {
		XXX_ScanMatrixItem best = null;
		double score = 0;
		double tmpScore;

		for (int i = scan_matrix.length - 1; i != -1; --i) {
			for (int j = 0; j != scan_matrix[0].length; ++j) {
				XXX_ScanMatrixItem newScanMatrixItem = scan_matrix[i][j];
				if (newScanMatrixItem.getWayPoint() == null) {
					continue;
				}
				if (best == null) {
					best = newScanMatrixItem;
					score = newScanMatrixItem.getTotalScore(self) +
							newScanMatrixItem.getWayPoint().getScoresOnWay() / newScanMatrixItem.getWayPoint().getDistanceFromStart() + XXX_Variables.maxDangerMatrixScore;
					continue;
				}
				tmpScore = newScanMatrixItem.getTotalScore(self) +
						newScanMatrixItem.getWayPoint().getScoresOnWay() / newScanMatrixItem.getWayPoint().getDistanceFromStart()
						+ XXX_Variables.maxDangerMatrixScore;
				if (tmpScore > score) {
					score = tmpScore;
					best = newScanMatrixItem;
				}
			}
		}
		pointToReach = best;
		finalizeQueue(pointToReach.getWayPoint());
	}

	private void finalizeQueue(XXX_WayPoint wayPoint) {
		do {
			wayPoints.add(wayPoint);
			wayPoint = wayPoint.getPrev();
		} while (wayPoint != null);
		Collections.reverse(wayPoints);
	}

	private void calcMatrixDanger() {
		double dxFwd = Math.cos(direction) * XXX_Constants.MOVE_SCAN_STEP;
		double dyFwd = Math.sin(direction) * XXX_Constants.MOVE_SCAN_STEP;

		double dxSide = Math.cos(direction - Math.PI * .5) * XXX_Constants.MOVE_SCAN_STEP;
		double dySide = Math.sin(direction - Math.PI * .5) * XXX_Constants.MOVE_SCAN_STEP;
		for (int i = 0; i != scan_matrix.length; ++i) {
			for (int j = 0; j != scan_matrix[0].length; ++j) {
				scan_matrix[i][j].setPoint(self.getX() + dxFwd * (i - XXX_Constants.CURRENT_PT_X) + dxSide * (XXX_Constants.CURRENT_PT_Y - j),
										   self.getY() + dyFwd * (i - XXX_Constants.CURRENT_PT_X) + dySide * (XXX_Constants.CURRENT_PT_Y - j));
			}
			XXX_Utils.calcTilesAvailable(filteredWorld.getAllBlocksList(), scan_matrix[i]);
			for (int j = 0; j != scan_matrix[0].length; ++j) {
				XXX_ScanMatrixItem item = scan_matrix[i][j];
				if (!item.isAvailable()) {
					continue;
				}

				double distanceTo = myLineCalc.calcLineDistanceOtherDanger(item);
				if (distanceTo > 0.) {
					item.addOtherDanger(distanceTo);
				}

				if (XXX_SpawnPoint.checkSpawnPoints()) {
					for (XXX_SpawnPoint spawnPoint : XXX_Constants.SPAWN_POINTS) {
						if (spawnPoint.isPointInDanger(item.getX(), item.getY())) {
							item.addOtherDanger(XXX_Constants.SPAWN_POINT_DANGER);
						}
					}
				}

				if (currentAction.getActionType() == XXX_CurrentAction.ActionType.MOVE_TO_POSITION) {
					if (XXX_FastMath.hypot(self.getX() - myLineCalc.getFightPoint().getX(), self.getY() - myLineCalc.getFightPoint().getY()) > 200) {
						item.addOtherBonus(item.getForwardDistanceDivision() * 70);
					}
				} else if (!enemyFound) {
					item.addOtherBonus(item.getForwardDistanceDivision() * .0001);
				}
			}
		}


		for (Bonus bonus : filteredWorld.getBonuses()) {
			if (XXX_FastMath.hypot(self.getX() - bonus.getX(), self.getY() - bonus.getY()) > XXX_Constants.getFightDistanceFilter()) {
				continue;
			}
			XXX_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(bonus.getId());

			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new XXX_Point(bonus.getX(), bonus.getY()));
			}
		}

		if (XXX_Utils.getTicksToBonusSpawn(world.getTickIndex()) < 250) {
			for (int i = 0; i != XXX_BonusesPossibilityCalcs.BONUSES_POINTS.length; ++i) {
				if (XXX_FastMath.hypot(self, XXX_BonusesPossibilityCalcs.BONUSES_POINTS[i]) > XXX_Constants.getFightDistanceFilter()) {
					continue;
				}

				XXX_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(i - 5);

				for (int j = 0; j != scan_matrix.length; ++j) {
					applyScoreForLine(scan_matrix[j], structure, XXX_BonusesPossibilityCalcs.BONUSES_POINTS[i]);
				}
			}
		}

		if (!enemyFound) {
			return;
		}

		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == XXX_Constants.getCurrentFaction()) {
				continue;
			}

			XXX_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(minion.getId());

			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new XXX_Point(minion.getX(), minion.getY()));
			}
		}

		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == XXX_Constants.getCurrentFaction()) {
				continue;
			}
			XXX_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(wizard.getId());
			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new XXX_Point(wizard.getX(), wizard.getY()));
			}
		}

		for (Building building : filteredWorld.getBuildings()) {
			if (building.getFaction() == XXX_Constants.getCurrentFaction()) {
				continue;
			}
			XXX_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(building.getId());

			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new XXX_Point(building.getX(), building.getY()));
			}
		}
	}

	public void applyScoreForLine(XXX_ScanMatrixItem[] items, XXX_ScoreCalcStructure structure, XXX_Point point) {
		double distance = XXX_Utils.distancePointToSegment(point, items[0], items[items.length - 1]);
		if (distance > structure.getMaxScoreDistance()) {
			return;
		}
		double itemDistance;
		for (int i = 0; i != items.length; ++i) {
			if (!items[i].isAvailable()) {
				continue;
			}
			itemDistance = XXX_FastMath.hypot(items[i].getX() - point.getX(), items[i].getY() - point.getY());
			if (itemDistance <= structure.getMaxScoreDistance()) {
				structure.applyScores(items[i], itemDistance);
			}
		}
	}

	public boolean isMeNearestWizard(XXX_Point point, boolean includeEnemies) {
		double distanceToMe = XXX_FastMath.hypot(self.getX() - point.getX(),
												 self.getY() - point.getY()) * XXX_Constants.NEAREST_TO_BONUS_CALCULATION_OTHER_MULT;
		if (includeEnemies) {
			for (XXX_WizardPhantom phantom : enemyPositionCalc.getDetectedWizards().values()) {
				double distance = XXX_FastMath.hypot(phantom.getPosition().getX() - point.getX(), phantom.getPosition().getY() - point.getY());
				if (!phantom.isUpdated()) {
					distance -= (world.getTickIndex() - phantom.getLastSeenTick()) * XXX_Constants.MAX_WIZARDS_FORWARD_SPEED;
				}
				if (distanceToMe > distance) {
					return false;
				}
			}
		}

		for (Wizard wizard : world.getWizards()) {
			if (wizard.getFaction() != XXX_Constants.getCurrentFaction()) {
				continue;
			}
			if (XXX_FastMath.hypot(wizard.getX() - point.getX(), wizard.getY() - point.getY()) < distanceToMe) {
				return false;
			}
		}
		return true;
	}


}
