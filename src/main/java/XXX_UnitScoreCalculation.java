import model.Bonus;
import model.Building;
import model.Faction;
import model.Minion;
import model.StatusType;
import model.Wizard;

import java.util.HashMap;

/**
 * Created by dragoon on 11/24/16.
 */
public class XXX_UnitScoreCalculation {

	private HashMap<Long, XXX_ScoreCalcStructure> unitsScoreCalc;

	public XXX_UnitScoreCalculation() {
		unitsScoreCalc = new HashMap<>();
	}

	public XXX_UnitScoreCalculation(HashMap<Long, XXX_ScoreCalcStructure> unitsScoreCalc) {
		this.unitsScoreCalc = new HashMap<>(unitsScoreCalc);
	}

	public XXX_ScoreCalcStructure getUnitsScoreCalc(long id) {
		return unitsScoreCalc.get(id);
	}

	public void updateScores(XXX_FilteredWorld filteredWorld, Wizard self, boolean enemyFound, XXX_AgressiveNeutralsCalcs agressiveCalcs) {
		updateScores(filteredWorld, self, enemyFound, agressiveCalcs, 0);
	}

	public void updateScores(XXX_FilteredWorld filteredWorld, Wizard self, boolean enemyFound, XXX_AgressiveNeutralsCalcs agressiveCalcs, int addTicks) {
		unitsScoreCalc.clear();

		for (Bonus bonus : filteredWorld.getBonuses()) {
			if (XXX_FastMath.hypot(self.getX() - bonus.getX(), self.getY() - bonus.getY()) > XXX_Constants.getFightDistanceFilter()) {
				continue;
			}
			XXX_ScoreCalcStructure structure = new XXX_ScoreCalcStructure();
			structure.putItem(XXX_ScoreCalcStructure.createOtherBonusApplyer(self.getRadius() + bonus.getRadius() - .1, 200.));
			unitsScoreCalc.put(bonus.getId(), structure);
		}

		if (XXX_Utils.getTicksToBonusSpawn(filteredWorld.getTickIndex()) < 250) {
			for (int i = 0; i != XXX_BonusesPossibilityCalcs.BONUSES_POINTS.length; ++i) {
				if (XXX_FastMath.hypot(self, XXX_BonusesPossibilityCalcs.BONUSES_POINTS[i]) > XXX_Constants.getFightDistanceFilter()) {
					continue;
				}
				XXX_ScoreCalcStructure structure = new XXX_ScoreCalcStructure();
				structure.putItem(XXX_ScoreCalcStructure.createMinionDangerApplyer(self.getRadius() + XXX_Constants.getGame().getBonusRadius() + .1, 100.));
				structure.putItem(XXX_ScoreCalcStructure
										  .createOtherBonusApplyer(self.getRadius() + XXX_Constants.getGame().getBonusRadius() + XXX_Constants.getGame().getWizardBackwardSpeed() * XXX_Variables.moveFactor,
																   (350 - XXX_Utils.getTicksToBonusSpawn(filteredWorld.getTickIndex())) * .75));
				unitsScoreCalc.put((long) (i - 5), structure);
			}
		}

		if (!enemyFound) {
			return;
		}

		double myDamage = 12. + XXX_Variables.magicDamageBonus;
		if (XXX_Utils.wizardStatusTicks(self, StatusType.EMPOWERED) >= addTicks) {
			myDamage *= XXX_Constants.getGame().getEmpoweredDamageFactor();
		}

		double shieldBonus = XXX_Utils.wizardStatusTicks(self, StatusType.SHIELDED) >= addTicks ?
				(1. - XXX_Constants.getGame().getShieldedDirectDamageAbsorptionFactor()) : 1.;

		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == XXX_Constants.getCurrentFaction()) {
				continue;
			}

			XXX_ScoreCalcStructure structure = new XXX_ScoreCalcStructure();
			double expBonus = XXX_ScanMatrixItem.calcExpBonus(minion.getLife(), minion.getMaxLife(), 1.);
			double movePenalty = XXX_Constants.getGame().getMinionSpeed() * addTicks;
			if (expBonus > 0.) {
				structure.putItem(XXX_ScoreCalcStructure.createExpBonusApplyer(XXX_Constants.EXPERIENCE_DISTANCE - movePenalty, expBonus));
			}
			double damage = 0.;
			boolean dynamicAgro = minion.getFaction() != Faction.NEUTRAL || agressiveCalcs.isMinionAgressive(minion.getId());
			switch (minion.getType()) {
				case ORC_WOODCUTTER:
					damage = minion.getDamage() * shieldBonus * .5;
					structure.putItem(XXX_ScoreCalcStructure.createMinionDangerApplyer(
							XXX_Utils.cooldownDistanceCalculation(XXX_Constants.getGame().getOrcWoodcutterAttackRange() + self.getRadius(),
																  minion.getRemainingActionCooldownTicks() - addTicks) + movePenalty,
							damage));
					break;
				case FETISH_BLOWDART:
					damage = XXX_Constants.getGame().getDartDirectDamage() * shieldBonus; // damage x2 (cd 30)
					structure.putItem(XXX_ScoreCalcStructure.createMinionDangerApplyer(
							XXX_Utils.cooldownDistanceCalculation(XXX_Constants.getGame().getFetishBlowdartAttackRange() + self.getRadius(),
																  minion.getRemainingActionCooldownTicks() - addTicks) + movePenalty,
							damage));
					break;
			}

