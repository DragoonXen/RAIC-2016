import model.ActionType;
import model.Building;
import model.Faction;
import model.Minion;
import model.ProjectileType;
import model.StatusType;
import model.Tree;
import model.Wizard;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragoon on 12/3/16.
 */
public class TargetFinder {

	private List<ShootDescription> foundShoots;
	private List<ShootDescription> prevFoundShoots;

	public TargetFinder() {
		foundShoots = new ArrayList<>();
		prevFoundShoots = new ArrayList<>();
	}

	public TargetFinder(List<ShootDescription> foundShoots, List<ShootDescription> prevFoundShoots) {
		this.foundShoots = new ArrayList<>(foundShoots);
		this.prevFoundShoots = new ArrayList<>(prevFoundShoots);
	}

	public void updateTargets(FilteredWorld filteredWorld, BaseLine myLineCalc, Point pointToReach, AgressiveNeutralsCalcs agressiveNeutralsCalcs) {
		Wizard self = Variables.self;

		{
			List<ShootDescription> tmpSwap = prevFoundShoots;
			prevFoundShoots = foundShoots;
			foundShoots = tmpSwap;
			foundShoots.clear();
		}

		WizardsInfo.WizardInfo myWizardInfo = Variables.wizardsInfo.getMe();
		int missileDamage = myWizardInfo.getMagicalMissileDamage();
		int frostBoltDamage = myWizardInfo.getFrostBoltDamage();
		int staffDamage = myWizardInfo.getStaffDamage();

		boolean treeCut = (myLineCalc == PositionMoveLine.INSTANCE &&
				(Utils.unitsCountCloseToDestination(filteredWorld.getTrees(), new Point(self.getX(), self.getY())) > 0 ||
						Utils.unitsCountAtDistance(filteredWorld.getTrees(), self, Constants.TREES_DISTANCE_TO_CUT) >= 3)) ||
				Utils.unitsCountAtDistance(filteredWorld.getTrees(),
										   self,
										   Constants.TREES_DISTANCE_TO_CUT) >= Constants.TREES_COUNT_TO_CUT || // too much trees around
				Utils.unitsCountCloseToDestination(filteredWorld.getAllBlocksList(), pointToReach) >= 2 && // can't go throught obstacles
						Utils.unitsCountCloseToDestination(filteredWorld.getTrees(), pointToReach) > 0;
		double distanceToTarget;
		double score;
		double direction;

		if (treeCut) {
			for (Tree tree : filteredWorld.getTrees()) {

				// distance to destination
				// distance to me
				score = Constants.CUT_REACH_POINT_DISTANCE_PTIORITY / FastMath.hypot(pointToReach.getX() - tree.getX(),
																					 pointToReach.getY() - tree.getY());
				distanceToTarget = FastMath.hypot(self, tree);
				score += Constants.CUT_SELF_DISTANCE_PRIORITY / distanceToTarget;

				score *= (tree.getRadius() + self.getRadius()) * .02;
				direction = tree.getAngleTo(self);
				Point backShootPoint = new Point(tree.getX() + Math.cos(direction) * tree.getRadius(),
												 tree.getY() + Math.sin(direction) * tree.getRadius());
				foundShoots.add(new ShootDescription(backShootPoint,
													 ActionType.MAGIC_MISSILE,
													 score / Utils.getHitsToKill(tree.getLife(), missileDamage) -
															 Constants.CUT_REACH_POINT_DISTANCE_PTIORITY));

				if (distanceToTarget < Constants.getGame().getStaffRange() + tree.getRadius() + 50) {
					distanceToTarget -= Constants.getGame().getStaffRange() + tree.getRadius();
					foundShoots.add(new ShootDescription(backShootPoint,
														 ActionType.STAFF,
														 score / Utils.getHitsToKill(tree.getLife(), staffDamage) -
																 Constants.CUT_REACH_POINT_DISTANCE_PTIORITY,
														 distanceToTarget));
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
			foundShoots.add(new ShootDescription(new Point(minion.getX() + Math.cos(minion.getAngle()) * minion.getRadius(),
														   minion.getY() + Math.sin(minion.getAngle()) * minion.getRadius()),
												 ActionType.MAGIC_MISSILE,
												 score));
			if (missileDamage >= minion.getLife()) {
				ShootDescription.lastInstance.setMinionsKills(1);
				ShootDescription.lastInstance.setMinionsDamage(minion.getLife());
			} else {
				ShootDescription.lastInstance.setMinionsDamage(missileDamage);
			}

			appendStaffTarget(minion, Utils.getMinionAttackPriority(minion, staffDamage, self), staffDamage);
			if (frostBoltDamage > 0) {
				score = Utils.getMinionAttackPriority(minion, frostBoltDamage, self);
				if (frostBoltDamage >= minion.getLife()) {
					ShootDescription.lastInstance.setMinionsKills(1);
					ShootDescription.lastInstance.setMinionsDamage(minion.getLife());
				} else {
					ShootDescription.lastInstance.setMinionsDamage(frostBoltDamage);
				}
				foundShoots.add(new ShootDescription(new Point(minion.getX(), minion.getY()),
													 ActionType.FROST_BOLT,
													 score));
			}
		}

		for (BuildingPhantom building : filteredWorld.getBuildings()) {
			if (building.getFaction() == Constants.getCurrentFaction() || building.isInvulnerable()) {
				continue;
			}
			score = Constants.LOW_AIM_SCORE;
			double tmp = (building.getMaxLife() - building.getLife()) / (double) building.getMaxLife();
			score += tmp * tmp;
			score *= Constants.BUILDING_AIM_PROIRITY;
			direction = building.getAngleTo(self);
			Point backShootPoint = new Point(building.getX() + Math.cos(direction) * building.getRadius(),
											 building.getY() + Math.sin(direction) * building.getRadius());

			foundShoots.add(new ShootDescription(backShootPoint,
												 ActionType.MAGIC_MISSILE,
												 score));

			appendStaffTarget(building, score, backShootPoint, staffDamage);
		}

		double oneStepMoving = ShootEvasionMatrix.EVASION_MATRIX[6][0] * myWizardInfo.getMoveFactor(); // doesn't matter, hastened or not

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
			distanceToTarget = self.getCastRange() + wizard.getRadius() + oneStepMoving;
			if (myWizardInfo.isHasFireball()) {
				distanceToTarget += Constants.getGame().getFireballExplosionMinDamageRange() * .5;
			}

			if (FastMath.hypot(self, wizard) > distanceToTarget) {
				continue;
			}

			// staff
			direction = Utils.normalizeAngle(wizard.getAngleTo(Variables.self) + wizard.getAngle());
			appendStaffTarget(wizard,
							  score,
							  new Point(wizard.getX() + Math.cos(direction) * wizard.getRadius(),
										wizard.getY() + Math.sin(direction) * wizard.getRadius()),
							  staffDamage);
			EnemyEvasionFilteredWorld evastionFiltering = new EnemyEvasionFilteredWorld(wizard, Variables.world);
			// MM
			Point pointToShoot = new Point(wizard.getX(), wizard.getY());

			Point pointFwdToShoot = new Point(wizard.getX() + Math.cos(wizard.getAngle()) * ShootEvasionMatrix.mmDistanceFromCenter,
											  wizard.getY() + Math.sin(wizard.getAngle()) * ShootEvasionMatrix.mmDistanceFromCenter);
			//(Wizard wizard, double score, Point shootPoint, int damage) {
			if (frostBoltDamage > 0) {
				// Frost
				pointFwdToShoot = new Point(wizard.getX() + Math.cos(wizard.getAngle()) * ShootEvasionMatrix.frostBoltDistanceFromCenter,
											wizard.getY() + Math.sin(wizard.getAngle()) * ShootEvasionMatrix.frostBoltDistanceFromCenter);
			}
		}
	}

	private void appendStaffTarget(Minion minion, double score, int damage) {
		double distanceToTarget = FastMath.hypot(Variables.self, minion) - minion.getRadius() - Constants.getGame().getStaffRange();
		if (distanceToTarget < 50) {
			double direction = Utils.normalizeAngle(minion.getAngleTo(Variables.self) + minion.getAngle());
			;
			Point backShootPoint = new Point(minion.getX() + Math.cos(direction) * minion.getRadius(),
											 minion.getY() + Math.sin(direction) * minion.getRadius());

			foundShoots.add(new ShootDescription(backShootPoint, ActionType.STAFF, score, distanceToTarget));
			if (damage <= minion.getLife()) {
				ShootDescription.lastInstance.setMinionsDamage(minion.getLife());
				ShootDescription.lastInstance.setMinionsKills(1);
			} else {
				ShootDescription.lastInstance.setMinionsDamage(damage);
			}
		}
	}

	private void appendStaffTarget(Building building, double score, Point shootPoint, int damage) {
		double distanceToTarget = FastMath.hypot(Variables.self, shootPoint) - Constants.getGame().getStaffRange();
		if (distanceToTarget < 50) {
			foundShoots.add(new ShootDescription(shootPoint, ActionType.STAFF, score, distanceToTarget));
			if (damage <= building.getLife()) {
				ShootDescription.lastInstance.setBuildingDamage(building.getLife());
				ShootDescription.lastInstance.setBuildingsDestroy(1);
			} else {
				ShootDescription.lastInstance.setBuildingDamage(damage);
			}
		}
	}

	private void appendStaffTarget(Wizard wizard, double score, Point shootPoint, int damage) {
		double distanceToTarget = FastMath.hypot(Variables.self, shootPoint) - Constants.getGame().getStaffRange();
		if (distanceToTarget < 50) {
			foundShoots.add(new ShootDescription(shootPoint, ActionType.STAFF, score, distanceToTarget));
			if (damage <= wizard.getLife()) {
				ShootDescription.lastInstance.setWizardsDamage(wizard.getLife());
				ShootDescription.lastInstance.setWizardsKills(1);
			} else {
				ShootDescription.lastInstance.setBuildingDamage(damage);
			}
		}
	}

	private final static int EVASION_CHECK_COUNT = 18;
	private final static int EVASION_CHECK_ANGLE_STEP = 360 / EVASION_CHECK_COUNT;

	private int checkEnemyWizardEvasion(Wizard wizard,
										Point shootPoint,
										double stepDistance,
										EnemyEvasionFilteredWorld evasionFilter,
										ProjectileType projectileType) {
		Wizard self = Variables.self;
		Point shootingPosition = new Point(self.getX(), self.getY());
		Point projectilePoint = shootingPosition;
		double direction = Utils.normalizeAngle(self.getAngleTo(shootPoint.getX(), shootPoint.getY()) + self.getAngle());
		double projectileSpeed = Utils.PROJECTIVE_SPEED[projectileType.ordinal()];
		double projectileRadius = Utils.PROJECTIVE_RADIUS[projectileType.ordinal()];
		Point projectileVector = new Point(Math.cos(direction) * projectileRadius,
										   Math.sin(direction) * projectileRadius);
		Point movementVector = new Point();

		for (int i = 0; i != EVASION_CHECK_COUNT; ++i) {
			int intAngle = i * EVASION_CHECK_ANGLE_STEP;
			double angle = intAngle / 180 * Math.PI;
			movementVector.update(Math.cos(angle + wizard.getAngle()), Math.sin(angle + wizard.getAngle()));
		}

		return 2;
	}

	/*
	private void findTargets() {
		prevIceTargets = iceTargets;
		prevMissileTargets = missileTargets;
		prevStaffTargets = staffTargets;
		iceTargets = new ArrayList<>();
		missileTargets = new ArrayList<>();
		staffTargets = new ArrayList<>();
		int missileDamage = wizardsInfo.getMe().getMagicalMissileDamage();
		int frostBoltDamage = wizardsInfo.getMe().getFrostBoltDamage();
		double distanceToTarget;

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

		if (wizardsInfo.getMe().isHasFireball()) {
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
											 minion.getRadius() + Constants.getGame().getFireballExplosionMaxDamageRange() - .5));
				addFireTarget(checkDistances(checkPoint,
											 minion.getRadius() + Constants.getGame().getFireballExplosionMinDamageRange() - .5));
			}

			for (Building building : filteredWorld.getBuildings()) {
				if (building.getFaction() == Constants.getCurrentFaction()) {
					continue;
				}
				Point checkPoint = new Point(building.getX(), building.getY());
				addFireTarget(checkDistances(checkPoint, building.getRadius() + Constants.getGame().getFireballExplosionMaxDamageRange() - .5));
				addFireTarget(checkDistances(checkPoint, building.getRadius() + Constants.getGame().getFireballExplosionMinDamageRange() - .5));
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
						Constants.getGame().getFireballExplosionMaxDamageRange() -
						ShootEvasionMatrix.EVASION_MATRIX[0][ticks] - .1;
				if (checkDistance > 0.) {
					addFireTarget(checkDistances(checkPoint, checkDistance));
				}
				checkDistance = wizard.getRadius() +
						Constants.getGame().getFireballExplosionMinDamageRange() -
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

	 */

	public static class ShootDescription {
		private int minionsDamage;
		private int wizardsDamage;
		private int buildingDamage;

		private int minionsKills;
		private int wizardsKills;
		private int buildingsDestroy;

		private double distanceWalkToShoot;
		private double turnAngle;
		private int ticksToTurn;
		private int ticksToGo;

		private Point shootPoint;
		private ActionType actionType;

		private double score;

		private static ShootDescription lastInstance;

		public ShootDescription(Point shootPoint, ActionType actionType, double score) {
			lastInstance = this;
			this.shootPoint = shootPoint;
			this.actionType = actionType;
			this.score = score;

			turnAngle = Variables.self.getAngleTo(shootPoint.getX(), shootPoint.getY());

			this.ticksToTurn = (int) ((Math.abs(turnAngle) - Constants.MAX_SHOOT_ANGLE + Variables.maxTurnAngle - .1) / Variables.maxTurnAngle);

			this.score -= Constants.PER_TURN_TICK_PENALTY * ticksToTurn;
		}

		public ShootDescription(Point shootPoint, ActionType actionType, double score, double walkingDistance) {
			this(shootPoint, actionType, score);
			this.distanceWalkToShoot = walkingDistance;
			if (walkingDistance > 0.) {
				ShootEvasionMatrix.getTicksForDistance(walkingDistance, (int) Math.round(turnAngle), Variables.wizardsInfo.getMe().getMoveFactor());
			}
		}

		public void setMinionsDamage(int minionsDamage) {
			this.minionsDamage = minionsDamage;
		}

		public void setWizardsDamage(int wizardsDamage) {
			this.wizardsDamage = wizardsDamage;
		}

		public void setBuildingDamage(int buildingDamage) {
			this.buildingDamage = buildingDamage;
		}

		public void setMinionsKills(int minionsKills) {
			this.minionsKills = minionsKills;
		}

		public void setWizardsKills(int wizardsKills) {
			this.wizardsKills = wizardsKills;
		}

		public void setBuildingsDestroy(int buildingsDestroy) {
			this.buildingsDestroy = buildingsDestroy;
		}

		public void setDistanceWalkToShoot(double distanceWalkToShoot) {
			this.distanceWalkToShoot = distanceWalkToShoot;
		}

		public void setTicksToGo(int ticksToGo) {
			this.ticksToGo = ticksToGo;
		}

		public int getMinionsDamage() {
			return minionsDamage;
		}

		public int getWizardsDamage() {
			return wizardsDamage;
		}

		public int getBuildingDamage() {
			return buildingDamage;
		}

		public int getMinionsKills() {
			return minionsKills;
		}

		public int getWizardsKills() {
			return wizardsKills;
		}

		public int getBuildingsDestroy() {
			return buildingsDestroy;
		}

		public double getDistanceWalkToShoot() {
			return distanceWalkToShoot;
		}

		public int getTicksToTurn() {
			return ticksToTurn;
		}

		public int getTicksToGo() {
			return ticksToGo;
		}

		public Point getShootPoint() {
			return shootPoint;
		}

		public ActionType getActionType() {
			return actionType;
		}

		public double getScore() {
			return score;
		}
	}
}
