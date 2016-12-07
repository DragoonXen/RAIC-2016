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

	protected ScanMatrixItem pointToReach;

	private ScanMatrixItem testScanItem = new ScanMatrixItem(0, 0, 1.);

	protected ArrayList<WayPoint> wayPoints = new ArrayList<>();

	protected Point moveToPoint;

	private PriorityQueue<WayPoint> queue = new PriorityQueue<>();

	protected double minAngle = 0.;
	protected double maxAngle = 0.;
	protected double angle = 0.;
	protected double targetAngle = 0.;

	protected HashMap<Long, Double> projectilesDTL = new HashMap<>(); //store
	protected CurrentAction currentAction = new CurrentAction();

	protected EnemyPositionCalc enemyPositionCalc = new EnemyPositionCalc();

	protected BonusesPossibilityCalcs bonusesPossibilityCalcs = new BonusesPossibilityCalcs();

	protected AgressiveNeutralsCalcs agressiveNeutralsCalcs = new AgressiveNeutralsCalcs();
	protected UnitScoreCalculation unitScoreCalculation = new UnitScoreCalculation();
	protected TeammateIdsContainer teammateIdsContainer = new TeammateIdsContainer();

	protected boolean treeCut;
	protected boolean turnFixed;
	protected boolean goToBonusActivated = false;
	protected boolean moveToLineActivated = false;
	protected FightStatus fightStatus;

	protected TargetFinder targetFinder = new TargetFinder();

	protected WizardsInfo wizardsInfo = new WizardsInfo();

	public StrategyImplement(Wizard self) {
		myLineCalc = Constants.getLine(Utils.getDefaultMyLine((int) self.getId()));
		lastFightLine = myLineCalc;
	}

	public void move(Wizard self, World world, Game game, Move move) {
		agressiveNeutralsCalcs.updateMap(world);
		enemyPositionCalc.updatePositions(world);
		wizardsInfo.updateData(world, enemyPositionCalc);
		bonusesPossibilityCalcs.updateTick(world, enemyPositionCalc);
		teammateIdsContainer.updateTeammatesIds(world);
		SkillsLearning.updateSkills(self, enemyPositionCalc, world.getWizards(), move);
		fightStatus = FightStatus.NO_ENEMY;
		treeCut = false;
		moveToPoint = null;
		minAngle = 0.;
		wayPoints.clear();
		maxAngle = 0.;
		angle = 0.;
		targetAngle = 0.;
		turnFixed = false;

		Variables.self = self;
		Variables.world = world;
		this.world = world;
		this.self = self;
		SpawnPoint.updateTick(world.getTickIndex());

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
		unitScoreCalculation.updateScores(filteredWorld, self, fightStatus, agressiveNeutralsCalcs);
		if (isInDanger()) {
			fightStatus = FightStatus.IN_DANGER;
			direction = Utils.normalizeAngle(direction + Math.PI);
			filteredWorld = Utils.filterWorld(world,
											  new Point(self.getX() + Math.cos(direction) * Constants.MOVE_SCAN_FIGURE_CENTER,
														self.getY() + Math.sin(direction) * Constants.MOVE_SCAN_FIGURE_CENTER),
											  enemyPositionCalc.getBuildingPhantoms(), teammateIdsContainer);
		}


		currentAction.setActionType(CurrentAction.ActionType.FIGHT); // default state
		updateFightStatus();
		updateProjectilesDTL(filteredWorld.getProjectiles());

		unitScoreCalculation.updateScores(filteredWorld, self, fightStatus, agressiveNeutralsCalcs);
		evade(move, checkHitByProjectilePossible());

		targetFinder.updateTargets(filteredWorld, myLineCalc, pointToReach, agressiveNeutralsCalcs);

		makeShot(move);
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
				double ticksRunToBonusA = distanceToBonusA / (Constants.getGame().getWizardForwardSpeed() * wizardsInfo.getMe().getMoveFactor()) *
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
				double ticksRunToBonusB = distanceToBonusB / (Constants.getGame().getWizardForwardSpeed() * wizardsInfo.getMe().getMoveFactor()) *
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
				updateFightStatus();
				unitScoreCalculation.updateScores(filteredWorld, self, fightStatus, agressiveNeutralsCalcs);
			}

			if (goToBonusActivated && FastMath.hypot(self, PositionMoveLine.INSTANCE.getPositionToMove()) <= self.getRadius() +
					game.getBonusRadius() +
					game.getWizardForwardSpeed() * wizardsInfo.getMe().getMoveFactor() + .1) {
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
																									 wizardsInfo.getMe().getMoveFactor());
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

		if (!turnFixed) {
			turnTo(moveToPoint, move);
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
					Variables.wizardsInfo.getWizardInfo(castUnit).getCastRange() - Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()]
					: Constants.getGame().getFetishBlowdartAttackRange() - Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()];
			projectilesDTL.put(projectile.getId(), castRange);
		}
	}

	protected double checkHitByProjectilePossible() {
		double maxStep = wizardsInfo.getMe().getMoveFactor() * Constants.getGame().getWizardForwardSpeed();
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
								unitScoreCalculationTickSupport.getScores(filteredWorld, self, fightStatus, agressiveNeutralsCalcs, ticks),
								fightStatus);
			bestScore = testScanItem.getTotalScore(self);
			bestDangerOnWay += testScanItem.getAllDangers();
			bestDamage += Utils.checkProjectiveCollision(position, ticks++);
		}

		double currScore;
		double currDamage;
		double currDangerOnWay;
		int hastenedTicks = Utils.wizardStatusTicks(self, StatusType.HASTENED);
		int currHastenedTicks;
		double moveFactor = wizardsInfo.getMe().getMoveFactor();
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
																 wizardsInfo.getMe().getTurnFactor() * Constants.getGame().getWizardMaxTurnAngle() :
																 Constants.getGame().getWizardMaxTurnAngle());
					if (--currHastenedTicks == -1) {
						currMoveFactor -= Constants.getGame().getHastenedMovementBonusFactor();
					}
					testScanItem.setPoint(position.getX(), position.getY());
					Utils.calcTileScore(testScanItem,
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

	private void makeShot(Move move) {
		List<TargetFinder.ShootDescription> targets = targetFinder.getMissileTargets();
		TargetFinder.ShootDescription missileShootDesc = null;
		if (!targets.isEmpty()) {
			missileShootDesc = targets.get(targets.size() - 1);
		}
		targets = targetFinder.getIceTargets();
		TargetFinder.ShootDescription iceShootDesc = null;
		if (!targets.isEmpty()) {
			iceShootDesc = targets.get(targets.size() - 1);
		}
		targets = targetFinder.getFireTargets();
		TargetFinder.ShootDescription fireShootDesc = null;
		if (!targets.isEmpty()) {
			fireShootDesc = targets.get(targets.size() - 1);
		}
		targets = targetFinder.getStaffTargets();
		TargetFinder.ShootDescription staffHitDesc = null;
		if (!targets.isEmpty()) {
			staffHitDesc = targets.get(0);
		}

		if (wizardsInfo.getMe().isHasHasteSkill() && wizardsInfo.getMe().getHastened() == 0 && Constants.getGame().getHasteManacost() <= self.getMana()) {
			if (self.getRemainingActionCooldownTicks() == 0) {
				move.setAction(ActionType.HASTE);
				Wizard allyWizardCastTo = null;
				for (Wizard wizard : filteredWorld.getWizards()) {
					if (wizard.getFaction() == Constants.getCurrentFaction() && FastMath.hypot(self, wizard) + .1 < self.getCastRange()) {
						if (allyWizardCastTo == null) {
							allyWizardCastTo = wizard;
						} else {
							if (allyWizardCastTo.getLife() > wizard.getLife()) {
								allyWizardCastTo = wizard;
							}
						}
					}
				}
				if (allyWizardCastTo != null) {
					move.setStatusTargetId(allyWizardCastTo.getId());
				}
				return;
			}
		}

		if (fireShootDesc != null && fireShootDesc.getScore() < 40) {
			fireShootDesc = null;
		}

		if (fireShootDesc != null) {
			if (fireShootDesc.getScore() > 90. || self.getMana() > self.getMaxMana() * .9) {
				if (applyTargetAction(ActionType.FIREBALL,
									  FastMath.hypot(self, fireShootDesc.getShootPoint()),
									  fireShootDesc.getShootPoint(),
									  move)) {
					return;
				}
			}
		}

		if (iceShootDesc != null) {
			if (iceShootDesc.getWizardsDamage() > 0 || self.getMana() > self.getMaxMana() * .9 || self.getLife() < self.getMaxLife() * .5) {
				if (applyTargetAction(ActionType.FROST_BOLT,
									  FastMath.hypot(self, iceShootDesc.getShootPoint()) - Constants.getGame().getWizardRadius(),
									  iceShootDesc.getShootPoint(),
									  move)) {
					return;
				}
			}
		}

		if (staffHitDesc != null && staffHitDesc.getTicksToGo() < 1 &&
				(missileShootDesc == null ||
						staffHitDesc.getMinionsKills() > 0 ||
						staffHitDesc.getMinionsDamage() >= missileShootDesc.getMinionsDamage() ||
						self.getMana() < self.getMaxMana() * .9)) {
			if (applyMeleeAction(staffHitDesc.getTarget(), move)) {
				return;
			}
		}

		if (missileShootDesc != null &&
				(missileShootDesc.getMinionsDamage() == 0 ||
						fireShootDesc == null ||
						fireShootDesc.getScore() < 70. ||
						self.getMana() > self.getMaxMana() * .9)) {
			double minCastDistance = 1000.; // for trees score always negative
			if (missileShootDesc.getScore() >= 0) {
				minCastDistance = FastMath.hypot(self, missileShootDesc.getShootPoint()) - .1; // exact distance for building
				if (missileShootDesc.getMinionsDamage() > 0) {
					minCastDistance -= 10.;
				} else if (missileShootDesc.getWizardsDamage() > 0) {
					minCastDistance = FastMath.hypot(self, missileShootDesc.getTarget()) -
							missileShootDesc.getTarget().getRadius();
				}
			}
			if (applyTargetAction(ActionType.MAGIC_MISSILE, minCastDistance, missileShootDesc.getShootPoint(), move)) {
				return;
			}
		}

		if (staffHitDesc != null) {
			applyMeleeAction(staffHitDesc.getTarget(), move);
		}
	}

	private boolean applyMeleeAction(CircularUnit target, Move move) {
		double turnAngle = self.getAngleTo(target.getX(), target.getY());
		double maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle() * wizardsInfo.getMe().getMoveFactor();
		int turnTicksCount = getTurnCount(turnAngle, maxTurnAngle);

		int hastenedTicksRemain = Utils.wizardStatusTicks(self, StatusType.HASTENED);
		if (hastenedTicksRemain > -1 && turnTicksCount > hastenedTicksRemain) {
			maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle();
			turnTicksCount = getTurnCount(turnAngle, maxTurnAngle);
		}

		if (waitTimeForAction(ActionType.STAFF) <= turnTicksCount + 2) {
			if (checkHit(turnAngle, target, move)) {
				return true;
			}
			turnTo(turnAngle, move);
			return true;
		}
		return false;
	}

	private boolean applyTargetAction(ActionType actionType, double minCastRange, Point target, Move move) {
		double turnAngle = self.getAngleTo(target.getX(), target.getY());

		double maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle() * wizardsInfo.getMe().getMoveFactor();
		int turnTicksCount = getTurnCount(turnAngle, maxTurnAngle);

		int hastenedTicksRemain = Utils.wizardStatusTicks(self, StatusType.HASTENED);
		if (hastenedTicksRemain > -1 && turnTicksCount > hastenedTicksRemain) {
			maxTurnAngle = Constants.getGame().getWizardMaxTurnAngle();
			turnTicksCount = getTurnCount(turnAngle, maxTurnAngle);
		}

		if (waitTimeForAction(actionType) <= turnTicksCount + 2) {
			// если уже можем попасть - атакуем и бежим дальше
			if (checkShot(turnAngle, minCastRange, move, actionType)) {
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

	private boolean checkShot(double angle, double minCastRange, Move move, ActionType actionType) {
		if (Math.abs(angle) > Constants.MAX_SHOOT_ANGLE) {
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

	private int getTurnCount(double currentAngle, double maxTurnAngle) {
		if (Math.abs(currentAngle) < Constants.MAX_SHOOT_ANGLE) {
			currentAngle = 0.;
		} else {
			if (currentAngle < 0.) {
				currentAngle += Constants.MAX_SHOOT_ANGLE;
			} else {
				currentAngle -= Constants.MAX_SHOOT_ANGLE;
			}
		}

		return (int) ((maxTurnAngle - .001 + Math.abs(currentAngle)) / maxTurnAngle);
	}

	private void turnTo(double angle, Move move) {
		if (currentAction.getActionType() == CurrentAction.ActionType.RUN_FROM_PROJECTILE) {
			return;
		}
		turnFixed = true;
		move.setTurn(Utils.updateMaxModule(angle, Constants.getGame().getWizardMaxTurnAngle() * wizardsInfo.getMe().getTurnFactor()));
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
			if (Math.abs(wizard.getAngleTo(self)) > Math.PI * .4) { // 0.4 = 72 degrees
				continue;
			}
			double distance = FastMath.hypot(wizard, self);
			if (distance < minDistance) {
				nearestEnemyWizard = wizard;
				minDistance = distance;
			}
		}

		if (minDistance < 580 && minDistance > 450.) {
			//turn to side
			double nearestWizardAngle = self.getAngleTo(nearestEnemyWizard);
			boolean leftSide;
			if (Math.abs(nearestWizardAngle) < Constants.getGame().getWizardMaxTurnAngle() * wizardsInfo.getMe().getTurnFactor() * .5) {
				double prefferedAngle = nearestWizardAngle;
				if (point != null && FastMath.hypot(self, point) > wizardsInfo.getMe().getMoveFactor() * Constants.getGame().getWizardStrafeSpeed()) {
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
			if (FastMath.hypot(point.getX() - self.getX(), point.getY() - self.getY()) < Constants.getGame().getWizardStrafeSpeed()) {
				point = pointToReach;
			}
			if (FastMath.hypot(point.getX() - self.getX(), point.getY() - self.getY()) < Constants.getGame().getWizardStrafeSpeed()) {
				return;
			}
			turnTo(self.getAngleTo(point.getX(), point.getY()), move);
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
			Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, unitScoreCalculation, fightStatus);

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
				Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, unitScoreCalculation, fightStatus);
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
		if (fightStatus == FightStatus.NO_ENEMY) {
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
				} else if (fightStatus == FightStatus.NO_ENEMY) {
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

		if (fightStatus == FightStatus.NO_ENEMY) {
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
		double distanceToMe = FastMath.hypot(self.getX() - point.getX(), self.getY() - point.getY());
		double distanceToMeCalc = distanceToMe * Constants.NEAREST_TO_BONUS_CALCULATION_OTHER_MULT;
		if (includeEnemies) {
			for (WizardPhantom phantom : enemyPositionCalc.getDetectedWizards().values()) {
				double distance = FastMath.hypot(phantom.getPosition().getX() - point.getX(), phantom.getPosition().getY() - point.getY());
				if (!phantom.isUpdated() && distanceToMe > 599.99) {
					distance -= (world.getTickIndex() - phantom.getLastSeenTick()) * Constants.MAX_WIZARDS_FORWARD_SPEED;
				}
				if (distanceToMeCalc > distance) {
					return false;
				}
			}
		}

		for (Wizard wizard : world.getWizards()) {
			if (wizard.getFaction() != Constants.getCurrentFaction()) {
				continue;
			}
			if (FastMath.hypot(wizard.getX() - point.getX(), wizard.getY() - point.getY()) < distanceToMeCalc) {
				return false;
			}
		}
		return true;
	}

	public boolean isInDanger() {
		List<Wizard> enemyWizards = new ArrayList<>();
		List<Wizard> allyWizards = new ArrayList<>();
		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == Constants.getEnemyFaction()) {
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
			double distanceToMe = FastMath.hypot(self, wizard) * 1.1;

			boolean danger = true;
			for (Minion minion : filteredWorld.getMinions()) {
				if (minion.getFaction() != Constants.getCurrentFaction()) {
					continue;
				}
				double tmpDistance = FastMath.hypot(minion, wizard);
				if (tmpDistance < distanceToMe) {
					danger = false;
					break;
				}
			}
			if (danger) {
				distanceToMe *= Constants.DANGER_SAFETY_ADDIT_DISTANCE;
				for (Wizard allyWizard : allyWizards) {
					double tmpDistance = FastMath.hypot(allyWizard, wizard);
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

		if (self.getLife() < self.getMaxLife() * Constants.ATTACK_ENEMY_WIZARD_LIFE) {
			testScanItem.setPoint(self.getX(), self.getY());
			Utils.calcTileScore(testScanItem, filteredWorld, myLineCalc, self, unitScoreCalculation, fightStatus);
			if (testScanItem.getWizardsDanger() > 0.) {
				return true;
			}
		}
		return false;
	}

	public void updateFightStatus() {
		if (fightStatus != FightStatus.IN_DANGER &&
				Utils.hasEnemy(filteredWorld.getMinions(), agressiveNeutralsCalcs) ||
				Utils.hasEnemy(filteredWorld.getWizards()) ||
				Utils.hasEnemy(filteredWorld.getBuildings())) {
			fightStatus = FightStatus.ENEMY_FOUND;
		}
	}
}
