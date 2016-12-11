import model.ActionType;
import model.SkillType;
import model.Status;
import model.Wizard;
import model.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Created by dragoon on 11/29/16.
 */
public class YYY_WizardsInfo {

	private WizardInfo[] wizardInfos;

	private WizardInfo me;

	public YYY_WizardsInfo() {
		wizardInfos = new WizardInfo[11];
		for (int i = 1; i != 11; ++i) {
			wizardInfos[i] = new WizardInfo();
		}
	}

	private YYY_WizardsInfo(WizardInfo[] wizardInfos, WizardInfo me) {
		this.wizardInfos = new WizardInfo[11];
		for (int i = 1; i != 11; ++i) {
			this.wizardInfos[i] = wizardInfos[i].makeClone();
			if (wizardInfos[i] == me) {
				this.me = this.wizardInfos[i];
			}
		}
	}

	public void updateData(World world, YYY_EnemyPositionCalc enemyPositionCalc) {
		YYY_Variables.wizardsInfo = this;
		if (me == null) {
			for (Wizard wizard : world.getWizards()) {
				if (wizard.isMe()) {
					me = wizardInfos[(int) wizard.getId()];
					break;
				}
			}
		}
		List<Wizard> allyWizards = new ArrayList<>();
		List<Wizard> enemyWizards = new ArrayList<>();
		for (Wizard wizard : world.getWizards()) {
			if (wizard.getFaction() == YYY_Constants.getCurrentFaction()) {
				allyWizards.add(wizard);
			} else {
				enemyWizards.add(wizard);
			}
			wizardInfos[(int) wizard.getId()].updateSkills(wizard.getSkills());
		}
		// TODO: добавить ауру от невидимых, если castRange отличается от расчётного.
		updateWizardsStatuses(allyWizards);
		updateWizardsStatuses(enemyWizards);

		YYY_Variables.maxTurnAngle = YYY_Constants.getGame().getWizardMaxTurnAngle() * me.getTurnFactor();
	}

	private void updateWizardsStatuses(List<Wizard> wizardsTeam) {
		for (int i = 0; i != wizardsTeam.size(); ++i) {
			Wizard first = wizardsTeam.get(i);
			WizardInfo currWizardInfo = wizardInfos[(int) first.getId()];
			for (int j = i + 1; j < wizardsTeam.size(); ++j) {
				Wizard second = wizardsTeam.get(j);
				if (YYY_Constants.getGame().getAuraSkillRange() > YYY_FastMath.hypot(first, second)) {
					currWizardInfo.updateAuras(wizardInfos[(int) second.getId()]);
				}
			}
			currWizardInfo.finalCalculation(first);
		}
	}

	public WizardInfo getMe() {
		return me;
	}

	public WizardInfo getWizardInfo(long wizardId) {
		return wizardInfos[(int) wizardId];
	}

	public YYY_WizardsInfo makeClone() {
		return new YYY_WizardsInfo(wizardInfos, me);
	}

	public static class WizardInfo {
		private int lineNo;
		private int hastened;
		private int shielded;
		private int frozen;
		private int empowered;
		private double moveFactor;
		private double turnFactor;
		private double castRange;

		private double manaRegenerationCounter;
		private int lastSeenMana;
		private int lastSeenTick;
		private double manaRegeneration;

		private int absorbMagicBonus;
		private int staffDamage;
		private int staffDamageBonus;
		private int magicalMissileDamage;
		private int magicDamageBonus;
		private int frostBoltDamage;
		private int fireballMaxDamage;
		private int fireballMinDamage;

		private boolean hasFrostBolt;
		private boolean hasFireball;
		private boolean hasHasteSkill;
		private boolean hasShieldSkill;
		private boolean hasFastMissileCooldown;

		private HashSet<SkillType> knownSkills;

		private int[] actionCooldown;
		private int[] skillsCount;
		private int[] aurasCount;
		private int[] otherAurasCount;

		public WizardInfo() {
			knownSkills = new HashSet<>();

			skillsCount = new int[5];
			aurasCount = new int[5];
			otherAurasCount = new int[5];
			actionCooldown = new int[7];
			castRange = 500.;
			lineNo = 1;
		}

