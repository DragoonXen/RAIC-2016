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
public class YYY_UnitScoreCalculation {

	private HashMap<Long, YYY_ScoreCalcStructure> unitsScoreCalc;

	public YYY_UnitScoreCalculation() {
		unitsScoreCalc = new HashMap<>();
	}

	public YYY_UnitScoreCalculation(HashMap<Long, YYY_ScoreCalcStructure> unitsScoreCalc) {
		this.unitsScoreCalc = new HashMap<>(unitsScoreCalc);
	}

	public YYY_ScoreCalcStructure getUnitsScoreCalc(long id) {
		return unitsScoreCalc.get(id);
	}

	public void updateScores(YYY_FilteredWorld filteredWorld, Wizard self, YYY_FightStatus status, YYY_AgressiveNeutralsCalcs agressiveCalcs) {
		updateScores(filteredWorld, self, status, agressiveCalcs, 0);
	}

	public void updateScores(YYY_FilteredWorld filteredWorld, Wizard self, YYY_FightStatus status, YYY_AgressiveNeutralsCalcs agressiveCalcs, int addTicks) {
		unitsScoreCalc.clear();

		YYY_WizardsInfo.WizardInfo myWizardInfo = YYY_Variables.wizardsInfo.getMe();
		for (Bonus bonus : filteredWorld.getBonuses()) {
			if (YYY_FastMath.hypot(self.getX() - bonus.getX(), self.getY() - bonus.getY()) > YYY_Constants.getFightDistanceFilter()) {
				continue;
			}
			YYY_ScoreCalcStructure structure = new YYY_ScoreCalcStructure();
			structure.putItem(YYY_ScoreCalcStructure.createOtherBonusApplyer(self.getRadius() + bonus.getRadius() - .1, 200.));
			unitsScoreCalc.put(bonus.getId(), structure);
		}

		if (YYY_Utils.getTicksToBonusSpawn(filteredWorld.getTickIndex()) < 250) {
			for (int i = 0; i != YYY_BonusesPossibilityCalcs.BONUSES_POINTS.length; ++i) {
				if (YYY_FastMath.hypot(self, YYY_BonusesPossibilityCalcs.BONUSES_POINTS[i]) > YYY_Constants.getFightDistanceFilter()) {
					continue;
				}
				YYY_ScoreCalcStructure structure = new YYY_ScoreCalcStructure();
				structure.putItem(YYY_ScoreCalcStructure.createOtherDangerApplyer(self.getRadius() + YYY_Constants.getGame().getBonusRadius() + .1, 100.));
				structure.putItem(YYY_ScoreCalcStructure
										  .createOtherBonusApplyer(self.getRadius() + YYY_Constants.getGame().getBonusRadius() + YYY_Constants.getGame().getWizardBackwardSpeed() *
																		   myWizardInfo.getMoveFactor(),
																   (266. - YYY_Utils.getTicksToBonusSpawn(filteredWorld.getTickIndex())) * .75));
				unitsScoreCalc.put((long) (i - 5), structure);
			}
		}

		if (status == YYY_FightStatus.NO_ENEMY) {
			return;
		}

		int myDamage = myWizardInfo.getMagicalMissileDamage(addTicks);
		int staffDamage = myWizardInfo.getStaffDamage(addTicks);
		double shieldBonus = YYY_Utils.wizardStatusTicks(self, StatusType.SHIELDED) > addTicks ?
				(1. - YYY_Constants.getGame().getShieldedDirectDamageAbsorptionFactor()) : 1.;

		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == YYY_Constants.getCurrentFaction()) {
				continue;
			}