			if (dynamicAgro) { // enemy and touched neutrals
				structure.putItem(XXX_ScoreCalcStructure.createMinionDangerApplyer(XXX_Utils.getDistanceToNearestAlly(minion,
																													  filteredWorld,
																													  minion.getVisionRange()) +
																						   XXX_Constants.getGame().getMinionSpeed() + .1 + movePenalty,
																				   damage));
				structure.putItem(XXX_ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() - movePenalty,
																				  myDamage * XXX_Constants.MINION_ATTACK_FACTOR));
				structure.putItem(XXX_ScoreCalcStructure.createMeleeAttackBonusApplyer(XXX_Constants.getGame().getStaffRange() + minion.getRadius() - movePenalty,
																					   XXX_Variables.staffDamage));
			}

			unitsScoreCalc.put(minion.getId(), structure);
		}

		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == XXX_Constants.getCurrentFaction()) {
				continue;
			}
			XXX_ScoreCalcStructure structure = new XXX_ScoreCalcStructure();
			double expBonus = XXX_ScanMatrixItem.calcExpBonus(wizard.getLife(), wizard.getMaxLife(), 4.);
			double movePenalty = XXX_Constants.getGame().getWizardForwardSpeed() * addTicks;
			if (expBonus > 0.) {
				structure.putItem(XXX_ScoreCalcStructure.createExpBonusApplyer(XXX_Constants.EXPERIENCE_DISTANCE - movePenalty, expBonus));
			}
			double wizardDamage = 12.;
			if (XXX_Utils.wizardHasStatus(wizard, StatusType.EMPOWERED)) {
				wizardDamage *= XXX_Constants.getGame().getEmpoweredDamageFactor();
			}
			if (self.getLife() < self.getMaxLife() * XXX_Constants.ENEMY_WIZARD_ATTACK_LIFE) {
				structure.putItem(XXX_ScoreCalcStructure.createWizardsDangerApplyer(
						wizard.getCastRange() + self.getRadius() + XXX_Constants.getGame().getWizardForwardSpeed() * 2 + movePenalty,
						wizardDamage * 3. * shieldBonus));
			} else {
				structure.putItem(XXX_ScoreCalcStructure.createWizardsDangerApplyer(
						wizard.getCastRange() +
								movePenalty +
								XXX_Constants.getGame().getWizardForwardSpeed() * Math.min(2, -wizard.getRemainingActionCooldownTicks() - addTicks + 4),
						wizardDamage * shieldBonus));
			}

			structure.putItem(XXX_ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() - movePenalty, myDamage));
			unitsScoreCalc.put(wizard.getId(), structure);
		}

		for (Building building : filteredWorld.getBuildings()) {
			if (building.getFaction() == XXX_Constants.getCurrentFaction()) {
				continue;
			}

			XXX_ScoreCalcStructure structure = new XXX_ScoreCalcStructure();
			double expBonus = XXX_ScanMatrixItem.calcExpBonus(building.getLife(), building.getMaxLife(), 1.);
			if (expBonus > 0.) {
				structure.putItem(XXX_ScoreCalcStructure.createExpBonusApplyer(expBonus));
			}

			int priorityAims = 0;
			if (self.getLife() > building.getDamage()) {
				priorityAims = XXX_Utils.getPrefferedUnitsCountInRange(building,
																	   filteredWorld,
																	   building.getAttackRange(),
																	   building.getDamage(),
																	   self.getLife());
			}

			if (priorityAims < 2) {
				structure.putItem(XXX_ScoreCalcStructure.createBuildingDangerApplyer(building.getAttackRange() + Math.min(2,
																														  -building.getRemainingActionCooldownTicks() - addTicks + 4) * 1.5,
																					 building.getDamage() * shieldBonus));
			} else if (priorityAims == 2) {
				structure.putItem(XXX_ScoreCalcStructure.createBuildingDangerApplyer((building.getAttackRange() + Math.min(2,
																														   -building.getRemainingActionCooldownTicks() - addTicks + 4) * 1.5) * .5,
																					 building.getDamage() * shieldBonus * .5));
			}

			structure.putItem(XXX_ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() + building.getRadius() + XXX_Constants.getGame().getMagicMissileRadius() - .1,
																			  myDamage));
			structure.putItem(XXX_ScoreCalcStructure.createMeleeAttackBonusApplyer(XXX_Constants.getGame().getStaffRange() + building.getRadius() - .1,
																				   XXX_Variables.staffDamage));
			unitsScoreCalc.put(building.getId(), structure);
		}
	}

	public XXX_UnitScoreCalculation makeClone() {
		return new XXX_UnitScoreCalculation(unitsScoreCalc);
	}
}
