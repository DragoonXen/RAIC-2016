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
public class TargetFinder {

	//public final static Comparator<Pair<Double, Point>> POINT_AIM_SORT_COMPARATOR = (o1, o2) -> o2.getFirst().compareTo(o1.getFirst());
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
	private FilteredWorld filteredWorld;
	private AgressiveNeutralsCalcs agressiveNeutralsCalcs;
	private WizardsInfo.WizardInfo myWizardInfo;

	public TargetFinder() {
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

	public TargetFinder(List<ShootDescription> missileTargets,
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

	public void updateTargets(FilteredWorld filteredWorld,
							  BaseLine myLineCalc,
							  Point pointToReach,
							  AgressiveNeutralsCalcs agressiveNeutralsCalcs,
							  int stuck) {
		this.self = Variables.self;
		this.filteredWorld = filteredWorld;
		this.agressiveNeutralsCalcs = agressiveNeutralsCalcs;
		this.myWizardInfo = Variables.wizardsInfo.getMe();

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

		boolean treeCut = stuck > 10 || (myLineCalc == PositionMoveLine.INSTANCE &&
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
				score = Constants.CUT_REACH_POINT_DISTANCE_PTIORITY / FastMath.hypot(tree, pointToReach);
				distanceToTarget = FastMath.hypot(self, tree);
				score += Constants.CUT_SELF_DISTANCE_PRIORITY / distanceToTarget;

				score *= (tree.getRadius() + self.getRadius()) * .02;
				direction = tree.getAngleTo(self);
				Point backShootPoint = new Point(tree.getX() + Math.cos(direction) * tree.getRadius(),
												 tree.getY() + Math.sin(direction) * tree.getRadius());
				missileTargets.add(new ShootDescription(backShootPoint,
														ActionType.MAGIC_MISSILE,
														score / Utils.getHitsToKill(tree.getLife(), missileDamage) -
																Constants.CUT_REACH_POINT_DISTANCE_PTIORITY,
														tree));

				if (distanceToTarget < Constants.getGame().getStaffRange() + tree.getRadius() + 50) {
					distanceToTarget -= Constants.getGame().getStaffRange() + tree.getRadius();
					staffTargets.add(new ShootDescription(backShootPoint,
														  ActionType.STAFF,
														  score / Utils.getHitsToKill(tree.getLife(), staffDamage) -
																  Constants.CUT_REACH_POINT_DISTANCE_PTIORITY,
														  tree,
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
			Point shootPoint = new Point(minion.getX() + Math.cos(minion.getAngle()) * minion.getRadius(),
										 minion.getY() + Math.sin(minion.getAngle()) * minion.getRadius());

			if (self.getCastRange() < FastMath.hypot(self, shootPoint)) {
				continue;
			}
			score = Utils.getMinionAttackPriority(minion, missileDamage, self);
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

			appendStaffTarget(minion, Utils.getMinionAttackPriority(minion, staffDamage, self), staffDamage);
			if (frostBoltDamage > 0) {
				score = Utils.getMinionAttackPriority(minion, frostBoltDamage, self);
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

		for (BuildingPhantom building : filteredWorld.getBuildings()) {
			if (building.getFaction() == Constants.getCurrentFaction() || building.isInvulnerable()) {
				continue;
			}
			score = Constants.LOW_AIM_SCORE;
			double tmp = (building.getMaxLife() - building.getLife()) / (double) building.getMaxLife();
			score += tmp * tmp;
			score *= Constants.BUILDING_AIM_PROIRITY;
			direction = Utils.normalizeAngle(building.getAngleTo(self) + building.getAngle());
			Point backShootPoint = new Point(building.getX() + Math.cos(direction) * building.getRadius(),
											 building.getY() + Math.sin(direction) * building.getRadius());
			if (self.getCastRange() + Utils.PROJECTIVE_RADIUS[ProjectileType.MAGIC_MISSILE.ordinal()]
					< FastMath.hypot(self, backShootPoint) + .1) {
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

			// staff
			direction = Utils.normalizeAngle(wizard.getAngleTo(self) + wizard.getAngle());
			appendStaffTarget(wizard,
							  score,
							  new Point(wizard.getX() + Math.cos(direction) * wizard.getRadius(),
										wizard.getY() + Math.sin(direction) * wizard.getRadius()),
							  staffDamage);
			EnemyEvasionFilteredWorld evastionFiltering = new EnemyEvasionFilteredWorld(wizard, Variables.world);
			// MM
			Point pointFwdToShoot = new Point(wizard.getX() + Math.cos(wizard.getAngle()) * ShootEvasionMatrix.mmDistanceFromCenter,
											  wizard.getY() + Math.sin(wizard.getAngle()) * ShootEvasionMatrix.mmDistanceFromCenter);
			int checked = checkEnemyWizardEvasion(wizard, pointFwdToShoot, oneStepMoving, evastionFiltering, ProjectileType.MAGIC_MISSILE);
			int checkedSecond;
			if (checked == 0) {
				appendShootTarget(wizard, score, pointFwdToShoot, missileDamage, ActionType.MAGIC_MISSILE);
			} else {
				Point pointToShoot = new Point(wizard.getX(), wizard.getY());
				checkedSecond = checkEnemyWizardEvasion(wizard, pointToShoot, oneStepMoving, evastionFiltering, ProjectileType.MAGIC_MISSILE);
				if (checkedSecond < checked) {
					appendShootTarget(wizard, score, pointToShoot, missileDamage, ActionType.MAGIC_MISSILE);
					ShootDescription.lastInstance.setTicksToGo(checkedSecond);
				} else if (checked < Constants.MAX_SHOOT_DETECT_STEP_DISTANCE) {
					appendShootTarget(wizard, score, pointFwdToShoot, missileDamage, ActionType.MAGIC_MISSILE);
					ShootDescription.lastInstance.setTicksToGo(checked);
				}
			}

			// Frost
			if (frostBoltDamage > 0) {
				pointFwdToShoot = new Point(wizard.getX() + Math.cos(wizard.getAngle()) * ShootEvasionMatrix.frostBoltDistanceFromCenter,
											wizard.getY() + Math.sin(wizard.getAngle()) * ShootEvasionMatrix.frostBoltDistanceFromCenter);
				checked = checkEnemyWizardEvasion(wizard, pointFwdToShoot, oneStepMoving, evastionFiltering, ProjectileType.FROST_BOLT);
				if (checked == 0) {
					appendShootTarget(wizard, score, pointFwdToShoot, missileDamage, ActionType.FROST_BOLT);
				} else {
					Point pointToShoot = new Point(wizard.getX(), wizard.getY());
					checkedSecond = checkEnemyWizardEvasion(wizard, pointToShoot, oneStepMoving, evastionFiltering, ProjectileType.FROST_BOLT);
					if (checkedSecond < checked) {
						appendShootTarget(wizard, score, pointToShoot, missileDamage, ActionType.FROST_BOLT);
						ShootDescription.lastInstance.setTicksToGo(checkedSecond);
					} else if (checked < Constants.MAX_SHOOT_DETECT_STEP_DISTANCE) {
						appendShootTarget(wizard, score, pointFwdToShoot, missileDamage, ActionType.FROST_BOLT);
						ShootDescription.lastInstance.setTicksToGo(checked);
					}
				}
			}
		}

		if (myWizardInfo.isHasFireball()) {
			fireTargets.clear();
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
					ShootDescription shootDescription = checkFireballDamage(wizardPoint);
					fireTargets.add(shootDescription);
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
			Collections.sort(fireTargets);
			for (Iterator<ShootDescription> iterator = fireTargets.iterator(); iterator.hasNext(); ) {
				ShootDescription fireTarget = iterator.next();
				if (Utils.noTreesOnWay(fireTarget.getShootPoint(), self, ProjectileType.FIREBALL, filteredWorld)) {
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

		if (myWizardInfo.isHasHasteSkill()) {
			for (Wizard wizard : filteredWorld.getWizards()) {
				if (wizard.getFaction() != Constants.getCurrentFaction()) {
					continue;
				}
				if (Variables.wizardsInfo.getWizardInfo(wizard.getId()).getHastened() < 30 &&
						FastMath.hypot(self, wizard) < self.getCastRange()) {
					hasteTargets.add(new ShootDescription(wizard, ActionType.HASTE));
				}
			}
			if (myWizardInfo.getHastened() < 30) {
				hasteTargets.add(new ShootDescription(self, ActionType.HASTE));
			}
		}

		if (myWizardInfo.isHasShieldSkill()) {
			for (Wizard wizard : filteredWorld.getWizards()) {
				if (wizard.getFaction() != Constants.getCurrentFaction()) {
					continue;
				}
				if (Variables.wizardsInfo.getWizardInfo(wizard.getId()).getShielded() < 30 &&
						FastMath.hypot(self, wizard) < self.getCastRange()) {
					shieldTargets.add(new ShootDescription(wizard, ActionType.SHIELD));
				}
				if (myWizardInfo.getShielded() < 30) {
					hasteTargets.add(new ShootDescription(self, ActionType.SHIELD));
				}
			}
		}

		Collections.sort(staffTargets);
		Collections.sort(missileTargets);
		Collections.sort(iceTargets);
		Collections.sort(hasteTargets);
		Collections.sort(shieldTargets);

		missileTargets = filterTargets(missileTargets);
		iceTargets = filterTargets(iceTargets);
		fireTargets = filterTargets(fireTargets);
	}

	private final static int angleCheck = 20;
	private final static int checkCount = 360 / angleCheck;
	private final static double angleCheckRadians = Math.PI / 180. * angleCheck;

	private ShootDescription checkDistances(Point point, double distance) {
		ShootDescription bestPoint = null;
		for (int i = 0; i != checkCount; ++i) {
			Point checkPoint = new Point(point.getX() + distance * Math.cos(angleCheckRadians * i), point.getY() + distance * Math.sin(angleCheckRadians * i));
			if (FastMath.hypot(self, checkPoint) > self.getCastRange()) {
				continue;
			}
			ShootDescription temp = checkFireballDamage(checkPoint);
			if (bestPoint == null || temp.getScore() > bestPoint.getScore()) {
				bestPoint = temp;
			}
		}
		return bestPoint;
	}

	public ShootDescription checkFireballDamage(Point where) {
		int ticksToFly = Utils.getTicksToFly(FastMath.hypot(self, where), Utils.PROJECTIVE_SPEED[ProjectileType.FIREBALL.ordinal()]);
		int minionsDamage = 0;
		int minionKills = 0;
		int buildingsDamage = 0;
		int buildingsDestroys = 0;
		int wizardsDamage = 0;
		int wizardsKills = 0;
		double score = 0.;
		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}
			if (minion.getFaction() == Faction.NEUTRAL && !agressiveNeutralsCalcs.isMinionAgressive(minion.getId())) {
				continue;
			}

			Point checkPoint = new Point(minion.getX() + minion.getSpeedY() * ticksToFly, minion.getY() + minion.getSpeedY() * ticksToFly);
			double distance = FastMath.hypot(checkPoint, where) - minion.getRadius();
			if (distance < Constants.getGame().getFireballExplosionMinDamageRange()) {
				double damage;
				if (distance < Constants.getGame().getFireballExplosionMaxDamageRange()) {
					damage = myWizardInfo.getFireballMaxDamage();
				} else {
					distance -= Constants.getGame().getFireballExplosionMaxDamageRange();
					damage = myWizardInfo.getFireballMaxDamage() -
							(int) ((Constants.getFireballLowerindDamageDistance() + distance) / Constants.getFireballLowerindDamageDistance());
				}
				if (damage + Constants.getGame().getBurningSummaryDamage() / 2 >= minion.getLife()) {
					minionsDamage += minion.getLife();
					++minionKills;
				} else {
					minionsDamage += damage + Constants.getGame().getBurningSummaryDamage() / 2;
				}
				minionsDamage += Math.min(damage + Constants.getGame().getBurningSummaryDamage() / 2, minion.getLife());
				score += Math.min(damage + Constants.getGame().getBurningSummaryDamage() / 2, minion.getLife());
			}
		}

		for (BuildingPhantom building : filteredWorld.getBuildings()) {
			if (building.getFaction() == Constants.getCurrentFaction() || building.isInvulnerable()) {
				continue;
			}

			double distance = FastMath.hypot(building, where) - building.getRadius();
			if (distance < Constants.getGame().getFireballExplosionMinDamageRange()) {
				double damage;
				if (distance < Constants.getGame().getFireballExplosionMaxDamageRange()) {
					damage = Constants.getGame().getFireballExplosionMaxDamage();
				} else {
					distance -= Constants.getGame().getFireballExplosionMaxDamageRange();
					damage = myWizardInfo.getFireballMaxDamage() -
							(int) ((Constants.getFireballLowerindDamageDistance() + distance) / Constants.getFireballLowerindDamageDistance());
				}
				damage = Math.min(damage + Constants.getGame().getBurningSummaryDamage(), building.getLife());
				if (damage == building.getLife()) {
					++buildingsDestroys;
				}
				buildingsDamage += damage;
				score += damage * 1.5;
			}
		}
		Pair<Double, Pair<Integer, Boolean>> wizardsDamageCalc;
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
		return new ShootDescription(minionsDamage,
									wizardsDamage,
									buildingsDamage,
									minionKills,
									wizardsKills,
									buildingsDestroys,
									where,
									score);
	}

	private Pair<Double, Pair<Integer, Boolean>> checkFirePointsWizard(Wizard wizard, Point where, int ticksToFly) {
		ticksToFly = Math.min(ticksToFly - 1, ShootEvasionMatrix.EVASION_MATRIX[0].length - 1);
		double distance = FastMath.hypot(wizard, where) - wizard.getRadius();
		if (wizard.getFaction() == Constants.getEnemyFaction()) {
			// TODO: fix this calculation
			distance += ShootEvasionMatrix.EVASION_MATRIX[0][ticksToFly];
		} else if (wizard.isMe()) {
			distance -= ShootEvasionMatrix.EVASION_MATRIX[0][ticksToFly] * Variables.wizardsInfo.getMe().getMoveFactor();
		}

		int damage;

		if (distance <= Constants.getGame().getFireballExplosionMinDamageRange()) {
			double score;
			if (distance <= Constants.getGame().getFireballExplosionMaxDamageRange()) {
				score = myWizardInfo.getFireballMaxDamage();
			} else {
				distance -= Constants.getGame().getFireballExplosionMaxDamageRange();
				score = myWizardInfo.getFireballMaxDamage() -
						(int) ((Constants.getFireballLowerindDamageDistance() + distance) / Constants.getFireballLowerindDamageDistance());
			}
			damage = Math.min((int) score + Constants.getGame().getBurningSummaryDamage(),
							  wizard.getLife() + Constants.getGame().getBurningSummaryDamage() / 2);
			boolean killed = false;
			score = Math.min(score + Constants.getGame().getBurningSummaryDamage(), wizard.getLife()) * 3.;
			if (wizard.getLife() < score + Constants.getGame().getBurningSummaryDamage() * .5 &&
					wizard.getFaction() == Constants.getEnemyFaction()) {
				killed = true;
				score += 65.;
			}
			if (wizard.getFaction() == Constants.getCurrentFaction()) {
				if (wizard.isMe()) {
					return new Pair<>(-score * 5, null);
				} else {
					return new Pair<>(-score, null);
				}
			} else {
				return new Pair<>(score, new Pair<>(damage, killed));
			}
		}
		return null;
	}

	private void addFireTarget(ShootDescription shootDescription) {
		if (shootDescription != null) {
			fireTargets.add(shootDescription);
		}
	}

	private List<ShootDescription> filterTargets(List<ShootDescription> shootDescriptions) {
		List<ShootDescription> filtered = new ArrayList<>();
		ShootDescription lastAdded = null;
		Point selfPoint = new Point(self.getX(), self.getY());
		for (ShootDescription shootDescription : shootDescriptions) {
			if (lastAdded != null && lastAdded.getTicksToGo() <= shootDescription.getTicksToGo()) {
				continue;
			}
			Point shootTo = shootDescription.getShootPoint();
			boolean canShoot = true;
			double radius = Utils.PROJECTIVE_RADIUS[shootDescription.getActionType().ordinal() - 2];
			for (Tree tree : FilteredWorld.lastInstance.getShootingTreeList()) {
				if (tree == shootDescription.getTarget()) {
					// skip tree if it's a target
					continue;
				}
				double distance = Utils.distancePointToSegment(new Point(tree.getX(), tree.getY()), shootTo, selfPoint);
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

	private void appendStaffTarget(Minion minion, double score, int damage) {
		double distanceToTarget = FastMath.hypot(self, minion) - minion.getRadius() - Constants.getGame().getStaffRange();
		if (distanceToTarget < 50) {
			double direction = Utils.normalizeAngle(minion.getAngleTo(self) + minion.getAngle());
			Point backShootPoint = new Point(minion.getX() + Math.cos(direction) * minion.getRadius(),
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

	private void appendStaffTarget(Building building, double score, Point shootPoint, int damage) {
		double distanceToTarget = FastMath.hypot(self, shootPoint) - Constants.getGame().getStaffRange();
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

	private void appendStaffTarget(Wizard wizard, double score, Point shootPoint, int damage) {
		double distanceToTarget = FastMath.hypot(self, shootPoint) - Constants.getGame().getStaffRange();
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

	private void appendShootTarget(Wizard wizard, double score, Point shootPoint, int damage, ActionType actionType) {
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
				sd.multScore(Constants.FROST_WIZARD_AIM_PROIRITY);
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
		double projectileSpeed = Utils.PROJECTIVE_SPEED[projectileType.ordinal()];
		int checkCount = (int) ((self.getCastRange() + projectileSpeed - .1) / projectileSpeed);
		double shootDirection = Utils.normalizeAngle(self.getAngleTo(shootPoint.getX(), shootPoint.getY()) + self.getAngle());
		Point projectileVector = new Point(Math.cos(shootDirection) * projectileSpeed,
										   Math.sin(shootDirection) * projectileSpeed);
		WizardsInfo.WizardInfo wizardInfo = Variables.wizardsInfo.getWizardInfo(wizard.getId());
		boolean hastened = wizardInfo.getHastened() > 0;
		double[][] evasionMatrix = hastened ? HastenedEvasionMatrix.HASTENED_EVASION_MATRIX : ShootEvasionMatrix.EVASION_MATRIX;
		Point prevProjectilePoint = new Point();
		Point movementVector = new Point();
		Point startPosition = new Point();
		Point wizardPosition = new Point();
		Point projectilePoint = new Point();
		double distance;
		double prevDistance;

		int currStepsToAim = 0;
		int maxStepsToAim = (int) ((FastMath.hypot(self, shootPoint) - 350.) / stepDistance);
		if (maxStepsToAim > Constants.MAX_SHOOT_DETECT_STEP_DISTANCE) {
			maxStepsToAim = Constants.MAX_SHOOT_DETECT_STEP_DISTANCE;
		}
		int minStepsToAim = 0;

		boolean totalConfirmed = false;
		do {
			Point shootingPosition = new Point(self.getX() + currStepsToAim * stepDistance * Math.cos(shootDirection),
											   self.getY() + currStepsToAim * stepDistance * Math.sin(shootDirection));

			double criticalDistance = Utils.PROJECTIVE_RADIUS[projectileType.ordinal()] + wizard.getRadius();
			boolean hitConfirmed = false;
			for (int i = 0; i != EVASION_CHECK_COUNT; ++i) {
				int intAngle = i * EVASION_CHECK_ANGLE_STEP;
				double angle = intAngle * Math.PI / 180;
				movementVector.update(Math.cos(angle + wizard.getAngle()), Math.sin(angle + wizard.getAngle()));
				distance = getDoubleDistance(intAngle, evasionMatrix, 0) * wizardInfo.getMoveFactor();
				if (Variables.prevActionType != CurrentAction.ActionType.PURSUIT && currStepsToAim == 0) {
					distance *= .5;
				}
				startPosition.update(wizard.getX() + movementVector.getX() * distance,
									 wizard.getY() + movementVector.getY() * distance);
				boolean stuck = false;
				hitConfirmed = false;
				wizardPosition.update(startPosition);
				projectilePoint.update(shootingPosition);
				distance = 0;
				for (int j = 0; j != checkCount; ++j) {
					if (j > 0 && !stuck) {
						prevDistance = distance;
						distance = getDoubleDistance(intAngle, evasionMatrix, j - 1) * wizardInfo.getMoveFactor();
						wizardPosition.update(startPosition.getX() + movementVector.getX() * distance,
											  startPosition.getY() + movementVector.getY() * distance);
						stuck = checkStuck(evasionFilter, wizardPosition, j + 1);
						if (stuck) {
							wizardPosition.update(startPosition.getX() + movementVector.getX() * prevDistance,
												  startPosition.getY() + movementVector.getY() * prevDistance);
						}
					}
					prevProjectilePoint.update(projectilePoint);
					if (j + 1 == checkCount) {
						distance = self.getCastRange() - j * projectileSpeed;
						projectilePoint.update(projectilePoint.getX() + Math.cos(shootDirection) * distance,
											   projectilePoint.getY() + Math.sin(shootDirection) * distance);
					} else {
						projectilePoint.update(projectilePoint.getX() + projectileVector.getX(),
											   projectilePoint.getY() + projectileVector.getY());
					}

					if (Utils.distancePointToSegment(wizardPosition, prevProjectilePoint, projectilePoint) < criticalDistance) {
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

		return Constants.MAX_SHOOT_DETECT_STEP_DISTANCE;
	}

	private double getDoubleDistance(int angle, double[][] evasionMatrix, int tick) {
		if (angle > 180) {
			angle = 360 - angle;
		}
		return evasionMatrix[angle][tick];
	}

	private boolean checkStuck(EnemyEvasionFilteredWorld enemyEvasionFilteredWorld, Point where, int ticks) {
		double distance;
		for (Building building : enemyEvasionFilteredWorld.getBuildingsEvasionCalc()) {
			distance = FastMath.hypot(building, where);
			if (distance < building.getRadius() + Constants.getGame().getWizardRadius()) {
				return true;
			}
		}
		for (Tree tree : enemyEvasionFilteredWorld.getTreesEvasionCalc()) {
			distance = FastMath.hypot(tree, where);
			if (distance < tree.getRadius() + Constants.getGame().getWizardRadius()) {
				return true;
			}
		}
		Point updatedPosition = new Point();
		for (Minion minion : enemyEvasionFilteredWorld.getMinionsEvasionCalc()) {
			updatedPosition.update(minion.getX() + minion.getSpeedX() * ticks,
								   minion.getY() + minion.getSpeedY() * ticks);
			distance = FastMath.hypot(where, updatedPosition);
			if (distance < minion.getRadius() + Constants.getGame().getWizardRadius()) {
				return true;
			}
		}
		for (Wizard wizard : enemyEvasionFilteredWorld.getWizardsEvasionCalc()) {
			updatedPosition.update(wizard.getX() + wizard.getSpeedX() * ticks,
								   wizard.getY() + wizard.getSpeedY() * ticks);
			distance = FastMath.hypot(where, updatedPosition);
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

	public TargetFinder makeClone() {
		return new TargetFinder(missileTargets,
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

		private Point shootPoint;
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
				turnAngle = Variables.self.getAngleTo(target);
			}
			this.ticksToTurn = Math.max((int) ((Math.abs(turnAngle) - Constants.MAX_SHOOT_ANGLE + Variables.maxTurnAngle - .1) / Variables.maxTurnAngle), 0);

			this.score = target.isMe() ? 1. : 2.;
			this.score -= Constants.PER_TURN_TICK_PENALTY * ticksToTurn;
		}

		public ShootDescription(Point shootPoint, ActionType actionType, double score, LivingUnit target) {
			lastInstance = this;
			this.target = target;
			this.shootPoint = shootPoint;
			this.actionType = actionType;
			this.score = score;

			turnAngle = Variables.self.getAngleTo(shootPoint.getX(), shootPoint.getY());

			this.ticksToTurn = Math.max((int) ((Math.abs(turnAngle) - Constants.MAX_SHOOT_ANGLE + Variables.maxTurnAngle - .1) / Variables.maxTurnAngle), 0);

			this.score -= Constants.PER_TURN_TICK_PENALTY * ticksToTurn;
		}

		public ShootDescription(Point shootPoint, ActionType actionType, double score, LivingUnit target, double walkingDistance) {
			this(shootPoint, actionType, score, target);
			this.distanceWalkToShoot = walkingDistance;
			if (walkingDistance > 0.) {
				this.ticksToGo = ShootEvasionMatrix.getTicksForDistance(walkingDistance,
																		(int) Math.round(turnAngle),
																		Variables.wizardsInfo.getMe().getMoveFactor());
			}
		}

		// for fireball
		public ShootDescription(int minionsDamage,
								int wizardsDamage,
								int buildingDamage,
								int minionsKills,
								int wizardsKills,
								int buildingsDestroy,
								Point shootPoint,
								double score) {
			this.actionType = ActionType.FIREBALL;
			this.minionsDamage = minionsDamage;
			this.wizardsDamage = wizardsDamage;
			this.buildingDamage = buildingDamage;
			this.minionsKills = minionsKills;
			this.wizardsKills = wizardsKills;
			this.buildingsDestroy = buildingsDestroy;
			this.shootPoint = shootPoint;
			this.score = score;

			turnAngle = Variables.self.getAngleTo(shootPoint.getX(), shootPoint.getY());

			this.ticksToTurn = Math.max((int) ((Math.abs(turnAngle) - Constants.MAX_SHOOT_ANGLE + Variables.maxTurnAngle - .1) / Variables.maxTurnAngle), 0);

			this.score -= Constants.PER_TURN_TICK_PENALTY * ticksToTurn;
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