			YYY_ScoreCalcStructure structure = new YYY_ScoreCalcStructure();
			double expBonus = YYY_ScanMatrixItem.calcExpBonus(minion.getLife(), minion.getMaxLife(), 1.);
			double movePenalty = YYY_Constants.getGame().getMinionSpeed() * addTicks;
			if (expBonus > 0.) {
				structure.putItem(YYY_ScoreCalcStructure.createExpBonusApplyer(YYY_Constants.EXPERIENCE_DISTANCE - movePenalty, expBonus));
			}
			double damage = 0.;
			boolean dynamicAgro = minion.getFaction() != Faction.NEUTRAL || agressiveCalcs.isMinionAgressive(minion.getId());
			switch (minion.getType()) {
				case ORC_WOODCUTTER:
					damage = minion.getDamage() * shieldBonus * .5;
					structure.putItem(YYY_ScoreCalcStructure.createMinionDangerApplyer(
							YYY_Utils.cooldownDistanceMinionsCalculation(YYY_Constants.getGame().getOrcWoodcutterAttackRange() + self.getRadius(),
																		 minion.getRemainingActionCooldownTicks() - addTicks) + movePenalty,
							damage));
					break;
				case FETISH_BLOWDART:
					damage = YYY_Constants.getGame().getDartDirectDamage() * shieldBonus; // damage x2 (cd 30)
					structure.putItem(YYY_ScoreCalcStructure.createMinionDangerApplyer(
							YYY_Utils.cooldownDistanceMinionsCalculation(YYY_Constants.getGame().getFetishBlowdartAttackRange() + self.getRadius(),
																		 minion.getRemainingActionCooldownTicks() - addTicks) + movePenalty,
							damage));
					break;
			}