		public WizardInfo(int hastened,
						  int shielded,
						  int frozen,
						  int empowered,
						  double moveFactor,
						  double turnFactor,
						  int staffDamage,
						  int staffDamageBonus,
						  int magicalMissileDamage,
						  int magicDamageBonus,
						  int frostBoltDamage,
						  int fireballMaxDamage,
						  int fireballMinDamage,
						  boolean hasFrostBolt,
						  boolean hasFireball,
						  boolean hasHasteSkill,
						  boolean hasShieldSkill,
						  boolean hasFastMissileCooldown,
						  HashSet<SkillType> knownSkills,
						  int[] skillsCount,
						  int[] aurasCount,
						  int[] otherAurasCount,
						  double castRange,
						  int[] actionCooldown,
						  int lineNo,
						  double manaRegenerationCounter,
						  int lastSeenMana,
						  int lastSeenTick) {
			this.hastened = hastened;
			this.shielded = shielded;
			this.frozen = frozen;
			this.empowered = empowered;
			this.moveFactor = moveFactor;
			this.turnFactor = turnFactor;
			this.staffDamage = staffDamage;
			this.staffDamageBonus = staffDamageBonus;
			this.magicalMissileDamage = magicalMissileDamage;
			this.magicDamageBonus = magicDamageBonus;
			this.frostBoltDamage = frostBoltDamage;
			this.fireballMaxDamage = fireballMaxDamage;
			this.fireballMinDamage = fireballMinDamage;
			this.hasFrostBolt = hasFrostBolt;
			this.hasFireball = hasFireball;
			this.hasHasteSkill = hasHasteSkill;
			this.hasShieldSkill = hasShieldSkill;
			this.hasFastMissileCooldown = hasFastMissileCooldown;
			this.knownSkills = new HashSet<>(knownSkills);
			this.skillsCount = Arrays.copyOf(skillsCount, skillsCount.length);
			this.aurasCount = Arrays.copyOf(aurasCount, aurasCount.length);
			this.otherAurasCount = Arrays.copyOf(otherAurasCount, otherAurasCount.length);
			this.castRange = castRange;
			this.actionCooldown = Arrays.copyOf(actionCooldown, actionCooldown.length);
			this.lineNo = lineNo;
			this.manaRegenerationCounter = manaRegenerationCounter;
			this.lastSeenMana = lastSeenMana;
			this.lastSeenTick = lastSeenTick;
		}

