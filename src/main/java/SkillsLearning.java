import model.Move;
import model.Player;
import model.SkillType;
import model.Wizard;
import model.World;

/**
 * Created by dragoon on 11/27/16.
 */
public class SkillsLearning {

	private static SkillType[] currentSkillsToLearn;

	private static final SkillType[] skillsToLearn = new SkillType[]{
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
			SkillType.ADVANCED_MAGIC_MISSILE};

	private static final SkillType[] thirdSkillsToLearn = new SkillType[]{
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
			SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
			SkillType.STAFF_DAMAGE_BONUS_AURA_1,
			SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
			SkillType.STAFF_DAMAGE_BONUS_AURA_2,
			SkillType.FIREBALL};

	private static final SkillType[] secondSkillsToLearn = new SkillType[]{
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
			SkillType.HASTE};

	private static final SkillType[][] arraySkillsToLearn = new SkillType[][]{skillsToLearn, secondSkillsToLearn, thirdSkillsToLearn};

	public static void init(World world) {
		Player[] players = new Player[11];
		long myId = 0;
		Player me = null;
		for (Player player : world.getPlayers()) {
			players[(int) player.getId()] = player;
			if (player.isMe()) {
				myId = player.getId();
				me = player;
			}
		}
		String name = me.getName().split(" ")[0];
		myId /= 6;
		int from = 1 + (int) myId * 5;
		int to = from + 5;
		int cntMe = 0;
		int myNom = 0;
		for (int i = from; i != to; ++i) {
			if (players[i].getName().startsWith(name)) {
				if (players[i].isMe()) {
					myNom = cntMe;
				}
				++cntMe;
			}
		}
		currentSkillsToLearn = arraySkillsToLearn[myNom % 3];
	}

	public static void updateSkills(Wizard self, Move move) {
		if (self.getLevel() > self.getSkills().length && self.getLevel() <= skillsToLearn.length) {
			move.setSkillToLearn(currentSkillsToLearn[self.getSkills().length]);
		}
	}
}
