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

	public void updateScores(FilteredWorld filteredWorld, Wizard self, boolean enemyFound, AgressiveNeutralsCalcs agressiveCalcs) {
		updateScores(filteredWorld, self, enemyFound, agressiveCalcs, 0);
	}

	public void updateScores(FilteredWorld filteredWorld, Wizard self, boolean enemyFound, AgressiveNeutralsCalcs agressiveCalcs, int addTicks) {
		unitsScoreCalc.clear();

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
				structure.putItem(ScoreCalcStructure.createMinionDangerApplyer(self.getRadius() + Constants.getGame().getBonusRadius() + .1, 100.));
				structure.putItem(ScoreCalcStructure
										  .createOtherBonusApplyer(self.getRadius() + Constants.getGame().getBonusRadius() + Constants.getGame().getWizardBackwardSpeed() * Variables.moveFactor,
																   (350 - Utils.getTicksToBonusSpawn(filteredWorld.getTickIndex())) * .75));
				unitsScoreCalc.put((long) (i - 5), structure);
			}
		}

		if (!enemyFound) {
			return;
		}

		double myDamage = 12. + Variables.magicDamageBonus;
		if (Utils.wizardStatusTicks(self, StatusType.EMPOWERED) >= addTicks) {
			myDamage *= Constants.getGame().getEmpoweredDamageFactor();
		}

		double shieldBonus = Utils.wizardStatusTicks(self, StatusType.SHIELDED) >= addTicks ?
				(1. - Constants.getGame().getShieldedDirectDamageAbsorptionFactor()) : 1.;

		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}

			ScoreCalcStructure structure = new ScoreCalcStructure();
			double expBonus = ScanMatrixItem.calcExpBonus(minion.getLife(), minion.getMaxLife(), 1.);
			if (expBonus > 0.) {
				structure.putItem(ScoreCalcStructure.createExpBonusApplyer(expBonus));
			}
			double damage = 0.;
			boolean dynamicAgro = minion.getFaction() != Faction.NEUTRAL || agressiveCalcs.isMinionAgressive(minion.getId());
			switch (minion.getType()) {
				case ORC_WOODCUTTER:
					damage = minion.getDamage() * shieldBonus * .5;
					structure.putItem(ScoreCalcStructure.createMinionDangerApplyer(
							Utils.cooldownDistanceCalculation(Constants.getGame().getOrcWoodcutterAttackRange() + self.getRadius(),
															  minion.getRemainingActionCooldownTicks()),
							damage));
					break;
				case FETISH_BLOWDART:
					damage = Constants.getGame().getDartDirectDamage() * shieldBonus * .5;
					structure.putItem(ScoreCalcStructure.createMinionDangerApplyer(
							Utils.cooldownDistanceCalculation(Constants.getGame().getFetishBlowdartAttackRange() + self.getRadius(),
															  minion.getRemainingActionCooldownTicks()),
							damage));
					break;
			}

			if (dynamicAgro) { // enemy and touched neutrals
				structure.putItem(ScoreCalcStructure.createMinionDangerApplyer(Utils.getDistanceToNearestAlly(minion,
																											  filteredWorld,
																											  minion.getVisionRange()) + .1,
																			   damage));
				structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange(),
																			  myDamage * Constants.MINION_ATTACK_FACTOR));
				structure.putItem(ScoreCalcStructure.createMeleeAttackBonusApplyer(Constants.getGame().getStaffRange() + minion.getRadius(),
																				   Variables.staffDamage));
			}

			unitsScoreCalc.put(minion.getId(), structure);
		}

		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}
			ScoreCalcStructure structure = new ScoreCalcStructure();
			double expBonus = ScanMatrixItem.calcExpBonus(wizard.getLife(), wizard.getMaxLife(), 4.);
			if (expBonus > 0.) {
				structure.putItem(ScoreCalcStructure.createExpBonusApplyer(expBonus));
			}
			double wizardDamage = 12.;
			if (Utils.wizardHasStatus(wizard, StatusType.EMPOWERED)) {
				wizardDamage *= Constants.getGame().getEmpoweredDamageFactor();
			}
			if (self.getLife() < self.getMaxLife() * Constants.ENEMY_WIZARD_ATTACK_LIFE) {
				structure.putItem(ScoreCalcStructure.createWizardsDangerApplyer(
						wizard.getCastRange() + self.getRadius() + Constants.getGame().getWizardForwardSpeed() * 2,
						wizardDamage * 3. * shieldBonus));
			} else {
				structure.putItem(ScoreCalcStructure.createWizardsDangerApplyer(
						wizard.getCastRange() + Constants.getGame().getWizardForwardSpeed() * Math.min(2, -wizard.getRemainingActionCooldownTicks() + 4),
						wizardDamage * shieldBonus));
			}

			structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange(), myDamage));
			unitsScoreCalc.put(wizard.getId(), structure);
		}

		for (Building building : filteredWorld.getBuildings()) {
			if (building.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}

			ScoreCalcStructure structure = new ScoreCalcStructure();
			double expBonus = ScanMatrixItem.calcExpBonus(building.getLife(), building.getMaxLife(), 1.);
			if (expBonus > 0.) {
				structure.putItem(ScoreCalcStructure.createExpBonusApplyer(expBonus));
			}

			int priorityAims = 0;
			if (self.getLife() > building.getDamage()) {
				priorityAims = Utils.getPrefferedUnitsCountInRange(building, filteredWorld, building.getAttackRange(), building.getDamage(), self.getLife());

			}

			if (priorityAims < 2) {
				structure.putItem(ScoreCalcStructure.createBuildingDangerApplyer(building.getAttackRange() + Math.min(2,
																													  -building.getRemainingActionCooldownTicks() + 4) * 1.5,
																				 building.getDamage() * shieldBonus));
			} else if (priorityAims == 2) {
				structure.putItem(ScoreCalcStructure.createBuildingDangerApplyer((building.getAttackRange() + Math.min(2,
																													   -building.getRemainingActionCooldownTicks() + 4) * 1.5) * .5,
																				 building.getDamage() * shieldBonus * .5));
			}

			structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() + building.getRadius() + Constants.getGame().getMagicMissileRadius() - .1,
																		  myDamage));
			structure.putItem(ScoreCalcStructure.createMeleeAttackBonusApplyer(Constants.getGame().getStaffRange() + building.getRadius() - .1,
																			   Variables.staffDamage));
			unitsScoreCalc.put(building.getId(), structure);
		}
	}

	public UnitScoreCalculation makeClone() {
		return new UnitScoreCalculation(unitsScoreCalc);
	}
}