			if (dynamicAgro) { // enemy and touched neutrals
				structure.putItem(YYY_ScoreCalcStructure.createMinionDangerApplyer(YYY_Utils.getDistanceToNearestAlly(minion,
																													  filteredWorld,
																													  minion.getVisionRange()) +
																						   YYY_Constants.getGame().getMinionSpeed() + .1 + movePenalty,
																				   damage));
				structure.putItem(YYY_ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() - movePenalty,
																				  myDamage * YYY_Constants.MINION_ATTACK_FACTOR));
				structure.putItem(YYY_ScoreCalcStructure.createMeleeAttackBonusApplyer(YYY_Constants.getGame().getStaffRange() + minion.getRadius() - movePenalty,
																					   staffDamage * YYY_Constants.MINION_ATTACK_FACTOR));
			}

			unitsScoreCalc.put(minion.getId(), structure);
		}

		for (YYY_BuildingPhantom building : filteredWorld.getBuildings()) {
			if (building.getFaction() == YYY_Constants.getCurrentFaction()) {
				continue;
			}

			YYY_ScoreCalcStructure structure = new YYY_ScoreCalcStructure();
			unitsScoreCalc.put(building.getId(), structure);

			int priorityAims = 0;
			if (self.getLife() > building.getDamage() && building.getType() == BuildingType.GUARDIAN_TOWER) {
				priorityAims = YYY_Utils.getPrefferedUnitsCountInRange(building,
																	   filteredWorld,
																	   building.getAttackRange(),
																	   building.getDamage(),
																	   self.getLife());
			}

			if (priorityAims < 2) {
				structure.putItem(YYY_ScoreCalcStructure.createBuildingDangerApplyer(building.getAttackRange() + Math.min(2,
																														  -building.getRemainingActionCooldownTicks() - addTicks + 4) * 1.5,
																					 building.getDamage() * shieldBonus));
			} else if (priorityAims == 2) {
				structure.putItem(YYY_ScoreCalcStructure.createBuildingDangerApplyer((building.getAttackRange() + Math.min(2,
																														   -building.getRemainingActionCooldownTicks() - addTicks + 4) * 1.5) * .5,
																					 building.getDamage() * shieldBonus * .5));
			}
			if (building.isInvulnerable()) {
				continue;
			}
			double expBonus = YYY_ScanMatrixItem.calcExpBonus(building.getLife(), building.getMaxLife(), 2.);
			if (expBonus > 0.) {
				structure.putItem(YYY_ScoreCalcStructure.createExpBonusApplyer(expBonus));
			}

			if (myWizardInfo.isHasFireball()) {
				structure.putItem(YYY_ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() + building.getRadius() + YYY_Constants.getGame().getFireballExplosionMinDamageRange() - .1,
																				  myDamage * YYY_Constants.BUILDING_ATTACK_FACTOR));
			} else {
				structure.putItem(YYY_ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() + building.getRadius() + YYY_Constants.getGame().getMagicMissileRadius() - .1,
																				  myDamage * YYY_Constants.BUILDING_ATTACK_FACTOR));
			}
			structure.putItem(YYY_ScoreCalcStructure.createMeleeAttackBonusApplyer(YYY_Constants.getGame().getStaffRange() + building.getRadius() - .1,
																				   staffDamage * YYY_Constants.BUILDING_ATTACK_FACTOR));
		}

		if (YYY_Constants.AGRESSIVE_PUSH_WIZARD_LIFE * self.getMaxLife() > self.getLife()) {
			staffDamage *= 3.;
			myDamage *= 3.;
		}

		boolean meHasFrostSkill = myWizardInfo.isHasFrostBolt();
		int absorbMagicDamageBonus = myWizardInfo.getAbsorbMagicBonus();
		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == YYY_Constants.getCurrentFaction()) {
				continue;
			}
			YYY_ScoreCalcStructure structure = new YYY_ScoreCalcStructure();
			double expBonus = YYY_ScanMatrixItem.calcExpBonus(wizard.getLife(), wizard.getMaxLife(), 4.);
			double movePenalty = YYY_Constants.getGame().getWizardForwardSpeed() * addTicks;
			if (expBonus > 0.) {
				structure.putItem(YYY_ScoreCalcStructure.createExpBonusApplyer(YYY_Constants.EXPERIENCE_DISTANCE - movePenalty, expBonus));
			}
			YYY_WizardsInfo.WizardInfo wizardInfo = YYY_Variables.wizardsInfo.getWizardInfo(wizard.getId());

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

			int addCooldownTurnOrFreeze = YYY_Utils.wizardStatusTicks(wizard, StatusType.FROZEN);
			int ticksToTurn = Math.max((int) ((Math.abs(wizard.getAngleTo(self)) - YYY_Constants.MAX_SHOOT_ANGLE + YYY_Variables.maxTurnAngle - .1) /
											   YYY_Variables.maxTurnAngle) - addTicks,
									   0);
			addCooldownTurnOrFreeze = Math.max(addCooldownTurnOrFreeze, ticksToTurn);
			if (self.getLife() < self.getMaxLife() * YYY_Constants.ATTACK_ENEMY_WIZARD_LIFE) {
				double range = YYY_ShootEvasionMatrix.getCorrectDistance(myWizardInfo.getMoveFactor()) +
						YYY_Constants.getGame().getWizardForwardSpeed() * wizardInfo.getMoveFactor() * 3. +
						self.getRadius();
				missileDangerInfo = new WizardsDangerInfo(wizardDamage, range);
			} else {
				double evasionRange = YYY_ShootEvasionMatrix.getCorrectDistance(myWizardInfo.getMoveFactor());

				double range = wizardInfo.getCastRange();
				int ticksToFly = YYY_Utils.getTicksToFly(range - self.getRadius(), YYY_Constants.getGame().getMagicMissileSpeed());
				range += self.getRadius() +
						YYY_Constants.getGame().getMagicMissileRadius() -
						YYY_ShootEvasionMatrix.getBackwardDistanceCanWalkInTicks(ticksToFly - 1, myWizardInfo.getMoveFactor()) +
						YYY_Constants.getGame().getWizardForwardSpeed() * wizardInfo.getMoveFactor();
				range = Math.min(evasionRange, range) +
						YYY_Utils.cooldownDistanceWizardCalculation(wizardInfo.getMoveFactor(),
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
				if (self.getLife() < self.getMaxLife() * YYY_Constants.ATTACK_ENEMY_WIZARD_LIFE) {
					double range = YYY_ShootEvasionMatrix.getCorrectDistance(myWizardInfo.getMoveFactor()) +
							YYY_Constants.getGame().getWizardForwardSpeed() * wizardInfo.getMoveFactor() * 3. +
							self.getRadius();
					frostDangerInfo = new WizardsDangerInfo(wizardDamage, range);
				} else {
					double evasionRange = YYY_ShootEvasionMatrix.getCorrectDistance(myWizardInfo.getMoveFactor());

					double range = wizardInfo.getCastRange();
					int ticksToFly = YYY_Utils.getTicksToFly(range - self.getRadius(), YYY_Constants.getGame().getMagicMissileSpeed());
					range += self.getRadius() +
							YYY_Constants.getGame().getMagicMissileRadius() -
							YYY_ShootEvasionMatrix.getBackwardDistanceCanWalkInTicks(ticksToFly - 1, myWizardInfo.getMoveFactor()) +
							YYY_Constants.getGame().getWizardForwardSpeed() * wizardInfo.getMoveFactor();
					range = Math.min(evasionRange, range) +
							YYY_Utils.cooldownDistanceWizardCalculation(wizardInfo.getMoveFactor(),
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
				wizardDamage += YYY_Constants.getGame().getBurningSummaryDamage();
				if (self.getLife() < self.getMaxLife() * YYY_Constants.ATTACK_ENEMY_WIZARD_LIFE) {
					// keep as far as possible
					fireDangerInfo = new WizardsDangerInfo(wizardDamage,
														   wizardInfo.getCastRange() + YYY_Constants.getGame().getFireballExplosionMinDamageRange() + self.getRadius());
				} else {
					double range;
					if (meHasFrostSkill && self.getMana() >= YYY_Constants.getGame().getFrostBoltManacost()) {
						range = 450;
					} else {
						int ticksToFlly = YYY_Utils.getTicksToFly(wizardInfo.getCastRange(), YYY_Utils.PROJECTIVE_SPEED[ProjectileType.FIREBALL.ordinal()]);
						double distance = YYY_ShootEvasionMatrix.getBackwardDistanceCanWalkInTicks(ticksToFlly, myWizardInfo.getMoveFactor());
						range = wizardInfo.getCastRange() + YYY_Constants.getGame().getFireballExplosionMinDamageRange() - distance +
								YYY_Utils.cooldownDistanceWizardCalculation(wizardInfo.getMoveFactor(),
																			Math.max(wizardInfo.getActionCooldown(ActionType.FIREBALL),
																					 addCooldownTurnOrFreeze) - addTicks);
					}
					fireDangerInfo = new WizardsDangerInfo(wizardDamage, range);
				}
			}

			// TODO: fix this, innacurate
			if (!frost && meHasFrostSkill && self.getMana() >= YYY_Constants.getGame().getFrostBoltManacost()) {
				structure.putItem(YYY_ScoreCalcStructure.createAttackBonusApplyer(500, 12));
			}
			if (frostDangerInfo != null) {
				if (fireDangerInfo != null) {
					structure.putItem(YYY_ScoreCalcStructure.createWizardsDangerApplyer(fireDangerInfo.dangerRadius, fireDangerInfo.dangerDamage));
				}
				structure.putItem(YYY_ScoreCalcStructure.createWizardsDangerApplyer(frostDangerInfo.dangerRadius, frostDangerInfo.dangerDamage));
			} else {
				structure.putItem(YYY_ScoreCalcStructure.createWizardsDangerApplyer(missileDangerInfo.dangerRadius, missileDangerInfo.dangerDamage));
				if (fireDangerInfo != null) {
					structure.putItem(YYY_ScoreCalcStructure.createWizardsDangerApplyer(fireDangerInfo.dangerRadius, fireDangerInfo.dangerDamage));
				}
			}

			structure.putItem(YYY_ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() - movePenalty, myDamage));

			structure.putItem(YYY_ScoreCalcStructure.createMeleeAttackBonusApplyer(YYY_Constants.getGame().getStaffRange() + wizard.getRadius() - .1,
																				   staffDamage));
			if (!wizardInfo.isHasFastMissileCooldown()) {
				structure.putItem(YYY_ScoreCalcStructure.createWizardsDangerApplyer(YYY_Constants.getGame().getStaffRange() + wizard.getRadius() - .1,
																					wizardInfo.getStaffDamage(addTicks)));
			}
			unitsScoreCalc.put(wizard.getId(), structure);
		}
	}

	public YYY_UnitScoreCalculation makeClone() {
		return new YYY_UnitScoreCalculation(unitsScoreCalc);
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