		public void finalCalculation(Wizard wizard) {
			empowered = 0;
			frozen = 0;
			hastened = 0;
			shielded = 0;
			for (Status status : wizard.getStatuses()) {
				switch (status.getType()) {
					case BURNING:
						break;
					case EMPOWERED:
						empowered = Math.max(empowered, status.getRemainingDurationTicks());
						break;
					case FROZEN:
						frozen = Math.max(frozen, status.getRemainingDurationTicks());
						break;
					case HASTENED:
						hastened = Math.max(hastened, status.getRemainingDurationTicks());
						break;
					case SHIELDED:
						shielded = Math.max(shielded, status.getRemainingDurationTicks());
						break;
				}
			}
			turnFactor = 1. + (hastened > 0 ? YYY_Constants.getGame().getHastenedRotationBonusFactor() : 0.);
			staffDamageBonus = (skillsCount[YYY_SkillFork.STAFF_DAMAGE.ordinal()] +
					Math.max(aurasCount[YYY_SkillFork.STAFF_DAMAGE.ordinal()], otherAurasCount[YYY_SkillFork.STAFF_DAMAGE.ordinal()])) *
					YYY_Constants.getGame().getStaffDamageBonusPerSkillLevel();
			staffDamage = YYY_Constants.getGame().getStaffDamage() + staffDamageBonus;

			magicDamageBonus = (skillsCount[YYY_SkillFork.MAGICAL_DAMAGE.ordinal()] +
					Math.max(aurasCount[YYY_SkillFork.MAGICAL_DAMAGE.ordinal()], otherAurasCount[YYY_SkillFork.MAGICAL_DAMAGE.ordinal()])) *
					YYY_Constants.getGame().getMagicalDamageBonusPerSkillLevel();

			absorbMagicBonus = (skillsCount[YYY_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] +
					Math.max(aurasCount[YYY_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()],
							 otherAurasCount[YYY_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()])) *
					YYY_Constants.getGame().getMagicalDamageAbsorptionPerSkillLevel();

			magicalMissileDamage = YYY_Constants.getGame().getMagicMissileDirectDamage() + magicDamageBonus;
			if (hasFireball) {
				fireballMaxDamage = YYY_Constants.getGame().getFireballExplosionMaxDamage() + magicDamageBonus;
				fireballMinDamage = YYY_Constants.getGame().getFireballExplosionMinDamage() + magicDamageBonus;
			}

			if (hasFrostBolt) {
				frostBoltDamage = YYY_Constants.getGame().getFrostBoltDirectDamage() + magicDamageBonus;
			}

			if (empowered > 0) {
				staffDamage *= YYY_Constants.getGame().getEmpoweredDamageFactor();
				magicalMissileDamage *= YYY_Constants.getGame().getEmpoweredDamageFactor();
				frostBoltDamage *= YYY_Constants.getGame().getEmpoweredDamageFactor();
				fireballMaxDamage *= YYY_Constants.getGame().getEmpoweredDamageFactor();
				fireballMinDamage *= YYY_Constants.getGame().getEmpoweredDamageFactor();
			}

			moveFactor = 1. + (hastened > 0 ? YYY_Constants.getGame().getHastenedMovementBonusFactor() : 0.) +
					(skillsCount[YYY_SkillFork.MOVEMENT.ordinal()] +
							Math.max(aurasCount[YYY_SkillFork.MOVEMENT.ordinal()], otherAurasCount[YYY_SkillFork.MOVEMENT.ordinal()])) *
							YYY_Constants.getGame().getMovementBonusFactorPerSkillLevel();

			castRange = wizard.getCastRange();

			manaRegeneration = YYY_Constants.getGame().getWizardBaseManaRegeneration()
					+ YYY_Constants.getGame().getWizardManaRegenerationGrowthPerLevel() * wizard.getLevel();
			if (YYY_Variables.world.getTickCount() == this.lastSeenTick + 1) {
				this.manaRegenerationCounter += manaRegeneration;
				if (this.manaRegenerationCounter >= 1.) {
					if ((this.lastSeenMana % 2) == (wizard.getMana() % 2)) {
						this.manaRegenerationCounter = 1. - YYY_Constants.getGame().getWizardManaRegenerationGrowthPerLevel();
					} else {
						this.manaRegenerationCounter -= 1.;
					}
				}
			} else {
				this.manaRegenerationCounter = 1. - YYY_Constants.getGame().getWizardManaRegenerationGrowthPerLevel();
			}
			this.lastSeenTick = YYY_Variables.world.getTickCount();
			this.lastSeenMana = wizard.getMana();

			this.actionCooldown[1] = Math.max(wizard.getRemainingActionCooldownTicks(),
											  wizard.getRemainingCooldownTicksByAction()[ActionType.STAFF.ordinal()]);
			this.actionCooldown[2] = Math.max(wizard.getRemainingActionCooldownTicks(),
											  wizard.getRemainingCooldownTicksByAction()[ActionType.MAGIC_MISSILE.ordinal()]);
			updateActionCooldownWithManacost(2, YYY_Constants.getGame().getMagicMissileManacost());
			this.actionCooldown[3] = Math.max(wizard.getRemainingActionCooldownTicks(),
											  wizard.getRemainingCooldownTicksByAction()[ActionType.FROST_BOLT.ordinal()]);
			updateActionCooldownWithManacost(3, YYY_Constants.getGame().getFrostBoltManacost());
			this.actionCooldown[4] = Math.max(wizard.getRemainingActionCooldownTicks(),
											  wizard.getRemainingCooldownTicksByAction()[ActionType.FIREBALL.ordinal()]);
			updateActionCooldownWithManacost(4, YYY_Constants.getGame().getFireballManacost());
			this.actionCooldown[5] = Math.max(wizard.getRemainingActionCooldownTicks(),
											  wizard.getRemainingCooldownTicksByAction()[ActionType.HASTE.ordinal()]);
			updateActionCooldownWithManacost(5, YYY_Constants.getGame().getHasteManacost());
			this.actionCooldown[6] = Math.max(wizard.getRemainingActionCooldownTicks(),
											  wizard.getRemainingCooldownTicksByAction()[ActionType.SHIELD.ordinal()]);
			updateActionCooldownWithManacost(6, YYY_Constants.getGame().getShieldManacost());

			this.lineNo = YYY_Utils.whichLine(wizard);
		}

