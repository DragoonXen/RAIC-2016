import model.ActionType;
import model.Building;
import model.CircularUnit;
import model.Faction;
import model.LivingUnit;
import model.Minion;
import model.ProjectileType;
import model.StatusType;
import model.Tree;
import model.Wizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dragoon on 12/3/16.
 */
public class YYY_TargetFinder {

	//public final static Comparator<YYY_Pair<Double, YYY_Point>> POINT_AIM_SORT_COMPARATOR = (o1, o2) -> o2.getFirst().compareTo(o1.getFirst());
	//protected Comparator<ShootDescription> TARGET_SCORE_COMPARATOR = (o1, o2) -> o1.getScore() == o2.getScore() ? o1.getI

	protected List<ShootDescription> missileTargets;
	protected List<ShootDescription> staffTargets;
	protected List<ShootDescription> iceTargets;
	protected List<ShootDescription> hasteTargets;
	protected List<ShootDescription> shieldTargets;
	protected List<ShootDescription> prevMissileTargets;
	protected List<ShootDescription> prevStaffTargets;
	protected List<ShootDescription> prevIceTargets;
	protected List<ShootDescription> fireTargets;

	private Wizard self;
	private YYY_FilteredWorld filteredWorld;
	private YYY_AgressiveNeutralsCalcs agressiveNeutralsCalcs;
	private YYY_WizardsInfo.WizardInfo myWizardInfo;

	private double oneStepMoving;

	public YYY_TargetFinder() {
		missileTargets = new ArrayList<>();
		staffTargets = new ArrayList<>();
		iceTargets = new ArrayList<>();
		hasteTargets = new ArrayList<>();
		shieldTargets = new ArrayList<>();
		prevMissileTargets = new ArrayList<>();
		prevStaffTargets = new ArrayList<>();
		prevIceTargets = new ArrayList<>();
		fireTargets = new LinkedList<>();
	}

	public YYY_TargetFinder(List<ShootDescription> missileTargets,
							List<ShootDescription> staffTargets,
							List<ShootDescription> iceTargets,
							List<ShootDescription> hasteTargets,
							List<ShootDescription> shieldTargets,
							List<ShootDescription> prevMissileTargets,
							List<ShootDescription> prevStaffTargets,
							List<ShootDescription> prevIceTargets,
							List<ShootDescription> fireTargets) {
		this.missileTargets = new ArrayList<>(missileTargets);
		this.staffTargets = new ArrayList<>(staffTargets);
		this.iceTargets = new ArrayList<>(iceTargets);
		this.hasteTargets = new ArrayList<>(hasteTargets);
		this.shieldTargets = new ArrayList<>(shieldTargets);
		this.prevMissileTargets = new ArrayList<>(prevMissileTargets);
		this.prevStaffTargets = new ArrayList<>(prevStaffTargets);
		this.prevIceTargets = new ArrayList<>(prevIceTargets);
		this.fireTargets = new LinkedList<>(fireTargets);
	}

	public void updateTargets(YYY_FilteredWorld filteredWorld,
							  YYY_BaseLine myLineCalc,
							  YYY_Point pointToReach,
							  YYY_AgressiveNeutralsCalcs agressiveNeutralsCalcs,
							  int stuck) {
		this.myWizardInfo = YYY_Variables.wizardsInfo.getMe();
		oneStepMoving = YYY_ShootEvasionMatrix.EVASION_MATRIX[6][0] * myWizardInfo.getMoveFactor(); // doesn't matter, hastened or not
		this.self = YYY_Variables.self;
		this.filteredWorld = filteredWorld;
		this.agressiveNeutralsCalcs = agressiveNeutralsCalcs;

		{
			List<ShootDescription> tmp = prevMissileTargets;
			prevMissileTargets = missileTargets;
			missileTargets = tmp;
			tmp.clear();
			tmp = prevIceTargets;
			prevIceTargets = iceTargets;
			iceTargets = tmp;
			tmp.clear();
			tmp = prevStaffTargets;
			prevStaffTargets = staffTargets;
			staffTargets = tmp;
			tmp.clear();
			hasteTargets.clear();
			shieldTargets.clear();
		}

		int missileDamage = myWizardInfo.getMagicalMissileDamage();
		int frostBoltDamage = myWizardInfo.getFrostBoltDamage();
		int staffDamage = myWizardInfo.getStaffDamage();

		boolean treeCut = stuck > 10 || (myLineCalc == YYY_PositionMoveLine.INSTANCE &&
				(YYY_Utils.unitsCountCloseToDestination(filteredWorld.getTrees(), new YYY_Point(self.getX(), self.getY())) > 0 ||
						YYY_Utils.unitsCountAtDistance(filteredWorld.getTrees(), self, YYY_Constants.TREES_DISTANCE_TO_CUT) >= 3)) ||
				YYY_Utils.unitsCountAtDistance(filteredWorld.getTrees(),
											   self,
											   YYY_Constants.TREES_DISTANCE_TO_CUT) >= YYY_Constants.TREES_COUNT_TO_CUT || // too much trees around
				YYY_Utils.unitsCountCloseToDestination(filteredWorld.getAllBlocksList(), pointToReach) >= 2 && // can't go throught obstacles
						YYY_Utils.unitsCountCloseToDestination(filteredWorld.getTrees(), pointToReach) > 0;
		double score;
		double direction;

		if (treeCut) {
			addTreesAsTargets(filteredWorld, pointToReach, missileDamage, staffDamage);
		}

		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == YYY_Constants.getCurrentFaction()) {
				continue;
			}
			if (minion.getFaction() == Faction.NEUTRAL && !agressiveNeutralsCalcs.isMinionAgressive(minion.getId())) {
				continue;
			}
			YYY_Point shootPoint = new YYY_Point(minion.getX() + Math.cos(minion.getAngle()) * minion.getRadius(),
												 minion.getY() + Math.sin(minion.getAngle()) * minion.getRadius());

