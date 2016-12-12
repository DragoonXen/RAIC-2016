import model.Move;
import model.SkillType;
import model.Wizard;
import model.World;

/**
 * Created by dragoon on 11/27/16.
 */
public class SkillsLearning {

	private static SkillType[] currentSkillsToLearn;

	private static final SkillType[] FIRE_RANGE_MOVEMENT = new SkillType[]{
			SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
			SkillType.STAFF_DAMAGE_BONUS_AURA_1,
			SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
			SkillType.STAFF_DAMAGE_BONUS_AURA_2,
			SkillType.FIREBALL,
			SkillType.RANGE_BONUS_PASSIVE_1,
			SkillType.RANGE_BONUS_AURA_1,
			SkillType.RANGE_BONUS_PASSIVE_2,
			SkillType.RANGE_BONUS_AURA_2,
			SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
			SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
			SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
			SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
			SkillType.HASTE,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
			SkillType.SHIELD};

	private static final SkillType[] FROST_MOVEMENT_RANGE = new SkillType[]{
			SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
			SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
			SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
			SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
			SkillType.FROST_BOLT,
			SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
			SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
			SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
			SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
			SkillType.HASTE,
			SkillType.RANGE_BONUS_PASSIVE_1,
			SkillType.RANGE_BONUS_AURA_1,
			SkillType.RANGE_BONUS_PASSIVE_2,
			SkillType.RANGE_BONUS_AURA_2,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
			SkillType.SHIELD};

	private static final SkillType[] RANGE_FIRE_MOVEMENT = new SkillType[]{
			SkillType.RANGE_BONUS_PASSIVE_1,
			SkillType.RANGE_BONUS_AURA_1,
			SkillType.RANGE_BONUS_PASSIVE_2,
			SkillType.RANGE_BONUS_AURA_2,
			SkillType.ADVANCED_MAGIC_MISSILE,
			SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
			SkillType.STAFF_DAMAGE_BONUS_AURA_1,
			SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
			SkillType.STAFF_DAMAGE_BONUS_AURA_2,
			SkillType.FIREBALL,
			SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
			SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
			SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
			SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
			SkillType.HASTE,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
			SkillType.SHIELD};

	private static final SkillType[] MOVEMENT_FROST_RANGE = new SkillType[]{
			SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
			SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
			SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
			SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
			SkillType.HASTE,
			SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
			SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
			SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
			SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
			SkillType.FROST_BOLT,
			SkillType.RANGE_BONUS_PASSIVE_1,
			SkillType.RANGE_BONUS_AURA_1,
			SkillType.RANGE_BONUS_PASSIVE_2,
			SkillType.RANGE_BONUS_AURA_2,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
			SkillType.SHIELD};

	private static final SkillType[] SHIELD_FIRE_RANGE = new SkillType[]{
			SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
			SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
			SkillType.SHIELD,
			SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
			SkillType.STAFF_DAMAGE_BONUS_AURA_1,
			SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
			SkillType.STAFF_DAMAGE_BONUS_AURA_2,
			SkillType.FIREBALL,
			SkillType.RANGE_BONUS_PASSIVE_1,
			SkillType.RANGE_BONUS_AURA_1,
			SkillType.RANGE_BONUS_PASSIVE_2,
			SkillType.RANGE_BONUS_AURA_2,
			SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
			SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
			SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
			SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
			SkillType.HASTE};

	private static final SkillType[][] arraySkillsToLearn = new SkillType[][]{
			FIRE_RANGE_MOVEMENT,
			RANGE_FIRE_MOVEMENT,
			FROST_MOVEMENT_RANGE,
			MOVEMENT_FROST_RANGE,
			SHIELD_FIRE_RANGE};

	public static void init(World world) {
		int myNom = (int) world.getMyPlayer().getId();
		if (myNom > 5) {
			myNom -= 5;
		}
		--myNom;
		currentSkillsToLearn = arraySkillsToLearn[myNom];
	}

	public static void updateSkills(Wizard self, EnemyPositionCalc enemyPositionCalc, Wizard[] wizards, Move move) {
		if (self.getLevel() > self.getSkills().length && self.getLevel() <= currentSkillsToLearn.length) {
			move.setSkillToLearn(currentSkillsToLearn[self.getSkills().length]);
			if (self.getSkills().length == 0) {
//				WizardsInfo wizardsInfo = Variables.wizardsInfo;
//				currentSkillsToLearn = FIRE_RANGE_MOVEMENT;
//				int myLine = wizardsInfo.getMe().getLineNo();
//				List<Wizard> allyWizards = new ArrayList<>();
//				for (Wizard wizard : wizards) {
//					if (wizard.getFaction() != Constants.getCurrentFaction()) {
//						continue;
//					}
//					if (!wizard.isMe() && wizardsInfo.getWizardInfo(wizard.getId()).getLineNo() == myLine) {
//						allyWizards.add(wizard);
//					}
//				}
//				if (allyWizards.isEmpty()) {
//					currentSkillsToLearn = MOVEMENT_FROST_RANGE;
//				} else if (allyWizards.size() == 1) {
//					if (self.getId() > allyWizards.get(0).getId()) {
//						currentSkillsToLearn = RANGE_FIRE_MOVEMENT;
//					} else {
//						currentSkillsToLearn = FIRE_RANGE_MOVEMENT;
//					}
//				} else {
//					boolean max = true;
//					for (Wizard allyWizard : allyWizards) {
//						if (allyWizard.getId() > self.getId()) {
//							max = false;
//							break;
//						}
//					}
//					if (max) {
//						currentSkillsToLearn = RANGE_FIRE_MOVEMENT;
//					} else {
//						currentSkillsToLearn = FIRE_RANGE_MOVEMENT;
//					}
//				}
			}
		}
	}
}