		public int getTicksToManaRestore(ActionType actionType) {
			switch (actionType) {
				case MAGIC_MISSILE:
					return getTicksToManaRestore(YYY_Constants.getGame().getMagicMissileManacost());
				case FROST_BOLT:
					return getTicksToManaRestore(YYY_Constants.getGame().getFrostBoltManacost());
				case FIREBALL:
					return getTicksToManaRestore(YYY_Constants.getGame().getFireballManacost());
				case HASTE:
					return getTicksToManaRestore(YYY_Constants.getGame().getHasteManacost());
				case SHIELD:
					return getTicksToManaRestore(YYY_Constants.getGame().getShieldManacost());
			}
			return 0;
		}

		public int getTicksToManaRestore(int amount) {
			if (this.lastSeenMana >= amount) {
				return 0;
			}
			int val = amount - this.lastSeenMana;
			return (int) Math.floor((val - this.manaRegenerationCounter) / this.manaRegeneration + .99);
		}

		private void updateActionCooldownWithManacost(int nom, int manaCost) {
			this.actionCooldown[nom] = Math.max(actionCooldown[nom], getTicksToManaRestore(manaCost));
		}

		public void updateAuras(WizardInfo other) {
			for (int i = 0; i != aurasCount.length; ++i) {
				otherAurasCount[i] = Math.max(otherAurasCount[i], other.aurasCount[i]);
				other.otherAurasCount[i] = Math.max(other.otherAurasCount[i], aurasCount[i]);
			}
		}