			if (self.getCastRange() < YYY_FastMath.hypot(self, shootPoint)) {
				continue;
			}
			score = YYY_Utils.getMinionAttackPriority(minion, missileDamage, self);
			missileTargets.add(new ShootDescription(shootPoint,
													ActionType.MAGIC_MISSILE,
													score,
													minion));
			if (missileDamage >= minion.getLife()) {
				ShootDescription.lastInstance.setMinionsKills(1);
				ShootDescription.lastInstance.setMinionsDamage(minion.getLife());
			} else {
				ShootDescription.lastInstance.setMinionsDamage(missileDamage);
			}

			appendStaffTarget(minion, YYY_Utils.getMinionAttackPriority(minion, staffDamage, self), staffDamage);
			if (frostBoltDamage > 0) {
				score = YYY_Utils.getMinionAttackPriority(minion, frostBoltDamage, self);
				if (frostBoltDamage >= minion.getLife()) {
					ShootDescription.lastInstance.setMinionsKills(1);
					ShootDescription.lastInstance.setMinionsDamage(minion.getLife());
				} else {
					ShootDescription.lastInstance.setMinionsDamage(frostBoltDamage);
				}
				iceTargets.add(new ShootDescription(shootPoint,
													ActionType.FROST_BOLT,
													score,
													minion));
			}
		}

		for (YYY_BuildingPhantom building : filteredWorld.getBuildings()) {
			if (building.getFaction() == YYY_Constants.getCurrentFaction() || building.isInvulnerable()) {
				continue;
			}
			score = YYY_Constants.LOW_AIM_SCORE;
			double tmp = (building.getMaxLife() - building.getLife()) / (double) building.getMaxLife();
			score += tmp * tmp;
			score *= YYY_Constants.BUILDING_AIM_PROIRITY;
			direction = YYY_Utils.normalizeAngle(building.getAngleTo(self) + building.getAngle());
			YYY_Point backShootPoint = new YYY_Point(building.getX() + Math.cos(direction) * building.getRadius(),
													 building.getY() + Math.sin(direction) * building.getRadius());
			if (self.getCastRange() + YYY_Utils.PROJECTIVE_RADIUS[ProjectileType.MAGIC_MISSILE.ordinal()]
					< YYY_FastMath.hypot(self, backShootPoint) + .1) {
				continue;
			}

			missileTargets.add(new ShootDescription(backShootPoint,
													ActionType.MAGIC_MISSILE,
													score,
													building));
			if (missileDamage >= building.getLife()) {
				ShootDescription.lastInstance.setBuildingDamage(building.getLife());
				ShootDescription.lastInstance.setBuildingsDestroy(1);
			} else {
				ShootDescription.lastInstance.setBuildingDamage(missileDamage);
			}

			appendStaffTarget(building, score, backShootPoint, staffDamage);
		}

		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == YYY_Constants.getCurrentFaction()) {
				continue;
			}
			score = YYY_Constants.LOW_AIM_SCORE;
			double tmp = (wizard.getMaxLife() - wizard.getLife()) / (double) wizard.getMaxLife();
			score += tmp * tmp;
			score *= YYY_Constants.WIZARD_AIM_PROIRITY;
			if (YYY_Utils.wizardHasStatus(wizard, StatusType.SHIELDED)) {
				score *= YYY_Constants.SHIELDENED_AIM_PRIORITY;
			}
			if (YYY_Utils.wizardHasStatus(wizard, StatusType.EMPOWERED)) {
				score *= YYY_Constants.EMPOWERED_AIM_PRIORITY;
			}

			// staff
			direction = YYY_Utils.normalizeAngle(wizard.getAngleTo(self) + wizard.getAngle());
			appendStaffTarget(wizard,
							  score,
							  new YYY_Point(wizard.getX() + Math.cos(direction) * wizard.getRadius(),
											wizard.getY() + Math.sin(direction) * wizard.getRadius()),
							  staffDamage);
			YYY_EnemyEvasionFilteredWorld evastionFiltering = new YYY_EnemyEvasionFilteredWorld(wizard, YYY_Variables.world);
			// MM
			YYY_Point pointFwdToShoot = new YYY_Point(wizard.getX() + Math.cos(wizard.getAngle()) * YYY_ShootEvasionMatrix.mmDistanceFromCenter,
													  wizard.getY() + Math.sin(wizard.getAngle()) * YYY_ShootEvasionMatrix.mmDistanceFromCenter);
			int checked = checkEnemyWizardEvasion(wizard, pointFwdToShoot, evastionFiltering, ProjectileType.MAGIC_MISSILE);
			int checkedSecond;
			if (checked == 0) {
				appendShootTarget(wizard, score, pointFwdToShoot, missileDamage, ActionType.MAGIC_MISSILE);
			} else {
				YYY_Point pointToShoot = new YYY_Point(wizard.getX(), wizard.getY());
				checkedSecond = checkEnemyWizardEvasion(wizard, pointToShoot, evastionFiltering, ProjectileType.MAGIC_MISSILE);
				if (checkedSecond < checked) {
					appendShootTarget(wizard, score, pointToShoot, missileDamage, ActionType.MAGIC_MISSILE);
					ShootDescription.lastInstance.setTicksToGo(checkedSecond);
				} else if (checked < YYY_Constants.MAX_SHOOT_DETECT_STEP_DISTANCE) {
					appendShootTarget(wizard, score, pointFwdToShoot, missileDamage, ActionType.MAGIC_MISSILE);
					ShootDescription.lastInstance.setTicksToGo(checked);
				}
			}

			// Frost
			if (frostBoltDamage > 0) {
				pointFwdToShoot = new YYY_Point(wizard.getX() + Math.cos(wizard.getAngle()) * YYY_ShootEvasionMatrix.frostBoltDistanceFromCenter,
												wizard.getY() + Math.sin(wizard.getAngle()) * YYY_ShootEvasionMatrix.frostBoltDistanceFromCenter);
				checked = checkEnemyWizardEvasion(wizard, pointFwdToShoot, evastionFiltering, ProjectileType.FROST_BOLT);
				if (checked == 0) {
					appendShootTarget(wizard, score, pointFwdToShoot, missileDamage, ActionType.FROST_BOLT);
				} else {
					YYY_Point pointToShoot = new YYY_Point(wizard.getX(), wizard.getY());
					checkedSecond = checkEnemyWizardEvasion(wizard, pointToShoot, evastionFiltering, ProjectileType.FROST_BOLT);
					if (checkedSecond < checked) {
						appendShootTarget(wizard, score, pointToShoot, missileDamage, ActionType.FROST_BOLT);
						ShootDescription.lastInstance.setTicksToGo(checkedSecond);
					} else if (checked < YYY_Constants.MAX_SHOOT_DETECT_STEP_DISTANCE) {
						appendShootTarget(wizard, score, pointFwdToShoot, missileDamage, ActionType.FROST_BOLT);
						ShootDescription.lastInstance.setTicksToGo(checked);
					}
				}
			}
		}

		if (myWizardInfo.isHasFireball()) {
			fireTargets.clear();
			for (Minion minion : filteredWorld.getMinions()) {
				if (minion.getFaction() == YYY_Constants.getCurrentFaction()) {
					continue;
				}
				if (minion.getFaction() == Faction.NEUTRAL && !agressiveNeutralsCalcs.isMinionAgressive(minion.getId())) {
					continue;
				}
				if (minion.getSpeedX() != 0. || minion.getSpeedY() != 0) {
					continue;
				}
				YYY_Point checkPoint = new YYY_Point(minion.getX(), minion.getY());
				checkDistances(checkPoint,
							   minion.getRadius() + YYY_Constants.getGame().getFireballExplosionMaxDamageRange() - .5);
				checkDistances(checkPoint,
							   minion.getRadius() + YYY_Constants.getGame().getFireballExplosionMinDamageRange() - .5);
			}

			for (Building building : filteredWorld.getBuildings()) {
				if (building.getFaction() == YYY_Constants.getCurrentFaction()) {
					continue;
				}
				YYY_Point checkPoint = new YYY_Point(building.getX(), building.getY());
				checkDistances(checkPoint, building.getRadius() + YYY_Constants.getGame().getFireballExplosionMaxDamageRange() - .5);
				checkDistances(checkPoint, building.getRadius() + YYY_Constants.getGame().getFireballExplosionMinDamageRange() - .5);
			}

			for (Wizard wizard : filteredWorld.getWizards()) {
				if (wizard.getFaction() == YYY_Constants.getCurrentFaction()) {
					continue;
				}

				YYY_Point checkPoint = new YYY_Point(wizard.getX(), wizard.getY());
				int ticks = YYY_Utils.getTicksToFly(YYY_FastMath.hypot(self, wizard), YYY_Utils.PROJECTIVE_SPEED[ProjectileType.FIREBALL.ordinal()]);
				ticks = Math.min(ticks - 1, YYY_ShootEvasionMatrix.EVASION_MATRIX[0].length - 1);
				if (YYY_FastMath.hypot(self, wizard) < self.getCastRange()) {
					addFireTarget(checkFireballDamage(checkPoint));
				}
				double checkDistance = wizard.getRadius() +
						YYY_Constants.getGame().getFireballExplosionMaxDamageRange() -
						YYY_ShootEvasionMatrix.EVASION_MATRIX[0][ticks] - .1;
				if (checkDistance > 0.) {
					checkDistances(checkPoint, checkDistance);
				}
				checkDistance = wizard.getRadius() +
						YYY_Constants.getGame().getFireballExplosionMinDamageRange() -
						YYY_ShootEvasionMatrix.EVASION_MATRIX[0][ticks] - .1;
				checkDistances(checkPoint, checkDistance);

				checkPoint.update(wizard.getX() + Math.cos(wizard.getAngle()) * YYY_ShootEvasionMatrix.fireballDistanceFromCenter,
								  wizard.getY() + Math.sin(wizard.getAngle()) * YYY_ShootEvasionMatrix.fireballDistanceFromCenter);
				if (YYY_FastMath.hypot(self, checkPoint) < self.getCastRange()) {
					addFireTarget(checkFireballDamage(checkPoint));
				}
			}
			Collections.sort(fireTargets);
			int ticksToGo = 21;
			for (Iterator<ShootDescription> iterator = fireTargets.iterator(); iterator.hasNext(); ) {
				ShootDescription fireTarget = iterator.next();
				if (fireTarget.getTicksToGo() >= ticksToGo) {
					iterator.remove();
					continue;
				}
				if (YYY_Utils.noTreesOnWay(fireTarget.getShootPoint(), self, ProjectileType.FIREBALL, filteredWorld)) {
					ticksToGo = fireTarget.ticksToGo;
				} else {
					iterator.remove();
				}
			}
		}

		if (myWizardInfo.isHasHasteSkill()) {
			for (Wizard wizard : filteredWorld.getWizards()) {
				if (wizard.getFaction() != YYY_Constants.getCurrentFaction()) {
					continue;
				}
				if (YYY_Variables.wizardsInfo.getWizardInfo(wizard.getId()).getHastened() < 30 &&
						YYY_FastMath.hypot(self, wizard) < self.getCastRange()) {
					hasteTargets.add(new ShootDescription(wizard, ActionType.HASTE));
				}
			}
			if (myWizardInfo.getHastened() < 30) {
				hasteTargets.add(new ShootDescription(self, ActionType.HASTE));
			}
		}

		if (myWizardInfo.isHasShieldSkill()) {
			for (Wizard wizard : filteredWorld.getWizards()) {
				if (wizard.getFaction() != YYY_Constants.getCurrentFaction()) {
					continue;
				}
				if (YYY_Variables.wizardsInfo.getWizardInfo(wizard.getId()).getShielded() < 30 &&
						YYY_FastMath.hypot(self, wizard) < self.getCastRange()) {
					shieldTargets.add(new ShootDescription(wizard, ActionType.SHIELD));
				}
				if (myWizardInfo.getShielded() < 30) {
					hasteTargets.add(new ShootDescription(self, ActionType.SHIELD));
				}
			}
		}

		Collections.sort(missileTargets);
		Collections.sort(iceTargets);
		Collections.sort(hasteTargets);
		Collections.sort(shieldTargets);

		boolean hasTargets = hasTargets();

		missileTargets = filterTargets(missileTargets);
		iceTargets = filterTargets(iceTargets);
		fireTargets = filterTargets(fireTargets);

		// not cut trees, but have targets, no one reached from trees
		if (!treeCut && hasTargets && !hasTargets()) {
			addTreesAsTargets(filteredWorld, pointToReach, missileDamage, staffDamage);
			Collections.sort(missileTargets);
			missileTargets = filterTargets(missileTargets);
		}
		Collections.sort(staffTargets);
		staffTargets = filterStaffTargets(staffTargets);
	}

	private boolean hasTargets() {
		return !missileTargets.isEmpty() ||
				!iceTargets.isEmpty() ||
				!fireTargets.isEmpty();
	}

	private void addTreesAsTargets(YYY_FilteredWorld filteredWorld, YYY_Point pointToReach, int missileDamage, int staffDamage) {
		double score;
		double distanceToTarget;
		double direction;
		for (Tree tree : filteredWorld.getTrees()) {

			// distance to destination
			// distance to me
			score = YYY_Constants.CUT_REACH_POINT_DISTANCE_PTIORITY / YYY_FastMath.hypot(tree, pointToReach);
			distanceToTarget = YYY_FastMath.hypot(self, tree);
			score += YYY_Constants.CUT_SELF_DISTANCE_PRIORITY / distanceToTarget;

			score *= (tree.getRadius() + self.getRadius()) * .02;
			direction = YYY_Utils.normalizeAngle(tree.getAngleTo(self) + tree.getAngle());
			YYY_Point backShootPoint = new YYY_Point(tree.getX() + Math.cos(direction) * tree.getRadius(),
													 tree.getY() + Math.sin(direction) * tree.getRadius());
			missileTargets.add(new ShootDescription(backShootPoint,
													ActionType.MAGIC_MISSILE,
													score / YYY_Utils.getHitsToKill(tree.getLife(), missileDamage) -
															YYY_Constants.CUT_REACH_POINT_DISTANCE_PTIORITY,
													tree));

			if (distanceToTarget < YYY_Constants.getGame().getStaffRange() + tree.getRadius() + 50) {
				distanceToTarget -= YYY_Constants.getGame().getStaffRange() + tree.getRadius();
				staffTargets.add(new ShootDescription(backShootPoint,
													  ActionType.STAFF,
													  score / YYY_Utils.getHitsToKill(tree.getLife(), staffDamage) -
															  YYY_Constants.CUT_REACH_POINT_DISTANCE_PTIORITY,
													  tree,
													  distanceToTarget));
			}
		}
	}

	private final static int angleCheck = 20;
	private final static int checkCount = 360 / angleCheck;
	private final static double angleCheckRadians = Math.PI / 180. * angleCheck;

	private void checkDistances(YYY_Point point, double distance) {
		for (int i = 0; i != checkCount; ++i) {
			YYY_Point checkPoint = new YYY_Point(point.getX() + distance * Math.cos(angleCheckRadians * i),
												 point.getY() + distance * Math.sin(angleCheckRadians * i));
			ShootDescription temp = checkFireballDamage(checkPoint);
			if (temp != null && temp.getScore() > 0 && (temp.getTicksToGo() == 0 || temp.getWizardsDamage() > 0)) {
				addFireTarget(temp);
			}
		}
	}

	public ShootDescription checkFireballDamage(YYY_Point where) {
		int ticksToGo = 0;
		double score = YYY_FastMath.hypot(self, where);
		int ticksToFly;
		if (score > self.getCastRange()) {
			ticksToGo = (int) ((score - self.getCastRange()) / oneStepMoving + .95);
			if (ticksToGo > 20) {
				return null;
			}
			ticksToFly = YYY_Utils.getTicksToFly(self.getCastRange(), YYY_Utils.PROJECTIVE_SPEED[ProjectileType.FIREBALL.ordinal()]);
		} else {
			ticksToFly = YYY_Utils.getTicksToFly(YYY_FastMath.hypot(self, where), YYY_Utils.PROJECTIVE_SPEED[ProjectileType.FIREBALL.ordinal()]);
		}

		int minionsDamage = 0;
		int minionKills = 0;
		int buildingsDamage = 0;
		int buildingsDestroys = 0;
		int wizardsDamage = 0;
		int wizardsKills = 0;
		score = 0.;
		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == YYY_Constants.getCurrentFaction()) {
				continue;
			}
			if (minion.getFaction() == Faction.NEUTRAL && !agressiveNeutralsCalcs.isMinionAgressive(minion.getId())) {
				continue;
			}

			YYY_Point checkPoint = new YYY_Point(minion.getX() + minion.getSpeedY() * ticksToFly, minion.getY() + minion.getSpeedY() * ticksToFly);
			double distance = YYY_FastMath.hypot(checkPoint, where) - minion.getRadius();
			if (distance < YYY_Constants.getGame().getFireballExplosionMinDamageRange()) {
				double damage;
				if (distance < YYY_Constants.getGame().getFireballExplosionMaxDamageRange()) {
					damage = myWizardInfo.getFireballMaxDamage();
				} else {
					distance -= YYY_Constants.getGame().getFireballExplosionMaxDamageRange();
					damage = myWizardInfo.getFireballMaxDamage() -
							(int) ((YYY_Constants.getFireballLowerindDamageDistance() + distance) / YYY_Constants.getFireballLowerindDamageDistance());
				}
				if (damage + YYY_Constants.getGame().getBurningSummaryDamage() / 2 >= minion.getLife()) {
					minionsDamage += minion.getLife();
					++minionKills;
				} else {
					minionsDamage += damage + YYY_Constants.getGame().getBurningSummaryDamage() / 2;
				}
				minionsDamage += Math.min(damage + YYY_Constants.getGame().getBurningSummaryDamage() / 2, minion.getLife());
				score += Math.min(damage + YYY_Constants.getGame().getBurningSummaryDamage() / 2, minion.getLife());
			}
		}

		for (YYY_BuildingPhantom building : filteredWorld.getBuildings()) {
			if (building.getFaction() == YYY_Constants.getCurrentFaction() || building.isInvulnerable()) {
				continue;
			}

			double distance = YYY_FastMath.hypot(building, where) - building.getRadius();
			if (distance < YYY_Constants.getGame().getFireballExplosionMinDamageRange()) {
				double damage;
				if (distance < YYY_Constants.getGame().getFireballExplosionMaxDamageRange()) {
					damage = YYY_Constants.getGame().getFireballExplosionMaxDamage();
				} else {
					distance -= YYY_Constants.getGame().getFireballExplosionMaxDamageRange();
					damage = myWizardInfo.getFireballMaxDamage() -
							(int) ((YYY_Constants.getFireballLowerindDamageDistance() + distance) / YYY_Constants.getFireballLowerindDamageDistance());
				}
				damage = Math.min(damage + YYY_Constants.getGame().getBurningSummaryDamage(), building.getLife());
				if (damage == building.getLife()) {
					++buildingsDestroys;
				}
				buildingsDamage += damage;
				score += damage * 1.5;
			}
		}
		YYY_Pair<Double, YYY_Pair<Integer, Boolean>> wizardsDamageCalc;
		for (Wizard wizard : filteredWorld.getWizards()) {
			wizardsDamageCalc = checkFirePointsWizard(wizard, where, ticksToFly);
			if (wizardsDamageCalc != null) {
				if (wizardsDamageCalc.getSecond() != null) {
					wizardsDamage += wizardsDamageCalc.getSecond().getFirst();
					if (wizardsDamageCalc.getSecond().getSecond()) {
						++wizardsKills;
					}
				}
				score += wizardsDamageCalc.getFirst();
			}
		}
		wizardsDamageCalc = checkFirePointsWizard(self, where, ticksToFly);
		if (wizardsDamageCalc != null) {
			if (wizardsDamageCalc.getSecond() != null) {
				wizardsDamage += wizardsDamageCalc.getSecond().getFirst();
				if (wizardsDamageCalc.getSecond().getSecond()) {
					++wizardsKills;
				}
			}
			score += wizardsDamageCalc.getFirst();
		}
		if (score <= 1.) {
			return null;
		}
		return new ShootDescription(minionsDamage,
									wizardsDamage,
									buildingsDamage,
									minionKills,
									wizardsKills,
									buildingsDestroys,
									where,
									score,
									ticksToGo);
	}

	private YYY_Pair<Double, YYY_Pair<Integer, Boolean>> checkFirePointsWizard(Wizard wizard, YYY_Point where, int ticksToFly) {
		if (YYY_FastMath.hypot(wizard, where) > 300.) {
			return null;
		}
		ticksToFly = Math.min(ticksToFly - 1, YYY_ShootEvasionMatrix.EVASION_MATRIX[0].length - 1);
		double distance = YYY_FastMath.hypot(wizard, where) - wizard.getRadius();
		YYY_WizardsInfo.WizardInfo wizardInfo = YYY_Variables.wizardsInfo.getWizardInfo(wizard.getId());
		if (wizard.getFaction() == YYY_Constants.getEnemyFaction()) {
			int angleToPoint = Math.abs((int) Math.round(wizard.getAngleTo(where.getX(), where.getY()) * 180. / Math.PI));
			boolean hastened = YYY_Variables.wizardsInfo.getWizardInfo(wizard.getId()).getHastened() > 0;
			double tmpDistance =
					(hastened ?
							YYY_HastenedEvasionMatrix.HASTENED_EVASION_MATRIX[180 - angleToPoint][ticksToFly] :
							YYY_ShootEvasionMatrix.EVASION_MATRIX[180 - angleToPoint][ticksToFly]) * wizardInfo.getMoveFactor();
			distance += tmpDistance;
			// alternate check move forward
			{
				tmpDistance = YYY_ShootEvasionMatrix.EVASION_MATRIX[0][ticksToFly] * wizardInfo.getMoveFactor();
				YYY_Point newPoint = new YYY_Point(wizard.getX() + Math.cos(wizard.getAngle()) * tmpDistance,
												   wizard.getY() + Math.sin(wizard.getAngle()) * tmpDistance);
				tmpDistance = YYY_FastMath.hypot(newPoint, where) - wizard.getRadius();
				if (tmpDistance > distance) {
					distance = tmpDistance;
				}
			}
		} else if (wizard.isMe()) {
			int angleToPoint = Math.abs((int) Math.round(wizard.getAngleTo(where.getX(), where.getY()) * 180. / Math.PI));
			boolean hastened = YYY_Variables.wizardsInfo.getWizardInfo(wizard.getId()).getHastened() > 0;
			double tmpDistance =
					(hastened ?
							YYY_HastenedEvasionMatrix.HASTENED_EVASION_MATRIX[angleToPoint][ticksToFly] :
							YYY_ShootEvasionMatrix.EVASION_MATRIX[angleToPoint][ticksToFly]) * wizardInfo.getMoveFactor();
			distance -= tmpDistance;
		}

		int damage;
		if (distance <= YYY_Constants.getGame().getFireballExplosionMinDamageRange()) {
			double score;
			if (distance <= YYY_Constants.getGame().getFireballExplosionMaxDamageRange()) {
				score = myWizardInfo.getFireballMaxDamage();
			} else {
				distance -= YYY_Constants.getGame().getFireballExplosionMaxDamageRange();
				score = myWizardInfo.getFireballMaxDamage() -
						(int) ((YYY_Constants.getFireballLowerindDamageDistance() + distance) / YYY_Constants.getFireballLowerindDamageDistance());
			}
			damage = Math.min((int) score + YYY_Constants.getGame().getBurningSummaryDamage(),
							  wizard.getLife() + YYY_Constants.getGame().getBurningSummaryDamage() / 2);
			boolean killed = false;
			score = Math.min(score + YYY_Constants.getGame().getBurningSummaryDamage(), wizard.getLife()) * 3.;
			if (wizard.getLife() < score + YYY_Constants.getGame().getBurningSummaryDamage() * .5 &&
					wizard.getFaction() == YYY_Constants.getEnemyFaction()) {
				killed = true;
				score += 65.;
			}
			if (wizard.getFaction() == YYY_Constants.getCurrentFaction()) {
				if (wizard.isMe()) {
					return new YYY_Pair<>(-score * 5, null);
				} else {
					return new YYY_Pair<>(-score, null);
				}
			} else {
				return new YYY_Pair<>(score, new YYY_Pair<>(damage, killed));
			}
		}
		return null;
	}

	private void addFireTarget(ShootDescription shootDescription) {
		if (shootDescription != null && shootDescription.getScore() > 0) {
			fireTargets.add(shootDescription);
		}
	}

	private List<ShootDescription> filterTargets(List<ShootDescription> shootDescriptions) {
		List<ShootDescription> filtered = new ArrayList<>();
		ShootDescription lastAdded = null;
		YYY_Point selfPoint = new YYY_Point(self.getX(), self.getY());
		for (ShootDescription shootDescription : shootDescriptions) {
			if (lastAdded != null && lastAdded.getTicksToGo() <= shootDescription.getTicksToGo()) {
				continue;
			}
			YYY_Point shootTo = shootDescription.getShootPoint();
			boolean canShoot = true;
			double radius = YYY_Utils.PROJECTIVE_RADIUS[shootDescription.getActionType().ordinal() - 2];
			for (Tree tree : YYY_FilteredWorld.lastInstance.getShootingTreeList()) {
				if (tree == shootDescription.getTarget()) {
					// skip tree if it's a target
					continue;
				}
				double distance = YYY_Utils.distancePointToSegment(new YYY_Point(tree.getX(), tree.getY()), shootTo, selfPoint);
				if (distance < radius + tree.getRadius()) {
					canShoot = false;
					break;
				}
			}
			if (canShoot) {
				filtered.add(shootDescription);
				if (shootDescription.getTicksToGo() == 0) {
					break;
				}
				lastAdded = shootDescription;
			}
		}
		return filtered;
	}

	private List<ShootDescription> filterStaffTargets(List<ShootDescription> shootDescriptions) {
		List<ShootDescription> filtered = new ArrayList<>();
		ShootDescription lastAdded = null;
		for (ShootDescription shootDescription : shootDescriptions) {
			if (lastAdded != null && lastAdded.getTicksToGo() <= shootDescription.getTicksToGo()) {
				continue;
			}
			lastAdded = shootDescription;
			filtered.add(shootDescription);
		}
		return filtered;
	}

	private void appendStaffTarget(Minion minion, double score, int damage) {
		double distanceToTarget = YYY_FastMath.hypot(self, minion) - minion.getRadius() - YYY_Constants.getGame().getStaffRange();
		if (distanceToTarget < 50) {
			double direction = YYY_Utils.normalizeAngle(minion.getAngleTo(self) + minion.getAngle());
			YYY_Point backShootPoint = new YYY_Point(minion.getX() + Math.cos(direction) * minion.getRadius(),
													 minion.getY() + Math.sin(direction) * minion.getRadius());

			staffTargets.add(new ShootDescription(backShootPoint, ActionType.STAFF, score, minion, distanceToTarget));
			if (damage >= minion.getLife()) {
				ShootDescription.lastInstance.setMinionsDamage(minion.getLife());
				ShootDescription.lastInstance.setMinionsKills(1);
			} else {
				ShootDescription.lastInstance.setMinionsDamage(damage);
			}
		}
	}

	private void appendStaffTarget(Building building, double score, YYY_Point shootPoint, int damage) {
		double distanceToTarget = YYY_FastMath.hypot(self, shootPoint) - YYY_Constants.getGame().getStaffRange();
		if (distanceToTarget < 50) {
			staffTargets.add(new ShootDescription(shootPoint, ActionType.STAFF, score, building, distanceToTarget));
			if (damage >= building.getLife()) {
				ShootDescription.lastInstance.setBuildingDamage(building.getLife());
				ShootDescription.lastInstance.setBuildingsDestroy(1);
			} else {
				ShootDescription.lastInstance.setBuildingDamage(damage);
			}
		}
	}

	private void appendStaffTarget(Wizard wizard, double score, YYY_Point shootPoint, int damage) {
		double distanceToTarget = YYY_FastMath.hypot(self, shootPoint) - YYY_Constants.getGame().getStaffRange();
		if (distanceToTarget < 50) {
			staffTargets.add(new ShootDescription(shootPoint, ActionType.STAFF, score, wizard, distanceToTarget));
			if (damage >= wizard.getLife()) {
				ShootDescription.lastInstance.setWizardsDamage(wizard.getLife());
				ShootDescription.lastInstance.setWizardsKills(1);
			} else {
				ShootDescription.lastInstance.setWizardsDamage(damage);
			}
		}
	}

	private void appendShootTarget(Wizard wizard, double score, YYY_Point shootPoint, int damage, ActionType actionType) {
		ShootDescription sd = new ShootDescription(shootPoint, actionType, score, wizard);
		if (actionType == ActionType.MAGIC_MISSILE) {
			missileTargets.add(sd);
		} else {
			iceTargets.add(sd);
		}
		if (damage >= wizard.getLife()) {
			sd.setWizardsDamage(wizard.getLife());
			sd.setWizardsKills(1);
		} else {
			sd.setWizardsDamage(damage);
			if (actionType == ActionType.FROST_BOLT) {
				sd.multScore(YYY_Constants.FROST_WIZARD_AIM_PROIRITY);
			}
		}
	}

	private final static int EVASION_CHECK_COUNT = 36;
	private final static int EVASION_CHECK_ANGLE_STEP = 360 / EVASION_CHECK_COUNT;

	private int checkEnemyWizardEvasion(Wizard wizard,
										YYY_Point shootPoint,
										YYY_EnemyEvasionFilteredWorld evasionFilter,
										ProjectileType projectileType) {
		double projectileSpeed = YYY_Utils.PROJECTIVE_SPEED[projectileType.ordinal()];
		int checkCount = (int) ((self.getCastRange() + projectileSpeed - .1) / projectileSpeed);
		double shootDirection = YYY_Utils.normalizeAngle(self.getAngleTo(shootPoint.getX(), shootPoint.getY()) + self.getAngle());
		YYY_Point projectileVector = new YYY_Point(Math.cos(shootDirection) * projectileSpeed,
												   Math.sin(shootDirection) * projectileSpeed);
		YYY_WizardsInfo.WizardInfo wizardInfo = YYY_Variables.wizardsInfo.getWizardInfo(wizard.getId());
		boolean hastened = wizardInfo.getHastened() > 0;
		double[][] evasionMatrix = hastened ? YYY_HastenedEvasionMatrix.HASTENED_EVASION_MATRIX : YYY_ShootEvasionMatrix.EVASION_MATRIX;
		YYY_Point prevProjectilePoint = new YYY_Point();
		YYY_Point movementVector = new YYY_Point();
		YYY_Point startPosition = new YYY_Point();
		YYY_Point wizardPosition = new YYY_Point();
		YYY_Point projectilePoint = new YYY_Point();
		double distance;
		double prevDistance;

		int currStepsToAim = 0;
		int maxStepsToAim = (int) ((YYY_FastMath.hypot(self, shootPoint) - 350.) / oneStepMoving);
		if (maxStepsToAim > YYY_Constants.MAX_SHOOT_DETECT_STEP_DISTANCE) {
			maxStepsToAim = YYY_Constants.MAX_SHOOT_DETECT_STEP_DISTANCE;
		}
		int minStepsToAim = 0;
		int freezedStart = wizardInfo.getFrozen();
		int frozenTicks;
		int movementTicks;

		boolean totalConfirmed = false;
		do {
			YYY_Point shootingPosition = new YYY_Point(self.getX() + currStepsToAim * oneStepMoving * Math.cos(shootDirection),
													   self.getY() + currStepsToAim * oneStepMoving * Math.sin(shootDirection));

			double criticalDistance = YYY_Utils.PROJECTIVE_RADIUS[projectileType.ordinal()] + wizard.getRadius();
			boolean hitConfirmed = false;
			for (int i = 0; i != EVASION_CHECK_COUNT; ++i) {
				frozenTicks = freezedStart;
				int intAngle = i * EVASION_CHECK_ANGLE_STEP;
				double angle = intAngle * Math.PI / 180;
				movementVector.update(Math.cos(angle + wizard.getAngle()), Math.sin(angle + wizard.getAngle()));

				if (frozenTicks > 0) {
					distance = 0.;
				} else {
					distance = getDoubleDistance(intAngle, evasionMatrix, 0) * wizardInfo.getMoveFactor();
					if (YYY_Variables.prevActionType != YYY_CurrentAction.ActionType.PURSUIT && currStepsToAim == 0) {
						distance *= .5;
					}
				}
				startPosition.update(wizard.getX() + movementVector.getX() * distance,
									 wizard.getY() + movementVector.getY() * distance);
				boolean stuck = false;
				hitConfirmed = false;
				wizardPosition.update(startPosition);
				projectilePoint.update(shootingPosition);
				distance = 0;
				movementTicks = 0;
				for (int j = 0; j != checkCount; ++j) {
					if (j > 0 && !stuck && frozenTicks < 1) {
						prevDistance = distance;
						distance = getDoubleDistance(intAngle, evasionMatrix, movementTicks) * wizardInfo.getMoveFactor();
						wizardPosition.update(startPosition.getX() + movementVector.getX() * distance,
											  startPosition.getY() + movementVector.getY() * distance);
						stuck = checkStuck(evasionFilter, wizardPosition, j);
						++movementTicks;
						if (stuck) {
							wizardPosition.update(startPosition.getX() + movementVector.getX() * prevDistance,
												  startPosition.getY() + movementVector.getY() * prevDistance);
						}
					}
					--frozenTicks;
					prevProjectilePoint.update(projectilePoint);
					if (j + 1 == checkCount) {
						distance = self.getCastRange() - j * projectileSpeed;
						projectilePoint.update(projectilePoint.getX() + Math.cos(shootDirection) * distance,
											   projectilePoint.getY() + Math.sin(shootDirection) * distance);
					} else {
						projectilePoint.update(projectilePoint.getX() + projectileVector.getX(),
											   projectilePoint.getY() + projectileVector.getY());
					}

					if (YYY_Utils.distancePointToSegment(wizardPosition, prevProjectilePoint, projectilePoint) < criticalDistance) {
						hitConfirmed = true;
						break;
					}
				}
				if (!hitConfirmed) {
					break;
				}
			}
			if (hitConfirmed) {
				if (currStepsToAim == 0) {
					return currStepsToAim;
				}
				totalConfirmed = true;
				maxStepsToAim = currStepsToAim;
			} else {
				minStepsToAim = currStepsToAim;
			}
			currStepsToAim = (maxStepsToAim + minStepsToAim + 1) / 2;
		} while (minStepsToAim + 1 < maxStepsToAim);
		if (totalConfirmed) {
			return maxStepsToAim;
		}

		return YYY_Constants.MAX_SHOOT_DETECT_STEP_DISTANCE;
	}

	private double getDoubleDistance(int angle, double[][] evasionMatrix, int tick) {
		if (angle > 180) {
			angle = 360 - angle;
		}
		return evasionMatrix[angle][tick];
	}

	private boolean checkStuck(YYY_EnemyEvasionFilteredWorld enemyEvasionFilteredWorld, YYY_Point where, int ticks) {
		double distance;
		for (Building building : enemyEvasionFilteredWorld.getBuildingsEvasionCalc()) {
			distance = YYY_FastMath.hypot(building, where);
			if (distance < building.getRadius() + YYY_Constants.getGame().getWizardRadius()) {
				return true;
			}
		}
		for (Tree tree : enemyEvasionFilteredWorld.getTreesEvasionCalc()) {
			distance = YYY_FastMath.hypot(tree, where);
			if (distance < tree.getRadius() + YYY_Constants.getGame().getWizardRadius()) {
				return true;
			}
		}
		YYY_Point updatedPosition = new YYY_Point();
		for (Minion minion : enemyEvasionFilteredWorld.getMinionsEvasionCalc()) {
			updatedPosition.update(minion.getX() + minion.getSpeedX() * ticks,
								   minion.getY() + minion.getSpeedY() * ticks);
			distance = YYY_FastMath.hypot(where, updatedPosition);
			if (distance < minion.getRadius() + YYY_Constants.getGame().getWizardRadius()) {
				return true;
			}
		}
		for (Wizard wizard : enemyEvasionFilteredWorld.getWizardsEvasionCalc()) {
			updatedPosition.update(wizard.getX() + wizard.getSpeedX() * ticks,
								   wizard.getY() + wizard.getSpeedY() * ticks);
			distance = YYY_FastMath.hypot(where, updatedPosition);
			if (distance < wizard.getRadius() + wizard.getRadius()) {
				return true;
			}
		}
		return false;
	}

	public List<ShootDescription> getMissileTargets() {
		return missileTargets;
	}

	public List<ShootDescription> getStaffTargets() {
		return staffTargets;
	}

	public List<ShootDescription> getIceTargets() {
		return iceTargets;
	}

	public List<ShootDescription> getFireTargets() {
		return fireTargets;
	}

	public List<ShootDescription> getHasteTargets() {
		return hasteTargets;
	}

	public List<ShootDescription> getShieldTargets() {
		return shieldTargets;
	}

	public YYY_TargetFinder makeClone() {
		return new YYY_TargetFinder(missileTargets,
									staffTargets,
									iceTargets,
									hasteTargets,
									shieldTargets,
									prevMissileTargets,
									prevStaffTargets,
									prevIceTargets,
									fireTargets);
	}

	public static class ShootDescription implements Comparable<ShootDescription> {
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

		private YYY_Point shootPoint;
		private ActionType actionType;
		private CircularUnit target;

		private double score;

		private static ShootDescription lastInstance;

		// for buffs
		public ShootDescription(Wizard target, ActionType actionType) {
			this.target = target;
			this.actionType = actionType;

			if (target.isMe()) {
				turnAngle = 0;
			} else {
				turnAngle = YYY_Variables.self.getAngleTo(target);
			}
			this.ticksToTurn = Math.max((int) ((Math.abs(turnAngle) - YYY_Constants.MAX_SHOOT_ANGLE + YYY_Variables.maxTurnAngle - .1) / YYY_Variables.maxTurnAngle),
										0);

			this.score = target.isMe() ? 1. : 2.;
			this.score -= YYY_Constants.PER_TURN_TICK_PENALTY * ticksToTurn;
		}

		public ShootDescription(YYY_Point shootPoint, ActionType actionType, double score, LivingUnit target) {
			lastInstance = this;
			this.target = target;
			this.shootPoint = shootPoint;
			this.actionType = actionType;
			this.score = score;

			turnAngle = YYY_Variables.self.getAngleTo(shootPoint.getX(), shootPoint.getY());

			this.ticksToTurn = Math.max((int) ((Math.abs(turnAngle) - YYY_Constants.MAX_SHOOT_ANGLE + YYY_Variables.maxTurnAngle - .1) / YYY_Variables.maxTurnAngle),
										0);

			this.score -= YYY_Constants.PER_TURN_TICK_PENALTY * ticksToTurn;
		}

		public ShootDescription(YYY_Point shootPoint, ActionType actionType, double score, LivingUnit target, double walkingDistance) {
			this(shootPoint, actionType, score, target);
			this.distanceWalkToShoot = walkingDistance;
			if (walkingDistance > 0.) {
				this.ticksToGo = YYY_ShootEvasionMatrix.getTicksForDistance(walkingDistance,
																			(int) Math.round(turnAngle),
																			YYY_Variables.wizardsInfo.getMe().getMoveFactor());
			}
		}

		// for fireball
		public ShootDescription(int minionsDamage,
								int wizardsDamage,
								int buildingDamage,
								int minionsKills,
								int wizardsKills,
								int buildingsDestroy,
								YYY_Point shootPoint,
								double score,
								int ticksToGo) {
			this.actionType = ActionType.FIREBALL;
			this.minionsDamage = minionsDamage;
			this.wizardsDamage = wizardsDamage;
			this.buildingDamage = buildingDamage;
			this.minionsKills = minionsKills;
			this.wizardsKills = wizardsKills;
			this.buildingsDestroy = buildingsDestroy;
			this.shootPoint = shootPoint;
			this.score = score;

			turnAngle = YYY_Variables.self.getAngleTo(shootPoint.getX(), shootPoint.getY());

			this.ticksToTurn = Math.max((int) ((Math.abs(turnAngle) - YYY_Constants.MAX_SHOOT_ANGLE + YYY_Variables.maxTurnAngle - .1) / YYY_Variables.maxTurnAngle),
										0);

			this.score -= YYY_Constants.PER_TURN_TICK_PENALTY * ticksToTurn;
			this.ticksToGo = ticksToGo;
		}

		public void multScore(double mult) {
			this.score *= mult;
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

		public int getTotalDamage() {
			return minionsDamage + wizardsDamage + buildingDamage;
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

		public YYY_Point getShootPoint() {
			return shootPoint;
		}

		public ActionType getActionType() {
			return actionType;
		}

		public CircularUnit getTarget() {
			return target;
		}

		public double getScore() {
			return score;
		}

		@Override
		public int compareTo(ShootDescription o) {
			return o.getScore() == this.score ?
					(target == null ? 0 : Long.compare(o.target.getId(), target.getId())) :
					Double.compare(o.score, score);
		}
	}
}
