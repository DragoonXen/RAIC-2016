import model.Move;
import model.SkillType;
import model.Wizard;

/**
 * Created by dragoon on 11/27/16.
 */
public class SkillsLearning {

	private static final SkillType[] skillsToLearn = new SkillType[]{
			SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
			SkillType.STAFF_DAMAGE_BONUS_AURA_1,
			SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
			SkillType.STAFF_DAMAGE_BONUS_AURA_2,
			SkillType.FIREBALL,
			SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
			SkillType.RANGE_BONUS_PASSIVE_1,
			SkillType.RANGE_BONUS_AURA_1,
			SkillType.RANGE_BONUS_PASSIVE_2,
			SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
			SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
			SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
			SkillType.RANGE_BONUS_AURA_2,
			SkillType.ADVANCED_MAGIC_MISSILE,
			SkillType.HASTE};

	public static void updateSkills(Wizard self, Move move) {
		if (self.getLevel() > self.getSkills().length && self.getLevel() <= skillsToLearn.length) {
			move.setSkillToLearn(skillsToLearn[self.getSkills().length]);
		}
	}
}