		public void updateSkills(SkillType[] skills) {
			Arrays.fill(otherAurasCount, 0);
			if (knownSkills.size() == skills.length) {
				return;
			}
			for (SkillType skill : skills) {
				if (!knownSkills.contains(skill)) {
					knownSkills.add(skill);
					switch (skill) {
						case RANGE_BONUS_PASSIVE_1:
							skillsCount[YYY_SkillFork.RANGE.ordinal()] = 1;
							break;
						case RANGE_BONUS_AURA_1:
							aurasCount[YYY_SkillFork.RANGE.ordinal()] = 1;
							break;
						case RANGE_BONUS_PASSIVE_2:
							skillsCount[YYY_SkillFork.RANGE.ordinal()] = 2;
							break;
						case RANGE_BONUS_AURA_2:
							aurasCount[YYY_SkillFork.RANGE.ordinal()] = 2;
							break;
						case ADVANCED_MAGIC_MISSILE:
							hasFastMissileCooldown = true;
							break;

						case MAGICAL_DAMAGE_BONUS_PASSIVE_1:
							skillsCount[YYY_SkillFork.MAGICAL_DAMAGE.ordinal()] = 1;
							break;
						case MAGICAL_DAMAGE_BONUS_AURA_1:
							aurasCount[YYY_SkillFork.MAGICAL_DAMAGE.ordinal()] = 1;
							break;
						case MAGICAL_DAMAGE_BONUS_PASSIVE_2:
							skillsCount[YYY_SkillFork.MAGICAL_DAMAGE.ordinal()] = 2;
							break;
						case MAGICAL_DAMAGE_BONUS_AURA_2:
							aurasCount[YYY_SkillFork.MAGICAL_DAMAGE.ordinal()] = 2;
							break;
						case FROST_BOLT:
							hasFrostBolt = true;
							break;

						case STAFF_DAMAGE_BONUS_PASSIVE_1:
							skillsCount[YYY_SkillFork.STAFF_DAMAGE.ordinal()] = 1;
							break;
						case STAFF_DAMAGE_BONUS_AURA_1:
							aurasCount[YYY_SkillFork.STAFF_DAMAGE.ordinal()] = 1;
							break;
						case STAFF_DAMAGE_BONUS_PASSIVE_2:
							skillsCount[YYY_SkillFork.STAFF_DAMAGE.ordinal()] = 2;
							break;
						case STAFF_DAMAGE_BONUS_AURA_2:
							aurasCount[YYY_SkillFork.STAFF_DAMAGE.ordinal()] = 2;
							break;
						case FIREBALL:
							hasFireball = true;
							break;

						case MOVEMENT_BONUS_FACTOR_PASSIVE_1:
							skillsCount[YYY_SkillFork.MOVEMENT.ordinal()] = 1;
							break;
						case MOVEMENT_BONUS_FACTOR_AURA_1:
							aurasCount[YYY_SkillFork.MOVEMENT.ordinal()] = 1;
							break;
						case MOVEMENT_BONUS_FACTOR_PASSIVE_2:
							skillsCount[YYY_SkillFork.MOVEMENT.ordinal()] = 2;
							break;
						case MOVEMENT_BONUS_FACTOR_AURA_2:
							aurasCount[YYY_SkillFork.MOVEMENT.ordinal()] = 2;
							break;
						case HASTE:
							hasHasteSkill = true;
							break;

						case MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1:
							skillsCount[YYY_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = 1;
							break;
						case MAGICAL_DAMAGE_ABSORPTION_AURA_1:
							aurasCount[YYY_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = 1;
							break;
						case MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2:
							skillsCount[YYY_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = 2;
							break;
						case MAGICAL_DAMAGE_ABSORPTION_AURA_2:
							aurasCount[YYY_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = 2;
							break;
						case SHIELD:
							hasShieldSkill = true;
							break;
					}
				}
			}
		}

		public int getAbsorbMagicBonus() {
			return absorbMagicBonus;
		}

		public boolean isHasFrostBolt() {
			return hasFrostBolt;
		}

		public boolean isHasFireball() {
			return hasFireball;
		}

		public boolean isHasHasteSkill() {
			return hasHasteSkill;
		}

		public boolean isHasShieldSkill() {
			return hasShieldSkill;
		}

		public boolean isHasFastMissileCooldown() {
			return hasFastMissileCooldown;
		}

		public int getHastened() {
			return hastened;
		}

		public int getShielded() {
			return shielded;
		}

		public int getFrozen() {
			return frozen;
		}

		public int getEmpowered() {
			return empowered;
		}

		public double getMoveFactor() {
			return moveFactor;
		}

		public double getTurnFactor() {
			return turnFactor;
		}

		public int getStaffDamage() {
			return staffDamage;
		}

		public int getMagicalMissileDamage() {
			return magicalMissileDamage;
		}

		public int getActionCooldown(ActionType actionType) {
			return this.actionCooldown[actionType.ordinal()];
		}

		public int getFrostBoltDamage() {
			return frostBoltDamage;
		}

		public int getFireballMaxDamage() {
			return fireballMaxDamage;
		}

		public int getFireballMinDamage() {
			return fireballMinDamage;
		}

		public double getCastRange() {
			return castRange;
		}

		public int getLineNo() {
			return lineNo;
		}

		public int getStaffDamage(int addTicks) {
			if (empowered != 0 && addTicks >= empowered) {
				return staffDamageBonus + YYY_Constants.getGame().getStaffDamage();
			}
			return staffDamage;
		}

		public int getMagicalMissileDamage(int addTicks) {
			if (empowered != 0 && addTicks >= empowered) {
				return magicDamageBonus + YYY_Constants.getGame().getMagicMissileDirectDamage();
			}
			return magicalMissileDamage;
		}

		public int getFrostBoltDamage(int addTicks) {
			if (empowered != 0 && addTicks >= empowered) {
				return magicDamageBonus + YYY_Constants.getGame().getFrostBoltDirectDamage();
			}
			return frostBoltDamage;
		}

		public int getFireballMaxDamage(int addTicks) {
			if (empowered != 0 && addTicks >= empowered) {
				return magicDamageBonus + YYY_Constants.getGame().getFireballExplosionMaxDamage();
			}
			return fireballMaxDamage;
		}

		public int getFireballMinDamage(int addTicks) {
			if (empowered != 0 && addTicks >= empowered) {
				return magicDamageBonus + YYY_Constants.getGame().getFireballExplosionMinDamage();
			}
			return fireballMinDamage;
		}

		public WizardInfo makeClone() {
			return new WizardInfo(
					hastened,
					shielded,
					frozen,
					empowered,
					moveFactor,
					turnFactor,
					staffDamage,
					staffDamageBonus,
					magicalMissileDamage,
					magicDamageBonus,
					frostBoltDamage,
					fireballMaxDamage,
					fireballMinDamage,
					hasFrostBolt,
					hasFireball,
					hasHasteSkill,
					hasShieldSkill,
					hasFastMissileCooldown,
					knownSkills,
					skillsCount,
					aurasCount,
					otherAurasCount,
					castRange,
					actionCooldown,
					lineNo,
					manaRegenerationCounter,
					lastSeenMana,
					lastSeenTick);
		}
	}
}
