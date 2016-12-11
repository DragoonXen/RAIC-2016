import model.ActionType;
import model.Bonus;
import model.Building;
import model.CircularUnit;
import model.Game;
import model.Minion;
import model.Move;
import model.Projectile;
import model.ProjectileType;
import model.StatusType;
import model.Wizard;
import model.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by by.dragoon on 11/8/16.
 */
public class YYY_StrategyImplement implements Strategy {

	protected World world;
	protected Wizard self;

	protected YYY_BaseLine myLineCalc;
	protected YYY_BaseLine lastFightLine;
	protected double direction;

	protected YYY_FilteredWorld filteredWorld;

	protected final YYY_ScanMatrixItem[][] scan_matrix = YYY_Utils.createScanMatrix();

	protected int lastTick;

	protected YYY_ScanMatrixItem pointToReach;
	protected YYY_Point prevPointToReach;

	private YYY_ScanMatrixItem testScanItem = new YYY_ScanMatrixItem(0, 0, 1.);

	protected ArrayList<YYY_WayPoint> wayPoints = new ArrayList<>();

	protected YYY_Point moveToPoint;

	private PriorityQueue<YYY_WayPoint> queue = new PriorityQueue<>();

	protected double minAngle = 0.;
	protected double maxAngle = 0.;
	protected double angle = 0.;
	protected double targetAngle = 0.;

	protected HashMap<Long, Double> projectilesDTL = new HashMap<>(); //store
	protected YYY_CurrentAction currentAction = new YYY_CurrentAction();

	protected YYY_EnemyPositionCalc enemyPositionCalc = new YYY_EnemyPositionCalc();

	protected YYY_BonusesPossibilityCalcs bonusesPossibilityCalcs = new YYY_BonusesPossibilityCalcs();

	protected YYY_AgressiveNeutralsCalcs agressiveNeutralsCalcs = new YYY_AgressiveNeutralsCalcs();
	protected YYY_UnitScoreCalculation unitScoreCalculation = new YYY_UnitScoreCalculation();
	protected YYY_TeammateIdsContainer teammateIdsContainer = new YYY_TeammateIdsContainer();

	protected boolean treeCut;
	protected boolean turnFixed;
	protected boolean goToBonusActivated = false;
	protected boolean moveToLineActivated = false;
	protected YYY_FightStatus fightStatus;

	protected YYY_TargetFinder targetFinder = new YYY_TargetFinder();

	protected YYY_WizardsInfo wizardsInfo = new YYY_WizardsInfo();

	protected int stuck;
	protected YYY_Point prevPoint = new YYY_Point();
	protected boolean manaFree = false;

	public YYY_StrategyImplement(Wizard self) {
		myLineCalc = YYY_Constants.getLine(YYY_Utils.getDefaultMyLine((int) self.getId()));
		lastFightLine = myLineCalc;
		prevPointToReach = new YYY_Point(self.getX(), self.getY());
	}

