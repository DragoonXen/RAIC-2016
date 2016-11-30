import model.Bonus;
import model.Building;
import model.BuildingType;
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
				structure.putItem(ScoreCalcStructure.createMinionDangerApplyer(self.getRadius() + Constants.getGame().getBonusRadius() + .1, 100.));
				structure.putItem(ScoreCalcStructure
										  .createOtherBonusApplyer(self.getRadius() + Constants.getGame().getBonusRadius() + Constants.getGame().getWizardBackwardSpeed() *
																		   myWizardInfo.getMoveFactor(),
																   (350 - Utils.getTicksToBonusSpawn(filteredWorld.getTickIndex())) * .75));
				unitsScoreCalc.put((long) (i - 5), structure);
			}
		}

		if (!enemyFound) {
			return;
		}

		int myDamage = myWizardInfo.getMagicalMissileDamage(addTicks);
		int staffDamage = myWizardInfo.getStaffDamage(addTicks);
		double shieldBonus = Utils.wizardStatusTicks(self, StatusType.SHIELDED) >= addTicks ?
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
							Utils.cooldownDistanceCalculation(Constants.getGame().getOrcWoodcutterAttackRange() + self.getRadius(),
															  minion.getRemainingActionCooldownTicks() - addTicks) + movePenalty,
							damage));
					break;
				case FETISH_BLOWDART:
					damage = Constants.getGame().getDartDirectDamage() * shieldBonus; // damage x2 (cd 30)
					structure.putItem(ScoreCalcStructure.createMinionDangerApplyer(
							Utils.cooldownDistanceCalculation(Constants.getGame().getFetishBlowdartAttackRange() + self.getRadius(),
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
				structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() - movePenalty,
																			  myDamage * Constants.MINION_ATTACK_FACTOR));
				structure.putItem(ScoreCalcStructure.createMeleeAttackBonusApplyer(Constants.getGame().getStaffRange() + minion.getRadius() - movePenalty,
																				   staffDamage * Constants.MINION_ATTACK_FACTOR));
			}

			unitsScoreCalc.put(minion.getId(), structure);
		}

		boolean meHasFrostSkill = myWizardInfo.isHasFrostBolt();

		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}
			ScoreCalcStructure structure = new ScoreCalcStructure();
			double expBonus = ScanMatrixItem.calcExpBonus(wizard.getLife(), wizard.getMaxLife(), 4.);
			double movePenalty = Constants.getGame().getWizardForwardSpeed() * addTicks;
			if (expBonus > 0.) {
				structure.putItem(ScoreCalcStructure.createExpBonusApplyer(Constants.EXPERIENCE_DISTANCE - movePenalty, expBonus));
			}
			WizardsInfo.WizardInfo wizardInfo = Variables.wizardsInfo.getWizardInfo(wizard.getId());
			double wizardDamage = wizardInfo.getMagicalMissileDamage();
			if (wizardInfo.isHasFastMissileCooldown()) {
				wizardDamage *= 2; // x2 shot speed
			}
			boolean frost = wizardInfo.isHasFrostBolt();
			if (frost) {
				wizardDamage = wizardInfo.getFrostBoltDamage() * 2; // very dangerous cause freezing
			}
			boolean fire = wizardInfo.isHasFireball();
			if (fire) {
				wizardDamage = wizardInfo.getFireballMaxDamage() + Constants.getGame().getBurningSummaryDamage();
			}
			int freezeStatus = Utils.wizardStatusTicks(wizard, StatusType.FROZEN);
			if (self.getLife() < self.getMaxLife() * Constants.ATTACK_ENEMY_WIZARD_LIFE) {
				double range = ShootEvasionMatrix.getCorrectDistance(myWizardInfo.getMoveFactor()) +
						Constants.getGame().getWizardForwardSpeed() * wizardInfo.getMoveFactor() * 3. +
						self.getRadius();
				if (fire) {
					range = wizardInfo.getCastRange() + Constants.getGame().getFireballExplosionMinDamageRange() + self.getRadius();
				}
				structure.putItem(ScoreCalcStructure.createWizardsDangerApplyer(
						range + movePenalty,
						wizardDamage * 3. * shieldBonus));
			} else {
				double range = Math.min(ShootEvasionMatrix.getCorrectDistance(myWizardInfo.getMoveFactor()),
										wizardInfo.getCastRange() + ShootEvasionMatrix.distanceFromCenter * 3.) +
						Constants.getGame().getWizardForwardSpeed() * myWizardInfo.getMoveFactor() * .5 *
								Math.min(2,
										 -Math.max(wizard.getRemainingActionCooldownTicks(),
												   freezeStatus) - addTicks + 4);
				if (fire) {
					if (meHasFrostSkill && self.getMana() >= Constants.getGame().getFrostBoltManacost()) {
						range = 450;
					} else {
						range = wizardInfo.getCastRange() + Constants.getGame().getFireballExplosionMinDamageRange();
					}
				}
				structure.putItem(ScoreCalcStructure.createWizardsDangerApplyer(range + movePenalty, wizardDamage * shieldBonus));
			}

			if (!frost && meHasFrostSkill && self.getMana() >= Constants.getGame().getFrostBoltManacost()) {
				structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(500, 12));
			}

			structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() - movePenalty, myDamage));
			unitsScoreCalc.put(wizard.getId(), structure);
		}

		for (Building building : filteredWorld.getBuildings()) {
			if (building.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}

			ScoreCalcStructure structure = new ScoreCalcStructure();
			double expBonus = ScanMatrixItem.calcExpBonus(building.getLife(), building.getMaxLife(), 2.);
			if (expBonus > 0.) {
				structure.putItem(ScoreCalcStructure.createExpBonusApplyer(expBonus));
			}

			int priorityAims = 0;
			if (self.getLife() > building.getDamage() && building.getType() == BuildingType.GUARDIAN_TOWER) {
				priorityAims = Utils.getPrefferedUnitsCountInRange(building, filteredWorld, building.getAttackRange(), building.getDamage(), self.getLife());
			}

			if (priorityAims < 2) {
				structure.putItem(ScoreCalcStructure.createBuildingDangerApplyer(building.getAttackRange() + Math.min(2,
																													  -building.getRemainingActionCooldownTicks() - addTicks + 4) * 1.5,
																				 building.getDamage() * shieldBonus));
			} else if (priorityAims == 2) {
				structure.putItem(ScoreCalcStructure.createBuildingDangerApplyer((building.getAttackRange() + Math.min(2,
																													   -building.getRemainingActionCooldownTicks() - addTicks + 4) * 1.5) * .5,
																				 building.getDamage() * shieldBonus * .5));
			}

			structure.putItem(ScoreCalcStructure.createAttackBonusApplyer(self.getCastRange() + building.getRadius() + Constants.getGame().getMagicMissileRadius() - .1,
																		  myDamage * Constants.BUILDING_ATTACK_FACTOR));
			structure.putItem(ScoreCalcStructure.createMeleeAttackBonusApplyer(Constants.getGame().getStaffRange() + building.getRadius() - .1,
																			   staffDamage * Constants.BUILDING_ATTACK_FACTOR));
			unitsScoreCalc.put(building.getId(), structure);
		}
	}

	public UnitScoreCalculation makeClone() {
		return new UnitScoreCalculation(unitsScoreCalc);
	}
}
