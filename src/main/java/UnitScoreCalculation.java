import model.ActionType;
import model.Bonus;
import model.BuildingType;
import model.Faction;
import model.Minion;
import model.ProjectileType;
import model.StatusType;
import model.Wizard;

import java.util.HashMap;

/**
 * Created by dragoon on 11/24/16.
 */
public class UnitScoreCalculation {

	private HashMap<Long, ScoreCalcStructure> unitsScoreCalc;

	public UnitScoreCalculation() {
		unitsScoreCalc = new HashMap<>();
	}

	public UnitScoreCalculation(HashMap<Long, ScoreCalcStructure> unitsScoreCalc) {
		this.unitsScoreCalc = new HashMap<>(unitsScoreCalc);
	}

	public ScoreCalcStructure getUnitsScoreCalc(long id) {
		return unitsScoreCalc.get(id);
	}

	public void updateScores(FilteredWorld filteredWorld, Wizard self, FightStatus status, AgressiveNeutralsCalcs agressiveCalcs) {
		updateScores(filteredWorld, self, status, agressiveCalcs, 0);
	}

	public void updateScores(FilteredWorld filteredWorld, Wizard self, FightStatus status, AgressiveNeutralsCalcs agressiveCalcs, int addTicks) {
		unitsScoreCalc.clear();

		WizardsInfo.WizardInfo myWizardInfo = Variables.wizardsInfo.getMe();
		for (Bonus bonus : filteredWorld.getBonuses()) {
			if (FastMath.hypot(self.getX() - bonus.getX(), self.getY() - bonus.getY()) > Constants.getFightDistanceFilter()) {
				continue;
			}
			ScoreCalcStructure structure = new ScoreCalcStructure();
			structure.putItem(ScoreCalcStructure.createOtherBonusApplyer(self.getRadius() + bonus.getRadius() - .1, 200.));
			unitsScoreCalc.put(bonus.getId(), structure);
		}

		if (Utils.getTicksToBonusSpawn(filteredWorld.getTickIndex()) < 250) {
			for (int i = 0; i != BonusesPossibilityCalcs.BONUSES_POINTS.length; ++i) {
				if (FastMath.hypot(self, BonusesPossibilityCalcs.BONUSES_POINTS[i]) > Constants.getFightDistanceFilter()) {
					continue;
				}
				ScoreCalcStructure structure = new ScoreCalcStructure();
				structure.putItem(ScoreCalcStructure.createOtherDangerApplyer(self.getRadius() + Constants.getGame().getBonusRadius() + .1, 100.));
				structure.putItem(ScoreCalcStructure
										  .createOtherBonusApplyer(self.getRadius() + Constants.getGame().getBonusRadius() + Constants.getGame().getWizardBackwardSpeed() *
																		   myWizardInfo.getMoveFactor(),
																   (266. - Utils.getTicksToBonusSpawn(filteredWorld.getTickIndex())) * .75));
				unitsScoreCalc.put((long) (i - 5), structure);
			}
		}

		if (status == FightStatus.NO_ENEMY) {
			return;
		}

		int myDamage = myWizardInfo.getMagicalMissileDamage(addTicks);
		int staffDamage = myWizardInfo.getStaffDamage(addTicks);
		double shieldBonus = Utils.wizardStatusTicks(self, StatusType.SHIELDED) > addTicks ?
				(1. - Constants.getGame().getShieldedDirectDamageAbsorptionFactor()) : 1.;

		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}

			ScoreCalcStructure structure = new ScoreCalcStructure();
			double expBonus = ScanMatrixItem.calcExpBonus(minion.getLife(), minion.getMaxLife(), 1.);
			double movePenalty = Constants.getGame().getMinionSpeed() * addTicks;
			if (expBonus > 0.) {
				structure.putItem(ScoreCalcStructure.createExpBonusApplyer(Constants.EXPERIENCE_DISTANCE - movePenalty, expBonus));
			}
			double damage = 0.;
			boolean dynamicAgro = minion.getFaction() != Faction.NEUTRAL || agressiveCalcs.isMinionAgressive(minion.getId());
			switch (minion.getType()) {
				case ORC_WOODCUTTER:
					damage = minion.getDamage() * shieldBonus * .5;
					structure.putItem(ScoreCalcStructure.createMinionDangerApplyer(
							Utils.cooldownDistanceMinionsCalculation(Constants.getGame().getOrcWoodcutterAttackRange() + self.getRadius(),
																	 minion.getRemainingActionCooldownTicks() - addTicks) + movePenalty,
							damage));
					break;
				case FETISH_BLOWDART:
					damage = Constants.getGame().getDartDirectDamage() * shieldBonus; // damage x2 (cd 30)
					structure.putItem(ScoreCalcStructure.createMinionDangerApplyer(
							Utils.cooldownDistanceMinionsCalculation(Constants.getGame().getFetishBlowdartAttackRange() + self.getRadius(),
																	 minion.getRemainingActionCooldownTicks() - addTicks) + movePenalty,
							damage));
					break;
			}