	public void move(Wizard self, World world, Game game, Move move) {
		YYY_Variables.world = world;
		agressiveNeutralsCalcs.updateMap(world);
		enemyPositionCalc.updatePositions(world);
		wizardsInfo.updateData(world, enemyPositionCalc);
		bonusesPossibilityCalcs.updateTick(world, enemyPositionCalc);
		teammateIdsContainer.updateTeammatesIds(world);
		YYY_SkillsLearning.updateSkills(self, enemyPositionCalc, world.getWizards(), move);
		fightStatus = YYY_FightStatus.NO_ENEMY;
		treeCut = false;
		moveToPoint = null;
		manaFree = true;
		prevPointToReach = pointToReach == null ? prevPointToReach : pointToReach.clonePoint();
		pointToReach = null;
		minAngle = 0.;
		wayPoints.clear();
		maxAngle = 0.;
		angle = 0.;
		targetAngle = 0.;
		turnFixed = false;
		if (prevPoint.getX() == self.getX() && prevPoint.getY() == self.getY()) {
			++stuck;
		} else {
			stuck = 0;
			prevPoint.update(self.getX(), self.getY());
		}
		YYY_Variables.prevActionType = currentAction.getActionType();

		YYY_Variables.self = self;
		this.world = world;
		this.self = self;
		YYY_SpawnPoint.updateTick(world.getTickIndex());

		for (YYY_BaseLine baseLine : YYY_Constants.getLines()) {
			baseLine.updateFightPoint(world, enemyPositionCalc);
		}

		myLineCalc = YYY_Utils.fightLineSelect(lastFightLine, world, enemyPositionCalc, self);
		lastFightLine = myLineCalc;

		lastTick = world.getTickIndex();

		direction = myLineCalc.getMoveDirection(self);
		filteredWorld = YYY_Utils.filterWorld(world,
											  new YYY_Point(self.getX() + Math.cos(direction) * YYY_Constants.MOVE_SCAN_FIGURE_CENTER,
															self.getY() + Math.sin(direction) * YYY_Constants.MOVE_SCAN_FIGURE_CENTER),
											  enemyPositionCalc.getBuildingPhantoms(), teammateIdsContainer);
		currentAction.setActionType(YYY_CurrentAction.ActionType.FIGHT); // default state
		updateFightStatus();
		unitScoreCalculation.updateScores(filteredWorld, self, fightStatus, agressiveNeutralsCalcs);
		if (isInDanger()) {
			fightStatus = YYY_FightStatus.IN_DANGER;
			direction = YYY_Utils.normalizeAngle(direction + Math.PI);
			filteredWorld = YYY_Utils.filterWorld(world,
												  new YYY_Point(self.getX() + Math.cos(direction) * YYY_Constants.MOVE_SCAN_FIGURE_CENTER,
																self.getY() + Math.sin(direction) * YYY_Constants.MOVE_SCAN_FIGURE_CENTER),
												  enemyPositionCalc.getBuildingPhantoms(), teammateIdsContainer);
		}


		currentAction.setActionType(YYY_CurrentAction.ActionType.FIGHT); // default state
		updateFightStatus();
		updateProjectilesDTL(filteredWorld.getProjectiles());

		unitScoreCalculation.updateScores(filteredWorld, self, fightStatus, agressiveNeutralsCalcs);
		evade(move, checkHitByProjectilePossible());

		targetFinder.updateTargets(filteredWorld, myLineCalc, prevPointToReach, agressiveNeutralsCalcs, stuck);

		makeShot(move);
		if (currentAction.getActionType() == YYY_CurrentAction.ActionType.FIGHT) {
			int ticksToBonusSpawn = YYY_Utils.getTicksToBonusSpawn(world.getTickIndex());
			if (goToBonusActivated) {
				if (YYY_PositionMoveLine.INSTANCE.getPositionToMove().getX() > 2000) {
					if ((bonusesPossibilityCalcs.getScore()[1] < .1 && ticksToBonusSpawn > YYY_Constants.MAX_TICKS_RUN_TO_BONUS) ||
							!isMeNearestWizard(YYY_BonusesPossibilityCalcs.BONUSES_POINTS[1], false)) {
						goToBonusActivated = false;
						moveToLineActivated = true;
					}
				} else {
					if ((bonusesPossibilityCalcs.getScore()[0] < .1 && ticksToBonusSpawn > YYY_Constants.MAX_TICKS_RUN_TO_BONUS) ||
							!isMeNearestWizard(YYY_BonusesPossibilityCalcs.BONUSES_POINTS[0], false)) {
						goToBonusActivated = false;
						moveToLineActivated = true;
					}
				}
			}

			if (!goToBonusActivated) {
				double distanceToBonusA = YYY_FastMath.hypot(self.getX() - YYY_BonusesPossibilityCalcs.BONUSES_POINTS[0].getX(),
															 self.getY() - YYY_BonusesPossibilityCalcs.BONUSES_POINTS[0].getY()) -
						self.getRadius() -
						game.getBonusRadius();
				double ticksRunToBonusA = distanceToBonusA / (YYY_Constants.getGame().getWizardForwardSpeed() * wizardsInfo.getMe().getMoveFactor()) *
						YYY_Constants.TICKS_BUFFER_RUN_TO_BONUS;
				if (ticksRunToBonusA < YYY_Constants.MAX_TICKS_RUN_TO_BONUS &&
						(ticksRunToBonusA >= ticksToBonusSpawn || bonusesPossibilityCalcs.getScore()[0] > YYY_Constants.BONUS_POSSIBILITY_RUN) &&
						isMeNearestWizard(YYY_BonusesPossibilityCalcs.BONUSES_POINTS[0], true)) { // goto bonus 1
					goToBonusActivated = true;
					moveToLineActivated = false;
					currentAction.setActionType(YYY_CurrentAction.ActionType.MOVE_TO_POSITION);
					YYY_PositionMoveLine.INSTANCE.updatePointToMove(YYY_BonusesPossibilityCalcs.BONUSES_POINTS[0]);
				}

				double distanceToBonusB = YYY_FastMath.hypot(self.getX() - YYY_BonusesPossibilityCalcs.BONUSES_POINTS[1].getX(),
															 self.getY() - YYY_BonusesPossibilityCalcs.BONUSES_POINTS[1].getY()) -
						self.getRadius() -
						game.getBonusRadius();
				double ticksRunToBonusB = distanceToBonusB / (YYY_Constants.getGame().getWizardForwardSpeed() * wizardsInfo.getMe().getMoveFactor()) *
						YYY_Constants.TICKS_BUFFER_RUN_TO_BONUS;

				if (ticksRunToBonusB < YYY_Constants.MAX_TICKS_RUN_TO_BONUS &&
						(ticksRunToBonusB >= ticksToBonusSpawn || bonusesPossibilityCalcs.getScore()[1] > YYY_Constants.BONUS_POSSIBILITY_RUN) &&
						isMeNearestWizard(YYY_BonusesPossibilityCalcs.BONUSES_POINTS[1], true)) { // goto bonus 1
					goToBonusActivated = true;
					moveToLineActivated = false;
					if (!(currentAction.getActionType() == YYY_CurrentAction.ActionType.MOVE_TO_POSITION && ticksRunToBonusA < ticksRunToBonusB)) {
						YYY_PositionMoveLine.INSTANCE.updatePointToMove(YYY_BonusesPossibilityCalcs.BONUSES_POINTS[1]);
					}
					currentAction.setActionType(YYY_CurrentAction.ActionType.MOVE_TO_POSITION);
				}
			}

			if (goToBonusActivated) {
				currentAction.setActionType(YYY_CurrentAction.ActionType.MOVE_TO_POSITION);
			} else if (!moveToLineActivated) {
				if (myLineCalc.getDistanceTo(self) > 300.) {
					moveToLineActivated = true;
				}
			}

			if (moveToLineActivated) {
				myLineCalc = YYY_Utils.fightLineSelect(lastFightLine, world, enemyPositionCalc, self);
				lastFightLine = myLineCalc;
				if (YYY_FastMath.hypot(myLineCalc.getFightPoint().getX() - self.getX(), myLineCalc.getFightPoint().getY() - self.getY()) > 500. &&
						myLineCalc.getDistanceTo(self) > YYY_Constants.getTopLine().getLineDistance()) {
					currentAction.setActionType(YYY_CurrentAction.ActionType.MOVE_TO_POSITION);
					YYY_PositionMoveLine.INSTANCE.updatePointToMove(myLineCalc.getPreFightPoint());
				} else {
					moveToLineActivated = false;
				}
			}

			if (currentAction.getActionType() == YYY_CurrentAction.ActionType.MOVE_TO_POSITION) {
				myLineCalc = YYY_PositionMoveLine.INSTANCE;
				direction = myLineCalc.getMoveDirection(self);

				filteredWorld = YYY_Utils.filterWorld(world,
													  new YYY_Point(self.getX() + Math.cos(direction) * YYY_Constants.MOVE_SCAN_FIGURE_CENTER,
																	self.getY() + Math.sin(direction) * YYY_Constants.MOVE_SCAN_FIGURE_CENTER),
													  enemyPositionCalc.getBuildingPhantoms(), teammateIdsContainer);
				updateFightStatus();
				unitScoreCalculation.updateScores(filteredWorld, self, fightStatus, agressiveNeutralsCalcs);
			}

			if (goToBonusActivated && YYY_FastMath.hypot(self, YYY_PositionMoveLine.INSTANCE.getPositionToMove()) <= self.getRadius() +
					game.getBonusRadius() +
					game.getWizardForwardSpeed() * wizardsInfo.getMe().getMoveFactor() + .1) {
				boolean bonusOnPlace = bonusesPossibilityCalcs.getScore()[YYY_PositionMoveLine.INSTANCE.getPositionToMove().getX() > 2000 ? 1 : 0] > .9;
				if (!bonusOnPlace) {
					YYY_Point movePoint = YYY_PositionMoveLine.INSTANCE.getPositionToMove().negateCopy(self);
					movePoint.update(-movePoint.getX(), -movePoint.getY());
					currentAction.setActionType(YYY_CurrentAction.ActionType.EVADE_PROJECTILE);
					double needDistance = game.getBonusRadius() + self.getRadius() - .5;
					if (ticksToBonusSpawn < 2) {
						needDistance += 1;
					}
					movePoint.fixVectorLength(needDistance);
					movePoint.add(YYY_PositionMoveLine.INSTANCE.getPositionToMove());
					YYY_AccAndSpeedWithFix accAndSpeedByAngle = YYY_AccAndSpeedWithFix.getAccAndSpeedByAngle(YYY_Utils.normalizeAngle(self.getAngleTo(movePoint.getX(),
																																					  movePoint.getY())),
																											 self.getDistanceTo(movePoint.getX(),
																																movePoint.getY()),
																											 wizardsInfo.getMe().getMoveFactor());
					move.setSpeed(accAndSpeedByAngle.getSpeed());
					move.setStrafeSpeed(accAndSpeedByAngle.getStrafe());
					moveToPoint = movePoint;
				}
			}
		}

		if (currentAction.getActionType().moveCalc) {
			calcMatrixDanger();
			YYY_Variables.maxDangerMatrixScore = Double.NEGATIVE_INFINITY;
			for (int i = 0; i != scan_matrix.length; ++i) {
				for (int j = 0; j != scan_matrix[0].length; ++j) {
					if (!scan_matrix[i][j].isAvailable()) {
						continue;
					}
					YYY_Variables.maxDangerMatrixScore = Math.max(scan_matrix[i][j].getTotalScore(self), YYY_Variables.maxDangerMatrixScore);
				}
			}
			findAWay();
			if (!wayPoints.isEmpty()) {
				int lastPointGoTo = 1;
				double dangerAtStart = wayPoints.get(0).getPoint().getAllDangers() * YYY_Constants.DANGER_AT_START_MULT_RUN;
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

		if (!turnFixed) {
			turnTo(moveToPoint, move);
		}
	}

	private void updateProjectilesDTL(Projectile[] projectiles) {
		YYY_Variables.projectiles.clear();
		for (Projectile projectile : projectiles) {
			YYY_Variables.projectiles.add(projectile.getId());
		}
		for (Long projectileId : new ArrayList<>(projectilesDTL.keySet())) {
			if (!YYY_Variables.projectiles.contains(projectileId)) {
				projectilesDTL.remove(projectileId);
			}
		}

		for (Projectile projectile : projectiles) {
			if (projectilesDTL.containsKey(projectile.getId())) {
				projectilesDTL.put(projectile.getId(),
								   projectilesDTL.get(projectile.getId()) - YYY_Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()]);
				continue;
			}
			long castUnit = projectile.getOwnerUnitId();
			double castRange = castUnit <= 10 ?
					YYY_Variables.wizardsInfo.getWizardInfo(castUnit).getCastRange() - YYY_Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()]
					: YYY_Constants.getGame().getFetishBlowdartAttackRange() - YYY_Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()];
			projectilesDTL.put(projectile.getId(), castRange);
		}
	}

	protected double checkHitByProjectilePossible() {
		double maxStep = wizardsInfo.getMe().getMoveFactor() * YYY_Constants.getGame().getWizardForwardSpeed();
		YYY_Point self = new YYY_Point(this.self.getX(), this.self.getY());
		double distance;
		double sumDamage = 0.;
		double projectileRadius;
		for (Projectile projectile : filteredWorld.getProjectiles()) {
			YYY_Point projectileStart = new YYY_Point(projectile.getX(), projectile.getY());
			YYY_Point projectileDestination = new YYY_Point(projectile.getSpeedX(), projectile.getSpeedY());
			projectileDestination.fixVectorLength(projectilesDTL.get(projectile.getId()));
			projectileDestination.add(projectileStart);
			distance = YYY_Utils.distancePointToSegment(self, projectileStart, projectileDestination);
			projectileRadius = projectile.getRadius();
			if (projectile.getType() == ProjectileType.FIREBALL) {
				projectileRadius = YYY_Constants.getGame().getFireballExplosionMinDamageRange();
			}
			if (distance < maxStep + this.self.getRadius() + projectileRadius) {
				sumDamage += YYY_Utils.getProjectileDamage(projectile, distance - maxStep);
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
		YYY_Point position = new YYY_Point(self.getX(), self.getY());
		YYY_Point bestPosition = position;

		YYY_UnitScoreCalculationTickSupport unitScoreCalculationTickSupport = new YYY_UnitScoreCalculationTickSupport(unitScoreCalculation);
		YYY_Utils.fillProjectilesSim(filteredWorld, projectilesDTL);
		int ticks = 0;
		while (!YYY_Variables.projectilesSim.isEmpty()) {
			testScanItem.setPoint(self.getX(), self.getY());
			YYY_Utils.calcTileScore(testScanItem,
									filteredWorld,
									myLineCalc,
									self,
									unitScoreCalculationTickSupport.getScores(filteredWorld, self, fightStatus, agressiveNeutralsCalcs, ticks),
									fightStatus);
			bestScore = testScanItem.getTotalScore(self);
			bestDangerOnWay += testScanItem.getAllDangers();
			bestDamage += YYY_Utils.checkProjectiveCollision(position, ticks++);
		}
		bestDamage += YYY_Utils.finalizeFireballsDamage();

		double currScore;
		double currDamage;
		double currDangerOnWay;
		int hastenedTicks = YYY_Utils.wizardStatusTicks(self, StatusType.HASTENED);
		int currHastenedTicks;
		double moveFactor = wizardsInfo.getMe().getMoveFactor();
		double moveAngle;
		YYY_Point moveVector;
		boolean stuck;
		for (int i = 0; i != YYY_Constants.EVADE_CALCULATIONS_COUNT; ++i) {
			currScore = 0.;
			currDamage = 0.;
			currDangerOnWay = 0.;
			position = new YYY_Point(self.getX(), self.getY());
			YYY_Utils.fillProjectilesSim(filteredWorld, projectilesDTL);
			moveAngle = YYY_Utils.normalizeAngle(i * YYY_Constants.EVADE_DEGREE_STEP);
			currHastenedTicks = hastenedTicks;
			moveVector = new YYY_Point(Math.cos(moveAngle) * moveFactor * YYY_Constants.getGame().getWizardStrafeSpeed(),
									   Math.sin(moveAngle) * moveFactor * YYY_Constants.getGame().getWizardStrafeSpeed());
			ticks = 0;
			stuck = false;
			while (!YYY_Variables.projectilesSim.isEmpty()) {
				if (!stuck) {
					position.add(moveVector);
					if (--currHastenedTicks == 0) {
						moveVector.fixVectorLength((moveFactor - YYY_Constants.getGame().getHastenedMovementBonusFactor()) * YYY_Constants.getGame().getWizardStrafeSpeed());
					}
					testScanItem.setPoint(position.getX(), position.getY());
					YYY_Utils.calcTileScore(testScanItem,
											filteredWorld,
											myLineCalc,
											self,
											unitScoreCalculationTickSupport.getScores(filteredWorld, self, fightStatus, agressiveNeutralsCalcs, ticks),
											fightStatus);
					if (!testScanItem.isAvailable()) {
						stuck = true;
						position.negate(moveVector);
					}
					if (!stuck) {
						currScore = testScanItem.getTotalScore(self);
						currDangerOnWay += testScanItem.getAllDangers();
					}
				}
				currDamage += YYY_Utils.checkProjectiveCollision(position, ticks++);
			}
			currDamage += YYY_Utils.finalizeFireballsDamage();
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
		double bestEvadeDamage = bestDamage;
		YYY_Point bestEvadePosition = bestPosition;
		if (bestDamage > 0.) {
			double curentAngle;
			double currMoveFactor;
			YYY_AccAndSpeedWithFix accAndSpeed;
			YYY_Point positionChange;
			for (int i = 0; i != YYY_Constants.EVADE_CALCULATIONS_COUNT; ++i) {
				currScore = 0.;
				currDamage = 0.;
				currDangerOnWay = 0.;
				position = new YYY_Point(self.getX(), self.getY());
				YYY_Utils.fillProjectilesSim(filteredWorld, projectilesDTL);
				moveAngle = YYY_Utils.normalizeAngle(i * YYY_Constants.EVADE_DEGREE_STEP);
				curentAngle = YYY_Utils.normalizeAngle(self.getAngle());
				currHastenedTicks = hastenedTicks;
				currMoveFactor = moveFactor;
				ticks = 0;
				while (!YYY_Variables.projectilesSim.isEmpty()) {
					accAndSpeed = YYY_AccAndSpeedWithFix.getAccAndSpeedByAngle(YYY_Utils.normalizeAngle(moveAngle - curentAngle), 100., currMoveFactor);
					positionChange = accAndSpeed.getCoordChange(curentAngle);
					position.add(positionChange);
					curentAngle += YYY_Utils.updateMaxModule(YYY_Utils.normalizeAngle(moveAngle - curentAngle), // angle to turn
															 currHastenedTicks > 0 ?
																	 wizardsInfo.getMe().getTurnFactor() * YYY_Constants.getGame().getWizardMaxTurnAngle() :
																	 YYY_Constants.getGame().getWizardMaxTurnAngle());
					if (--currHastenedTicks == 0) {
						currMoveFactor -= YYY_Constants.getGame().getHastenedMovementBonusFactor();
					}
					testScanItem.setPoint(position.getX(), position.getY());
					YYY_Utils.calcTileScore(testScanItem,
											filteredWorld,
											myLineCalc,
											self,
											unitScoreCalculationTickSupport.getScores(filteredWorld, self, fightStatus, agressiveNeutralsCalcs, ticks),
											fightStatus);
					if (testScanItem.isAvailable()) {
						currScore = testScanItem.getTotalScore(self);
						currDangerOnWay += testScanItem.getAllDangers();
					} else {
						position.negate(positionChange);
					}
					currDamage += YYY_Utils.checkProjectiveCollision(position, ticks++);
				}
				currDamage += YYY_Utils.finalizeFireballsDamage();
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
				bestActionType = i + YYY_Constants.EVADE_CALCULATIONS_COUNT;
				bestDamage = currDamage;
				bestDangerOnWay = currDangerOnWay;
				bestScore = currScore;
				bestPosition = position;
			}
		}
		if (bestEvadeDamage < bestDamage + 3) {
			bestDamage = bestEvadeDamage;
			bestPosition = bestEvadePosition;
			bestActionType = 0; // evade, not run
		}
		if (bestDamage < maxDamageToRecieve) { // escape from projectile
			moveAngle = self.getAngleTo(bestPosition.getX(), bestPosition.getY());
			YYY_AccAndSpeedWithFix accAndSpeedByAngle = YYY_AccAndSpeedWithFix.getAccAndSpeedByAngle(moveAngle,
																									 YYY_FastMath.hypot(self.getX() - bestPosition.getX(),
																														self.getY() - bestPosition.getY()));
			move.setSpeed(accAndSpeedByAngle.getSpeed());
			move.setStrafeSpeed(accAndSpeedByAngle.getStrafe());
			if (bestActionType < YYY_Constants.EVADE_CALCULATIONS_COUNT) {
				currentAction.setActionType(YYY_CurrentAction.ActionType.EVADE_PROJECTILE); // block move
			} else {
				turnTo(moveAngle, move);
				currentAction.setActionType(YYY_CurrentAction.ActionType.RUN_FROM_PROJECTILE); // block move and turn
			}
		}
	}

	private double getWayTotalScore(YYY_Point moveDirection, int steps, double step) {
		double angle = YYY_Utils.normalizeAngle(self.getAngleTo(moveDirection.getX(), moveDirection.getY()) + self.getAngle());
		double score = 0.;
		for (int i = 1; i <= steps; ++i) {
			double stepDistance = step * i;
			testScanItem.setPoint(self.getX() + Math.cos(angle) * stepDistance,
								  self.getY() + Math.sin(angle) * stepDistance);
			YYY_Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, unitScoreCalculation, fightStatus);
			if (!testScanItem.isAvailable()) {
				return Double.NEGATIVE_INFINITY;
			}
			score += testScanItem.getTotalScore(self);
		}
		return score;
	}

	protected YYY_TargetFinder.ShootDescription selectTarget(List<YYY_TargetFinder.ShootDescription> targets) {
		if (targets.isEmpty()) {
			return null;
		}
		YYY_TargetFinder.ShootDescription result = null;
		if (fightStatus == YYY_FightStatus.IN_DANGER || !currentAction.getActionType().moveCalc) {
			result = targets.get(targets.size() - 1);
			return result.getTicksToGo() == 0 ? result : null;
		}
		YYY_TargetFinder.ShootDescription tmp;
		double currentScore = -1000000;
		double tmpScore;
		double dangerScore;
		for (Iterator<YYY_TargetFinder.ShootDescription> iterator = targets.iterator(); iterator.hasNext(); ) {
			tmp = iterator.next();
			tmpScore = tmp.getScore() / YYY_Constants.PURSUIT_COEFF[tmp.getTicksToGo()];
			if (tmpScore > currentScore) {
				dangerScore = getWayTotalScore(tmp.getShootPoint(),
											   tmp.getTicksToGo(),
											   YYY_ShootEvasionMatrix.EVASION_MATRIX[6][0] * wizardsInfo.getMe().getMoveFactor());
				if (tmp.getActionType() == ActionType.FIREBALL) {
					dangerScore = -dangerScore * .5;
				} else {
					dangerScore = -dangerScore * .1;
				}
				if (tmp.getTicksToGo() == 0 || dangerScore < tmpScore) {
					currentScore = tmpScore;
					result = tmp;
				}
			}
		}
		return result;
	}

	private void makeShot(Move move) {
		List<YYY_TargetFinder.ShootDescription> targets = targetFinder.getMissileTargets();
		YYY_TargetFinder.ShootDescription missileShootDesc = selectTarget(targets);

		targets = targetFinder.getIceTargets();
		YYY_TargetFinder.ShootDescription iceShootDesc = selectTarget(targets);

		targets = targetFinder.getFireTargets();
		YYY_TargetFinder.ShootDescription fireShootDesc = selectTarget(targets);

		targets = targetFinder.getStaffTargets();
		YYY_TargetFinder.ShootDescription staffHitDesc = null;
		if (!targets.isEmpty()) {
			int i = 0;
			staffHitDesc = targets.get(i++);
			while (i < targets.size() && staffHitDesc.getWizardsDamage() == 0) {
				staffHitDesc = targets.get(i++);
			}
		}

		int waitTime = 900;
		int tmpWaitTime = 900;

		if (!targetFinder.getHasteTargets().isEmpty()) {
			if ((waitTime = applyBuffAction(targetFinder.getHasteTargets().get(0), move, waitTime)) == -1) {
				return;
			}
		}

		if (manaFree && !targetFinder.getShieldTargets().isEmpty()) {
			if (YYY_Constants.getGame().getShieldManacost() > self.getMana()) { // wait until mana restored
				return;
			}
			if ((tmpWaitTime = applyBuffAction(targetFinder.getShieldTargets().get(0), move, waitTime)) == -1) {
				return;
			}
		}
		if (waitTime > tmpWaitTime) {
			waitTime = tmpWaitTime;
		}

		if (fireShootDesc != null && fireShootDesc.getScore() < 40) {
			fireShootDesc = null;
		}

		if (fireShootDesc != null && (manaFree || fireShootDesc.getWizardsDamage() != 0)) {
			if (fireShootDesc.getScore() > 90. || self.getMana() > self.getMaxMana() * .9) {
				if ((tmpWaitTime = applyTargetAction(fireShootDesc,
													 YYY_FastMath.hypot(self, fireShootDesc.getShootPoint()),
													 move, waitTime)) == -1) {
					return;
				}
			}
		}

		if (waitTime > tmpWaitTime) {
			waitTime = tmpWaitTime;
		}

		if (iceShootDesc != null && (manaFree || iceShootDesc.getWizardsDamage() != 0)) {
			if (iceShootDesc.getWizardsDamage() > 0 || self.getMana() > self.getMaxMana() * .9 || self.getLife() < self.getMaxLife() * .5) {
				double minCastDistance = YYY_FastMath.hypot(self, iceShootDesc.getShootPoint()) - .1; // exact distance for building
				if (iceShootDesc.getMinionsDamage() > 0) {
					minCastDistance -= 5.;
				} else if (iceShootDesc.getWizardsDamage() > 0) {
					minCastDistance -= YYY_Constants.getGame().getWizardRadius() + 5. - YYY_FastMath.hypot(iceShootDesc.getTarget(),
																										   iceShootDesc.getShootPoint());
				}
				if ((tmpWaitTime = applyTargetAction(iceShootDesc, minCastDistance, move, waitTime)) == -1) {
					return;
				}
			}
		}

		if (waitTime > tmpWaitTime) {
			waitTime = tmpWaitTime;
		}

		if (staffHitDesc != null && staffHitDesc.getTicksToGo() < 1 &&
				(missileShootDesc == null ||
						staffHitDesc.getMinionsKills() > 0 ||
						staffHitDesc.getTotalDamage() >= missileShootDesc.getTotalDamage() ||
						staffHitDesc.getScore() >= missileShootDesc.getScore() ||
						self.getMana() < self.getMaxMana() * .9)) {
			if ((tmpWaitTime = applyMeleeAction(staffHitDesc.getTarget(), move, waitTime)) == -1) {
				return;
			}
		}

		if (waitTime > tmpWaitTime) {
			waitTime = tmpWaitTime;
		}

		if (missileShootDesc != null && (manaFree || missileShootDesc.getWizardsDamage() != 0) &&
				(missileShootDesc.getMinionsDamage() == 0 ||
						fireShootDesc == null ||
						fireShootDesc.getScore() < 70. ||
						self.getMana() > self.getMaxMana() * .9)) {
			double minCastDistance = 1000.; // for trees score always negative
			if (missileShootDesc.getScore() >= 0) {
				minCastDistance = YYY_FastMath.hypot(self, missileShootDesc.getShootPoint()) - .1; // exact distance for building
				if (missileShootDesc.getMinionsDamage() > 0) {
					minCastDistance -= 10.;
				} else if (missileShootDesc.getWizardsDamage() > 0) {
					minCastDistance = YYY_FastMath.hypot(self, missileShootDesc.getTarget()) -
							missileShootDesc.getTarget().getRadius();
				}
			}
			if ((tmpWaitTime = applyTargetAction(missileShootDesc, minCastDistance, move, waitTime)) == -1) {
				return;
			}
		}

		if (waitTime > tmpWaitTime) {
			waitTime = tmpWaitTime;
		}

		if (staffHitDesc != null) {
			if ((tmpWaitTime = applyMeleeAction(staffHitDesc.getTarget(), move, waitTime)) == -1) {
				return;
			}
		}
		if (waitTime > tmpWaitTime) {
			waitTime = tmpWaitTime;
		}
	}

	private int applyMeleeAction(CircularUnit target, Move move, int bestWaitTime) {
		double turnAngle = self.getAngleTo(target.getX(), target.getY());
		double maxTurnAngle = YYY_Constants.getGame().getWizardMaxTurnAngle() * wizardsInfo.getMe().getTurnFactor();
		int turnTicksCount = getTurnCount(turnAngle, maxTurnAngle);

		int hastenedTicksRemain = YYY_Utils.wizardStatusTicks(self, StatusType.HASTENED);
		if (hastenedTicksRemain > 0 && turnTicksCount > hastenedTicksRemain) {
			maxTurnAngle = YYY_Constants.getGame().getWizardMaxTurnAngle();
			turnTicksCount = getTurnCount(turnAngle, maxTurnAngle);
		}

		int waitTime = waitTimeForAction(ActionType.STAFF);
		if (bestWaitTime <= waitTime) {
			return bestWaitTime;
		}

		if (waitTime <= turnTicksCount + 2) {
			if (waitTime == 0 && checkHit(turnAngle, target, move)) {
				return -1;
			}
			turnTo(turnAngle, move);
			return -1;
		}
		return waitTime;
	}

	private int applyTargetAction(YYY_TargetFinder.ShootDescription shootDescription, double minCastRange, Move move, int bestWaitTime) {
		YYY_Point target = shootDescription.getShootPoint();
		ActionType actionType = shootDescription.getActionType();
		double turnAngle = self.getAngleTo(target.getX(), target.getY());

		double maxTurnAngle = YYY_Constants.getGame().getWizardMaxTurnAngle() * wizardsInfo.getMe().getTurnFactor();
		int turnTicksCount = getTurnCount(turnAngle, maxTurnAngle);

		int hastenedTicksRemain = YYY_Utils.wizardStatusTicks(self, StatusType.HASTENED);
		if (hastenedTicksRemain > 0 && turnTicksCount > hastenedTicksRemain) {
			maxTurnAngle = YYY_Constants.getGame().getWizardMaxTurnAngle();
			turnTicksCount = getTurnCount(turnAngle, maxTurnAngle);
		}

		int waitTime = waitTimeForAction(actionType);
		if (bestWaitTime <= waitTime) {
			return bestWaitTime;
		}

		int manaRestoreTicks = wizardsInfo.getMe().getTicksToManaRestore(shootDescription.getActionType());
		if (waitTime < manaRestoreTicks) {
			this.manaFree = false;
			waitTime = manaRestoreTicks;
		}

		if (waitTime <= shootDescription.getTicksToGo() &&
				shootDescription.getTicksToGo() > 0) {
			YYY_AccAndSpeedWithFix accAndSpeedByAngle = YYY_AccAndSpeedWithFix.getAccAndSpeedByAngle(self.getAngleTo(target.getX(), target.getY()), 100.);
			move.setSpeed(accAndSpeedByAngle.getSpeed());
			move.setStrafeSpeed(accAndSpeedByAngle.getStrafe());
			currentAction.setActionType(YYY_CurrentAction.ActionType.PURSUIT);
			turnTo(turnAngle, move);
			return -1;
		}

		if (waitTime <= turnTicksCount + 2) {
			// если уже можем попасть - атакуем и бежим дальше
			if (waitTime == 0 && checkShot(turnAngle, minCastRange, move, actionType)) {
				return -1;
			}
			// если не можем попасть - доворачиваем на цель
			turnTo(turnAngle, move);
			return -1;
		}
		return waitTime;
	}

	private int applyBuffAction(YYY_TargetFinder.ShootDescription shootDescription, Move move, int bestWaitTime) {
		Wizard target = (Wizard) shootDescription.getTarget();
		double turnAngle = self.getAngleTo(target.getX(), target.getY());
		if (target.isMe()) {
			turnAngle = 0;
		}

		double maxTurnAngle = YYY_Constants.getGame().getWizardMaxTurnAngle() * wizardsInfo.getMe().getTurnFactor();
		int turnTicksCount = getTurnCount(turnAngle, maxTurnAngle);

		int hastenedTicksRemain = YYY_Utils.wizardStatusTicks(self, StatusType.HASTENED);
		if (hastenedTicksRemain > 0 && turnTicksCount > hastenedTicksRemain) {
			maxTurnAngle = YYY_Constants.getGame().getWizardMaxTurnAngle();
			turnTicksCount = getTurnCount(turnAngle, maxTurnAngle);
		}

		int waitTime = waitTimeForAction(shootDescription.getActionType());
		int manaRestoreTicks = wizardsInfo.getMe().getTicksToManaRestore(shootDescription.getActionType());
		if (waitTime < manaRestoreTicks) {
			this.manaFree = false;
			waitTime = manaRestoreTicks;
		}
		if (bestWaitTime <= waitTime) {
			return bestWaitTime;
		}

		if (waitTime <= turnTicksCount + 2) {
			// если уже можем попасть - атакуем и бежим дальше
			if (waitTime == 0 && checkCast(target, turnAngle, move, shootDescription.getActionType())) {
				return -1;
			}
			// если не можем попасть - доворачиваем на цель
			turnTo(turnAngle, move);
			return -1;
		}
		return waitTime;
	}

	private int waitTimeForAction(ActionType actionType) {
		return Math.max(self.getRemainingCooldownTicksByAction()[actionType.ordinal()], self.getRemainingActionCooldownTicks());
	}

	protected boolean checkHit(double angle, CircularUnit target, Move move) {
		if (Math.abs(angle) > YYY_Constants.getStaffHitSector()) {
			return false;
		}
		if (YYY_FastMath.hypot(self.getX() - target.getX(), self.getY() - target.getY()) < target.getRadius() + YYY_Constants.getGame().getStaffRange()) {
			move.setAction(ActionType.STAFF);
			return true;
		}
		return false;
	}

	private boolean checkShot(double angle, double minCastRange, Move move, ActionType actionType) {
		if (Math.abs(angle) > YYY_Constants.MAX_SHOOT_ANGLE) {
			return false;
		}
		if (waitTimeForAction(actionType) == 0) {
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

	private boolean checkCast(Wizard target, double angle, Move move, ActionType actionType) {
		if (Math.abs(angle) > YYY_Constants.MAX_SHOOT_ANGLE) {
			return false;
		}
		if (waitTimeForAction(actionType) == 0) {
			move.setAction(actionType);
			move.setStatusTargetId(target.getId());
			return true;
		}
		return false;
	}

	private int getTurnCount(double currentAngle, double maxTurnAngle) {
		if (Math.abs(currentAngle) < YYY_Constants.MAX_SHOOT_ANGLE) {
			currentAngle = 0.;
		} else {
			if (currentAngle < 0.) {
				currentAngle += YYY_Constants.MAX_SHOOT_ANGLE;
			} else {
				currentAngle -= YYY_Constants.MAX_SHOOT_ANGLE;
			}
		}

		return (int) ((maxTurnAngle - .001 + Math.abs(currentAngle)) / maxTurnAngle);
	}

	private void turnTo(double angle, Move move) {
		if (currentAction.getActionType() == YYY_CurrentAction.ActionType.RUN_FROM_PROJECTILE) {
			return;
		}
		turnFixed = true;
		move.setTurn(YYY_Utils.updateMaxModule(angle, YYY_Constants.getGame().getWizardMaxTurnAngle() * wizardsInfo.getMe().getTurnFactor()));
	}

	private void turnTo(YYY_Point point, Move move) {
		if (currentAction.getActionType() == YYY_CurrentAction.ActionType.RUN_FROM_PROJECTILE) {
			return;
		}
		Wizard nearestEnemyWizard = null;
		double minDistance = 1e6;
		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() != YYY_Constants.getEnemyFaction()) {
				continue;
			}
			if (Math.abs(wizard.getAngleTo(self)) > Math.PI * .4) { // 0.4 = 72 degrees
				continue;
			}
			double distance = YYY_FastMath.hypot(wizard, self);
			if (distance < minDistance) {
				nearestEnemyWizard = wizard;
				minDistance = distance;
			}
		}

		if (minDistance < 580 && minDistance > 450.) {
			//turn to side
			double nearestWizardAngle = self.getAngleTo(nearestEnemyWizard);
			boolean leftSide;
			if (Math.abs(nearestWizardAngle) < YYY_Constants.getGame().getWizardMaxTurnAngle() * wizardsInfo.getMe().getTurnFactor() * .5) {
				double prefferedAngle = nearestWizardAngle;
				if (point != null && YYY_FastMath.hypot(self, point) > wizardsInfo.getMe().getMoveFactor() * YYY_Constants.getGame().getWizardStrafeSpeed()) {
					prefferedAngle = self.getAngleTo(point.getX(), point.getY());
				}
				leftSide = prefferedAngle > 0;
			} else {
				leftSide = nearestWizardAngle < 0;
			}
			nearestWizardAngle = Math.abs(nearestWizardAngle);
			double turnAngle = Math.PI * .5 - nearestWizardAngle; // 90 degrees
			turnTo(leftSide ? turnAngle : turnAngle * -1, move);
		} else {
			if (point == null) {
				return;
			}
			if (YYY_FastMath.hypot(self, point) < YYY_Constants.getGame().getWizardStrafeSpeed()) {
				point = pointToReach;
			}
			if (point == null || YYY_FastMath.hypot(self, point) < YYY_Constants.getGame().getWizardStrafeSpeed()) {
				return;
			}
			turnTo(self.getAngleTo(point.getX(), point.getY()), move);
		}
	}

	protected void moveTo(int pointIdx, Move move, boolean run) {
		if (wayPoints.size() < 2) {
			return;
		}
		YYY_ScanMatrixItem point = wayPoints.get(pointIdx).getPoint();
		moveToPoint = point;
		double distance = YYY_FastMath.hypot(self, point.getX(), point.getY());
		angle = self.getAngleTo(point.getX(), point.getY());

		YYY_AccAndSpeedWithFix accAndStrafe = YYY_AccAndSpeedWithFix.getAccAndSpeedByAngle(angle, distance);
		move.setSpeed(accAndStrafe.getSpeed());
		move.setStrafeSpeed(accAndStrafe.getStrafe());

		minAngle = 0.;
		maxAngle = 0.;
		targetAngle = 0.;

		if (run) { // look forward
			int idx = pointIdx + 1;
			while (idx < wayPoints.size() && pointIdx + 5 >= idx) {
				YYY_Point wayPoint = wayPoints.get(idx++).getPoint();
				targetAngle = YYY_Utils.normalizeAngle(self.getAngleTo(wayPoint.getX(), wayPoint.getY()) - angle);
				if (targetAngle < minAngle) {
					minAngle = targetAngle;
				} else if (targetAngle > maxAngle) {
					maxAngle = targetAngle;
				}
			}
			minAngle = Math.max(-Math.PI, minAngle - YYY_Constants.RUN_ANGLE_EXPAND);
			maxAngle = Math.min(Math.PI, maxAngle + YYY_Constants.RUN_ANGLE_EXPAND);
			targetAngle = YYY_Utils.normalizeAngle(targetAngle + angle);
		} else { // look on path
			int step = Math.max(pointIdx / 5, 1);
			int idx = pointIdx - step;
			while (idx > 0) {
				YYY_Point wayPoint = wayPoints.get(idx).getPoint();
				targetAngle = YYY_Utils.normalizeAngle(self.getAngleTo(wayPoint.getX(), wayPoint.getY()) - angle);
				if (targetAngle < minAngle) {
					minAngle = targetAngle;
				} else if (targetAngle > maxAngle) {
					maxAngle = targetAngle;
				}
				idx -= step;
			}
			minAngle = Math.max(-Math.PI, minAngle - YYY_Constants.MOVE_ANGLE_EXPAND);
			maxAngle = Math.min(Math.PI, maxAngle + YYY_Constants.MOVE_ANGLE_EXPAND);
			targetAngle = angle;
		}
		YYY_Point changePosition;
		if (Math.abs(YYY_Utils.normalizeAngle(maxAngle - minAngle)) > YYY_Constants.MOVE_ANGLE_PRECISE) {
			changePosition = accAndStrafe.getCoordChange(self.getAngle());
			testScanItem.setPoint(self.getX() + changePosition.getX(), self.getY() + changePosition.getY());
			YYY_Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, unitScoreCalculation, fightStatus);

			double bestDanger = testScanItem.getAllDangers();
			double bestScore = testScanItem.getTotalScore(self);
			double newAngle;
			double closestAngle = Math.abs(YYY_Utils.normalizeAngle(angle - targetAngle));
			YYY_Point bestPosition = testScanItem.clonePoint();
			YYY_AccAndSpeedWithFix bestMove = accAndStrafe;
			double itAngle = minAngle;
			for (; maxAngle - itAngle > YYY_Constants.MOVE_ANGLE_PRECISE; itAngle += YYY_Constants.MOVE_ANGLE_PRECISE) {
				newAngle = YYY_Utils.normalizeAngle(angle + itAngle);
				accAndStrafe = YYY_AccAndSpeedWithFix.getAccAndSpeedByAngle(newAngle, 100.);
				changePosition = accAndStrafe.getCoordChange(self.getAngle());
				testScanItem.setPoint(self.getX() + changePosition.getX(), self.getY() + changePosition.getY());
				YYY_Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, unitScoreCalculation, fightStatus);
				if (!testScanItem.isAvailable() || bestDanger < testScanItem.getAllDangers()) { //run???
					continue;
				}
				if (testScanItem.getAllDangers() == bestDanger) {
					if (bestScore > testScanItem.getTotalScore(self)) {
						continue;
					}
					if (bestScore == testScanItem.getTotalScore(self) && Math.abs(YYY_Utils.normalizeAngle(newAngle - targetAngle)) > closestAngle) {
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

	private boolean testPointDirectAvailable(YYY_Point point) {
		double distance;
		YYY_Point from = new YYY_Point(self.getX(), self.getY());
		YYY_Point to = new YYY_Point(point.getX(), point.getY());
		for (CircularUnit circularUnit : filteredWorld.getAllBlocksList()) {
			distance = YYY_Utils.distancePointToSegment(new YYY_Point(circularUnit.getX(), circularUnit.getY()), from, to);
			if (distance < self.getRadius() + circularUnit.getRadius() + YYY_Constants.STUCK_FIX_RADIUS_ADD) {
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
		if (fightStatus == YYY_FightStatus.NO_ENEMY) {
			YYY_ScanMatrixItem item = null;
			for (int i = 0; i != scan_matrix.length; ++i) {
				for (int j = 0; j != scan_matrix[0].length; ++j) {
					YYY_ScanMatrixItem scanMatrixItem = scan_matrix[i][j];
					if (!scanMatrixItem.isAvailable()) {
						continue;
					}
					if (item == null || item.getTotalScore(self) < scanMatrixItem.getTotalScore(self)) {
						item = scanMatrixItem;
					}
				}
			}
			if (testPointDirectAvailable(item)) {
				wayPoints.add(new YYY_WayPoint(1, scan_matrix[YYY_Constants.CURRENT_PT_X][YYY_Constants.CURRENT_PT_Y], null));
				wayPoints.add(new YYY_WayPoint(2, item, wayPoints.get(0)));
				return;
			}
		}
		queue.add(new YYY_WayPoint(1, scan_matrix[YYY_Constants.CURRENT_PT_X][YYY_Constants.CURRENT_PT_Y], null));
		int nextDistanceFromStart;
		double newScoresOnWay;
		double newDangerOnWay;
		while (!queue.isEmpty()) {
			YYY_WayPoint currentPoint = queue.poll();
			if (currentPoint.getPoint().getWayPoint() != currentPoint) {
				continue;
			}
			nextDistanceFromStart = currentPoint.getDistanceFromStart() + 1;
			for (YYY_ScanMatrixItem scanMatrixItem : currentPoint.getPoint().getNeighbours()) {
				if (!scanMatrixItem.isAvailable()) {
					continue;
				}
				if (scanMatrixItem.getWayPoint() == null) {
					queue.add(new YYY_WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
					continue;
				}
				YYY_WayPoint wayPointToCompare = scanMatrixItem.getWayPoint();
				newScoresOnWay = scanMatrixItem.getTotalScore(self) - YYY_Variables.maxDangerMatrixScore + currentPoint.getScoresOnWay();

				if (newScoresOnWay == wayPointToCompare.getScoresOnWay()) {
					if (wayPointToCompare.getDistanceFromStart() == nextDistanceFromStart) {
						newDangerOnWay = scanMatrixItem.getAllDangers() + currentPoint.getDangerOnWay();
						if (newDangerOnWay < wayPointToCompare.getDangerOnWay()) {
							queue.add(new YYY_WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
						}
					} else if (nextDistanceFromStart < wayPointToCompare.getDistanceFromStart()) {
						queue.add(new YYY_WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
					}
				} else if (newScoresOnWay > wayPointToCompare.getDangerOnWay()) {
					queue.add(new YYY_WayPoint(nextDistanceFromStart, scanMatrixItem, currentPoint));
				}
			}
		}
		getBestMovePoint();
	}

	protected void getBestMovePoint() {
		YYY_ScanMatrixItem best = null;
		double score = 0;
		double tmpScore;

		for (int i = scan_matrix.length - 1; i != -1; --i) {
			for (int j = 0; j != scan_matrix[0].length; ++j) {
				YYY_ScanMatrixItem newScanMatrixItem = scan_matrix[i][j];
				if (newScanMatrixItem.getWayPoint() == null) {
					continue;
				}
				if (best == null) {
					best = newScanMatrixItem;
					score = newScanMatrixItem.getTotalScore(self) +
							newScanMatrixItem.getWayPoint().getScoresOnWay() / newScanMatrixItem.getWayPoint().getDistanceFromStart() + YYY_Variables.maxDangerMatrixScore;
					continue;
				}
				tmpScore = newScanMatrixItem.getTotalScore(self) +
						newScanMatrixItem.getWayPoint().getScoresOnWay() / newScanMatrixItem.getWayPoint().getDistanceFromStart()
						+ YYY_Variables.maxDangerMatrixScore;
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

	private void finalizeQueue(YYY_WayPoint wayPoint) {
		do {
			wayPoints.add(wayPoint);
			wayPoint = wayPoint.getPrev();
		} while (wayPoint != null);
		Collections.reverse(wayPoints);
	}

	private void calcMatrixDanger() {
		double dxFwd = Math.cos(direction) * YYY_Constants.MOVE_SCAN_STEP;
		double dyFwd = Math.sin(direction) * YYY_Constants.MOVE_SCAN_STEP;

		double dxSide = Math.cos(direction - Math.PI * .5) * YYY_Constants.MOVE_SCAN_STEP;
		double dySide = Math.sin(direction - Math.PI * .5) * YYY_Constants.MOVE_SCAN_STEP;
		for (int i = 0; i != scan_matrix.length; ++i) {
			for (int j = 0; j != scan_matrix[0].length; ++j) {
				scan_matrix[i][j].setPoint(self.getX() + dxFwd * (i - YYY_Constants.CURRENT_PT_X) + dxSide * (YYY_Constants.CURRENT_PT_Y - j),
										   self.getY() + dyFwd * (i - YYY_Constants.CURRENT_PT_X) + dySide * (YYY_Constants.CURRENT_PT_Y - j));
			}
			YYY_Utils.calcTilesAvailable(filteredWorld.getAllBlocksList(), scan_matrix[i]);
			for (int j = 0; j != scan_matrix[0].length; ++j) {
				YYY_ScanMatrixItem item = scan_matrix[i][j];
				if (!item.isAvailable()) {
					continue;
				}

				double distanceTo = myLineCalc.calcLineDistanceOtherDanger(item);
				if (distanceTo > 0.) {
					item.addOtherDanger(distanceTo);
				}

				if (YYY_SpawnPoint.checkSpawnPoints()) {
					for (YYY_SpawnPoint spawnPoint : YYY_Constants.SPAWN_POINTS) {
						if (spawnPoint.isPointInDanger(item.getX(), item.getY())) {
							item.addOtherDanger(YYY_Constants.SPAWN_POINT_DANGER);
						}
					}
				}

				if (currentAction.getActionType() == YYY_CurrentAction.ActionType.MOVE_TO_POSITION) {
					if (YYY_FastMath.hypot(self.getX() - myLineCalc.getFightPoint().getX(), self.getY() - myLineCalc.getFightPoint().getY()) > 200) {
						item.addOtherBonus(item.getForwardDistanceDivision() * 140);
					}
				} else if (fightStatus == YYY_FightStatus.NO_ENEMY) {
					item.addOtherBonus(item.getForwardDistanceDivision() * .01);
				}
			}
		}


		for (Bonus bonus : filteredWorld.getBonuses()) {
			if (YYY_FastMath.hypot(self.getX() - bonus.getX(), self.getY() - bonus.getY()) > YYY_Constants.getFightDistanceFilter()) {
				continue;
			}
			YYY_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(bonus.getId());

			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new YYY_Point(bonus.getX(), bonus.getY()));
			}
		}

		if (YYY_Utils.getTicksToBonusSpawn(world.getTickIndex()) < 250) {
			for (int i = 0; i != YYY_BonusesPossibilityCalcs.BONUSES_POINTS.length; ++i) {
				if (YYY_FastMath.hypot(self, YYY_BonusesPossibilityCalcs.BONUSES_POINTS[i]) > YYY_Constants.getFightDistanceFilter()) {
					continue;
				}

				YYY_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(i - 5);

				for (int j = 0; j != scan_matrix.length; ++j) {
					applyScoreForLine(scan_matrix[j], structure, YYY_BonusesPossibilityCalcs.BONUSES_POINTS[i]);
				}
			}
		}

		if (fightStatus == YYY_FightStatus.NO_ENEMY) {
			return;
		}

		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == YYY_Constants.getCurrentFaction()) {
				continue;
			}

			YYY_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(minion.getId());

			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new YYY_Point(minion.getX(), minion.getY()));
			}
		}

		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == YYY_Constants.getCurrentFaction()) {
				continue;
			}
			YYY_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(wizard.getId());
			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new YYY_Point(wizard.getX(), wizard.getY()));
			}
		}

		for (Building building : filteredWorld.getBuildings()) {
			if (building.getFaction() == YYY_Constants.getCurrentFaction()) {
				continue;
			}
			YYY_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(building.getId());

			for (int i = 0; i != scan_matrix.length; ++i) {
				applyScoreForLine(scan_matrix[i], structure, new YYY_Point(building.getX(), building.getY()));
			}
		}
	}

	public void applyScoreForLine(YYY_ScanMatrixItem[] items, YYY_ScoreCalcStructure structure, YYY_Point point) {
		double distance = YYY_Utils.distancePointToSegment(point, items[0], items[items.length - 1]);
		if (distance > structure.getMaxScoreDistance()) {
			return;
		}
		double itemDistance;
		for (int i = 0; i != items.length; ++i) {
			if (!items[i].isAvailable()) {
				continue;
			}
			itemDistance = YYY_FastMath.hypot(items[i].getX() - point.getX(), items[i].getY() - point.getY());
			if (itemDistance <= structure.getMaxScoreDistance()) {
				structure.applyScores(items[i], itemDistance);
			}
		}
	}

	public boolean isMeNearestWizard(YYY_Point point, boolean includeEnemies) {
		double distanceToMe = YYY_FastMath.hypot(self.getX() - point.getX(), self.getY() - point.getY());
		double distanceToMeCalc = distanceToMe * YYY_Constants.NEAREST_TO_BONUS_CALCULATION_OTHER_MULT;
		if (includeEnemies) {
			for (YYY_WizardPhantom phantom : enemyPositionCalc.getDetectedWizards().values()) {
				double distance = YYY_FastMath.hypot(phantom.getPosition().getX() - point.getX(), phantom.getPosition().getY() - point.getY());
				if (!phantom.isUpdated() && distanceToMe > 599.99) {
					distance -= (world.getTickIndex() - phantom.getLastSeenTick()) * YYY_Constants.MAX_WIZARDS_FORWARD_SPEED;
				}
				if (distanceToMeCalc > distance) {
					return false;
				}
			}
		}

		for (Wizard wizard : world.getWizards()) {
			if (wizard.getFaction() != YYY_Constants.getCurrentFaction()) {
				continue;
			}
			if (YYY_FastMath.hypot(wizard.getX() - point.getX(), wizard.getY() - point.getY()) < distanceToMeCalc) {
				return false;
			}
		}
		return true;
	}

	public boolean isInDanger() {
		List<Wizard> enemyWizards = new ArrayList<>();
		List<Wizard> allyWizards = new ArrayList<>();
		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == YYY_Constants.getEnemyFaction()) {
				enemyWizards.add(wizard);
			} else if (!wizard.isMe()) {
				allyWizards.add(wizard);
			}
		}
		if (enemyWizards.isEmpty()) {
			return false;
		}
		if (enemyWizards.size() == 1) {
			Wizard enemyWizard = enemyWizards.get(0);
			if (enemyWizard.getLife() * .8 < self.getLife()) {
				return false;
			}
		}
		for (Wizard wizard : enemyWizards) {
			double distanceToMe = YYY_FastMath.hypot(self, wizard) * 1.1;

			boolean danger = true;
			for (Minion minion : filteredWorld.getMinions()) {
				if (minion.getFaction() != YYY_Constants.getCurrentFaction()) {
					continue;
				}
				double tmpDistance = YYY_FastMath.hypot(minion, wizard);
				if (tmpDistance < distanceToMe) {
					danger = false;
					break;
				}
			}
			if (danger) {
				distanceToMe *= YYY_Constants.DANGER_SAFETY_ADDIT_DISTANCE;
				for (Wizard allyWizard : allyWizards) {
					double tmpDistance = YYY_FastMath.hypot(allyWizard, wizard);
					if (tmpDistance < distanceToMe) {
						danger = false;
						break;
					}
				}
			}

			if (danger) {
				for (Building building : filteredWorld.getBuildings()) {
					if (building.getFaction() != YYY_Constants.getCurrentFaction()) {
						continue;
					}
					double tmpDistance = YYY_FastMath.hypot(building, wizard);
					if (tmpDistance < distanceToMe) {
						danger = false;
						break;
					}
				}
			}

			if (danger) {
				return true;
			}
		}

		if (self.getLife() < self.getMaxLife() * YYY_Constants.ATTACK_ENEMY_WIZARD_LIFE) {
			testScanItem.setPoint(self.getX(), self.getY());
			YYY_Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, unitScoreCalculation, fightStatus);
			if (testScanItem.getWizardsDanger() > 0.) {
				return true;
			}
		}
		return false;
	}

	public void updateFightStatus() {
		if (fightStatus != YYY_FightStatus.IN_DANGER &&
				YYY_Utils.hasEnemy(filteredWorld.getMinions(), agressiveNeutralsCalcs) ||
				YYY_Utils.hasEnemy(filteredWorld.getWizards()) ||
				YYY_Utils.hasEnemy(filteredWorld.getBuildings())) {
			fightStatus = YYY_FightStatus.ENEMY_FOUND;
		}
	}
}