			if (dynamicAgro) { // enemy and touched neutrals
				structure.putItem(ScoreCalcStructure.createMinionDangerApplyer(Utils.getDistanceToNearestAlly(minion,
																											  filteredWorld,
																											  minion.getVisionRange()) +
																					   Constants.getGame().getMinionSpeed() + .1 + movePenalty,
																			   damage));
				if (minion.getFaction() != Faction.NEUTRAL || filteredWorld.getTickIndex() > 700) {
					structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() - movePenalty,
																				  myDamage * Constants.MINION_ATTACK_FACTOR));
					structure.putItem(ScoreCalcStructure.createMeleeAttackBonusApplyer(Constants.getGame().getStaffRange() + minion.getRadius() - movePenalty,
																					   staffDamage * Constants.MINION_ATTACK_FACTOR));
				}
			}

			unitsScoreCalc.put(minion.getId(), structure);
		}

		double backwardMoveBuildingSpeed = myWizardInfo.getMoveFactor() * Constants.getGame().getWizardBackwardSpeed() * .66;

		boolean enemyCanAttack = false;
		double dangerZone;

		if (Constants.AGRESSIVE_PUSH_WIZARD_LIFE * self.getMaxLife() > self.getLife()) {
			staffDamage *= 4.;
			myDamage *= 4.;
		}
		for (BuildingPhantom building : filteredWorld.getBuildings()) {
			if (building.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}

			ScoreCalcStructure structure = new ScoreCalcStructure();
			unitsScoreCalc.put(building.getId(), structure);

			int priorityAims = 0;
			if (self.getLife() > building.getDamage()) {
				priorityAims = Utils.getPrefferedUnitsCountInRange(building, filteredWorld, building.getAttackRange(), building.getDamage(), self.getLife());
			}

			dangerZone = 0.;
			ScoreCalcStructure.ScoreItem scoreItem = null;
			if (priorityAims < 2) {
				dangerZone = building.getAttackRange() + Math.min(2, -building.getRemainingActionCooldownTicks() - addTicks + 8) * backwardMoveBuildingSpeed;
				scoreItem = ScoreCalcStructure.createBuildingDangerApplyer(dangerZone, building.getDamage() * shieldBonus);
			} else if (priorityAims == 2) {
				dangerZone = (building.getAttackRange() + Math.min(2,
																   -building.getRemainingActionCooldownTicks() - addTicks + 8) * backwardMoveBuildingSpeed) * .5;
				scoreItem = ScoreCalcStructure.createBuildingDangerApplyer(dangerZone, building.getDamage() * shieldBonus * .5);
			}
			if (dangerZone > FastMath.hypot(self, building)) {
				enemyCanAttack = true;
			}

			if (building.isInvulnerable()) {
				if (scoreItem != null) {
					scoreItem.setScore(scoreItem.getScore() * 1.5);
					structure.putItem(scoreItem);
				}
				continue;
			}
			// charge base
			if (building.getType() != BuildingType.FACTION_BASE || Variables.attackPoint == null) {
				if (scoreItem != null) {
					structure.putItem(scoreItem);
				}
			} else {
				structure.putItem(ScoreCalcStructure.createOtherBonusApplyer(300., 300.));
			}
			double expBonus = ScanMatrixItem.calcExpBonus(building.getLife(), building.getMaxLife(), 2.);
			if (expBonus > 0.) {
				structure.putItem(ScoreCalcStructure.createExpBonusApplyer(expBonus));
			}

			if (myWizardInfo.isHasFireball()) {
				structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() + building.getRadius() + Constants.getGame().getFireballExplosionMinDamageRange() - .1,
																			  myDamage * Constants.BUILDING_ATTACK_FACTOR));
			} else {
				structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() + building.getRadius() + Constants.getGame().getMagicMissileRadius() - .1,
																			  myDamage * Constants.BUILDING_ATTACK_FACTOR));
			}
			structure.putItem(ScoreCalcStructure.createMeleeAttackBonusApplyer(Constants.getGame().getStaffRange() + building.getRadius() - .1,
																			   staffDamage * Constants.BUILDING_ATTACK_FACTOR));
		}

		double negateDistance = 0.;
		if (SchemeSelector.antmsu) {
			int myCooldown = 1000;
			myCooldown = Math.min(myCooldown, myWizardInfo.getActionCooldown(ActionType.MAGIC_MISSILE));
			if (myWizardInfo.isHasFrostBolt()) {
				myCooldown = Math.min(myCooldown, myWizardInfo.getActionCooldown(ActionType.FROST_BOLT));
			}
			if (myWizardInfo.isHasFireball()) {
				myCooldown = Math.min(myCooldown, myWizardInfo.getActionCooldown(ActionType.FIREBALL));
			}

			negateDistance = 15.;
			int ticksToWalk = ShootEvasionMatrix.getTicksForDistance(negateDistance, 6, myWizardInfo.getMoveFactor());
			if (ticksToWalk >= myCooldown) {
				myCooldown = Math.max(0, myCooldown - 1);
				negateDistance = (1. - ((double) myCooldown) / ticksToWalk) * negateDistance;
			} else {
				myCooldown -= ticksToWalk;
				negateDistance = -((double) myCooldown) / ticksToWalk * negateDistance;
			}
		}

		boolean meHasFrostSkill = myWizardInfo.isHasFrostBolt();
		int absorbMagicDamageBonus = myWizardInfo.getAbsorbMagicBonus();
		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}
			ScoreCalcStructure structure = new ScoreCalcStructure();
			unitsScoreCalc.put(wizard.getId(), structure);
			double expBonus = ScanMatrixItem.calcExpBonus(wizard.getLife(), wizard.getMaxLife(), 4.);
			double movePenalty = Constants.getGame().getWizardForwardSpeed() * addTicks;
			if (expBonus > 0.) {
				structure.putItem(ScoreCalcStructure.createExpBonusApplyer(Constants.EXPERIENCE_DISTANCE - movePenalty, expBonus));
			}

			structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() - movePenalty, myDamage));

			structure.putItem(ScoreCalcStructure.createMeleeAttackBonusApplyer(Constants.getGame().getStaffRange() + wizard.getRadius() - .1, staffDamage));
			if (Variables.attackWizardId != null && Variables.attackWizardId == wizard.getId() &&
					self.getLife() > self.getMaxLife() * Constants.ATTACK_ENEMY_WIZARD_LIFE) {
				int cnt = 0;
				for (Wizard wizardCheck : Variables.world.getWizards()) {
					if (wizardCheck.getFaction() == Constants.getCurrentFaction() &&
							FastMath.hypot(wizardCheck, wizard) < 600) {
						++cnt;
					}
				}
				if (cnt > 2) {
					structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(350, 24));
					continue;
				}
			}

			//attackWizardId
			WizardsInfo.WizardInfo wizardInfo = Variables.wizardsInfo.getWizardInfo(wizard.getId());

			// create missile danger description
			WizardsDangerInfo missileDangerInfo;
			int wizardDamage = wizardInfo.getMagicalMissileDamage();
			if (wizardInfo.isHasFastMissileCooldown()) {
				wizardDamage *= 2; // x2 shot speed
			}
			wizardDamage *= shieldBonus;
			if (absorbMagicDamageBonus > 0) {
				wizardDamage -= absorbMagicDamageBonus;
			}

			int addCooldownTurnOrFreeze = Utils.wizardStatusTicks(wizard, StatusType.FROZEN);
			int ticksToTurn = Math.max((int) ((Math.abs(wizard.getAngleTo(self)) - Constants.MAX_SHOOT_ANGLE + Variables.maxTurnAngle - .1) /
											   Variables.maxTurnAngle) - addTicks,
									   0);
			addCooldownTurnOrFreeze = Math.max(addCooldownTurnOrFreeze, ticksToTurn);
			if (self.getLife() < self.getMaxLife() * Constants.ATTACK_ENEMY_WIZARD_LIFE) {
				double range = ShootEvasionMatrix.getCorrectDistance(myWizardInfo.getMoveFactor()) +
						Constants.getGame().getWizardForwardSpeed() * wizardInfo.getMoveFactor() * 3. +
						self.getRadius();
				missileDangerInfo = new WizardsDangerInfo(wizardDamage, range);
			} else {
				double evasionRange = ShootEvasionMatrix.getCorrectDistance(myWizardInfo.getMoveFactor());

				double range = wizardInfo.getCastRange();
				int ticksToFly;
				if (SchemeSelector.antmsu) {
					ticksToFly = Utils.getTicksToFly(range - self.getRadius(), Constants.getGame().getMagicMissileSpeed());
					range += self.getRadius() +
							Constants.getGame().getMagicMissileRadius() -
							ShootEvasionMatrix.getBackwardDistanceCanWalkInTicks(ticksToFly - 1, myWizardInfo.getMoveFactor()) +
							Constants.getGame().getWizardForwardSpeed() * wizardInfo.getMoveFactor();
				} else {
					ticksToFly = Utils.getTicksToFly(range - self.getRadius(), Constants.getGame().getMagicMissileSpeed());
					range += self.getRadius() +
							Constants.getGame().getMagicMissileRadius() -
							ShootEvasionMatrix.getBackwardDistanceCanWalkInTicks(ticksToFly - 1, myWizardInfo.getMoveFactor()) +
							Constants.getGame().getWizardForwardSpeed() * wizardInfo.getMoveFactor();
				}
				range = Math.min(evasionRange, range - negateDistance) +
						Utils.cooldownDistanceWizardCalculation(wizardInfo.getMoveFactor(),
																Math.max(wizardInfo.getActionCooldown(ActionType.MAGIC_MISSILE),
																		 addCooldownTurnOrFreeze) - addTicks);
				missileDangerInfo = new WizardsDangerInfo(wizardDamage, range);
			}

			// create frost danger description
			boolean frost = wizardInfo.isHasFrostBolt();
			WizardsDangerInfo frostDangerInfo = null;
			if (frost) {
				wizardDamage = wizardInfo.getFrostBoltDamage() * 2; // very dangerous cause freezing
				wizardDamage *= shieldBonus;
				if (absorbMagicDamageBonus > 0) {
					wizardDamage -= absorbMagicDamageBonus;
				}
				// TODO: change frost danger radius
				// same range as magic missile, but more dangerous
				if (self.getLife() < self.getMaxLife() * Constants.ATTACK_ENEMY_WIZARD_LIFE) {
					double range = ShootEvasionMatrix.getCorrectDistance(myWizardInfo.getMoveFactor()) +
							Constants.getGame().getWizardForwardSpeed() * wizardInfo.getMoveFactor() * 3. +
							self.getRadius();
					frostDangerInfo = new WizardsDangerInfo(wizardDamage, range);
				} else {
					double evasionRange = ShootEvasionMatrix.getCorrectDistance(myWizardInfo.getMoveFactor());

					double range = wizardInfo.getCastRange();
					int ticksToFly = Utils.getTicksToFly(range - self.getRadius(), Constants.getGame().getMagicMissileSpeed());
					range += self.getRadius() +
							Constants.getGame().getMagicMissileRadius() -
							ShootEvasionMatrix.getBackwardDistanceCanWalkInTicks(ticksToFly - 1, myWizardInfo.getMoveFactor()) +
							Constants.getGame().getWizardForwardSpeed() * wizardInfo.getMoveFactor();
					range = Math.min(evasionRange, range) +
							Utils.cooldownDistanceWizardCalculation(wizardInfo.getMoveFactor(),
																	Math.max(wizardInfo.getActionCooldown(ActionType.FROST_BOLT),
																			 addCooldownTurnOrFreeze) - addTicks);
					frostDangerInfo = new WizardsDangerInfo(wizardDamage, range);
				}
			}

			boolean fire = wizardInfo.isHasFireball();
			WizardsDangerInfo fireDangerInfo = null;
			if (fire) {
				wizardDamage = wizardInfo.getFireballMaxDamage();
				wizardDamage *= shieldBonus;
				if (absorbMagicDamageBonus > 0) {
					wizardDamage -= absorbMagicDamageBonus;
				}
				wizardDamage += Constants.getGame().getBurningSummaryDamage();
				if (self.getLife() < self.getMaxLife() * Constants.ATTACK_ENEMY_WIZARD_LIFE) {
					// keep as far as possible
					fireDangerInfo = new WizardsDangerInfo(wizardDamage,
														   wizardInfo.getCastRange() + Constants.getGame().getFireballExplosionMinDamageRange() + self.getRadius());
				} else {
					double range;
					if (meHasFrostSkill && self.getMana() >= Constants.getGame().getFrostBoltManacost()) {
						range = 450;
					} else {
						int ticksToFlly = Utils.getTicksToFly(wizardInfo.getCastRange(), Utils.PROJECTIVE_SPEED[ProjectileType.FIREBALL.ordinal()]);
						double distance = ShootEvasionMatrix.getBackwardDistanceCanWalkInTicks(ticksToFlly, myWizardInfo.getMoveFactor());
						range = wizardInfo.getCastRange() + Constants.getGame().getFireballExplosionMinDamageRange() - distance +
								Utils.cooldownDistanceWizardCalculation(wizardInfo.getMoveFactor(),
																		Math.max(wizardInfo.getActionCooldown(ActionType.FIREBALL),
																				 addCooldownTurnOrFreeze) - addTicks);
					}
					fireDangerInfo = new WizardsDangerInfo(wizardDamage, range);
				}
			}

			// TODO: fix this, innacurate
			if (!frost && meHasFrostSkill && self.getMana() >= Constants.getGame().getFrostBoltManacost()) {
				structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(500, 12));
			}
			dangerZone = 0.;
			if (frostDangerInfo != null) {
				if (fireDangerInfo != null) {
					dangerZone = fireDangerInfo.dangerRadius;
					structure.putItem(ScoreCalcStructure.createWizardsDangerApplyer(fireDangerInfo.dangerRadius, fireDangerInfo.dangerDamage));
				}
				dangerZone = Math.max(frostDangerInfo.dangerRadius, dangerZone);
				structure.putItem(ScoreCalcStructure.createWizardsDangerApplyer(frostDangerInfo.dangerRadius, frostDangerInfo.dangerDamage));
			} else {
				structure.putItem(ScoreCalcStructure.createWizardsDangerApplyer(missileDangerInfo.dangerRadius, missileDangerInfo.dangerDamage));
				dangerZone = missileDangerInfo.dangerRadius;
				if (fireDangerInfo != null) {
					structure.putItem(ScoreCalcStructure.createWizardsDangerApplyer(fireDangerInfo.dangerRadius, fireDangerInfo.dangerDamage));
					dangerZone = Math.max(fireDangerInfo.dangerRadius, dangerZone);
				}
			}

			if (dangerZone > FastMath.hypot(self, wizard)) {
				enemyCanAttack = true;
			}

			if (!wizardInfo.isHasFastMissileCooldown()) {
				structure.putItem(ScoreCalcStructure.createWizardsDangerApplyer(Constants.getGame().getStaffRange() + wizard.getRadius() - .1,
																				wizardInfo.getStaffDamage(addTicks)));
			}
		}

		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == Constants.getEnemyFaction()) {
				continue;
			}
			ScoreCalcStructure structure = new ScoreCalcStructure();
			structure.putItem(ScoreCalcStructure.createOtherBonusApplyer(600., 100.));
			if (!enemyCanAttack) {
				structure.putItem(ScoreCalcStructure.createOtherDangerApplyer(SchemeSelector.antmsu ? 80. : 95., 200.));
			}
			unitsScoreCalc.put(wizard.getId(), structure);
		}
	}

	public UnitScoreCalculation makeClone() {
		return new UnitScoreCalculation(unitsScoreCalc);
	}

	private static class WizardsDangerInfo {
		private int dangerDamage;
		private double dangerRadius;

		public WizardsDangerInfo(int dangerDamage, double dangerRadius) {
			this.dangerDamage = dangerDamage;
			this.dangerRadius = dangerRadius;
		}
	}
}
