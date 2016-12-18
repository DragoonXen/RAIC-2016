import model.Player;
import model.Wizard;
import model.World;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by dragoon on 12/15/16.
 */
public class SchemeSelector {

	public static int schemeNo = -1;

	private static BaseLine fightLine;

	public static boolean goodEvasion = false;
	public static boolean antmsu = false;
	public static boolean sideAgressive = false;
	public static boolean core2duo = false;
	public static boolean mortido = false;

	// default scheme
	public static void init(World world) {
		int myNom = (int) world.getMyPlayer().getId();
		if (myNom > 5) {
			myNom -= 5;
		}
		--myNom;
		SkillsLearning.currentSkillsToLearn = SkillsLearning.arraySkillsToLearn[myNom];
		fightLine = Constants.getLines()[1];

		String playerName = null;
		for (Player player : world.getPlayers()) {
			if (!"Commandos".equals(player.getName())) {
				playerName = player.getName();
				break;
			}
		}
		goodEvasion = "NighTurs".equals(playerName) ||
				"Antmsu".equals(playerName) ||
				"ud1".equals(playerName);

		if ("tyamgin".equals(playerName)) {
			Date date = new Date();
			Calendar instance = Calendar.getInstance();
			instance.set(Calendar.HOUR, 4);
			instance.set(Calendar.MINUTE, 39);
			Date checkDate = instance.getTime();
			if (date.after(checkDate)) {
				instance.set(Calendar.MINUTE, 40);
				checkDate = instance.getTime();
				if (date.before(checkDate)) {
					playerName = "TonyK";
				}
			}
		}
		if ("TonyK".equals(playerName)) {
			antmsu = true;
		} else if ("core2duo".equals(playerName)) {
			core2duo = true;
		} else {
			Calendar calendar = Calendar.getInstance();
			Date dt = new Date();
			calendar.set(Calendar.MINUTE, 45);
			calendar.set(Calendar.HOUR, 11);
			calendar.set(Calendar.DATE, 18);
			if (calendar.getTime().before(dt)) {
				if ("ud1".equals(playerName) || "mortido".equals(playerName) || "Antmsu".equals(playerName) || "NighTurs".equals(playerName)) {
					sideAgressive = true;
					mortido = true;
					Constants.SIDE_AGRESSIVE_POINT = BonusesPossibilityCalcs.BONUSES_POINTS[1].addWithCopy(new Point(300., 300.));
					antmsu = true;
				}
			}
		}
		if ("Antmsu".equals(playerName) && myNom == 0) {
			SkillsLearning.currentSkillsToLearn = SkillsLearning.MOVEMENT_FROST_RANGE;
		}

//		if ("NighTurs".equals(playerName)) {
//			schemeNo = 0;
//		}
//		schemeNo = 1;

		switch (schemeNo) {
			// antmsu, 2 2 1
			case 0:
				switch (myNom) {
					case 0:
						SkillsLearning.currentSkillsToLearn = SkillsLearning.RANGE_FIRE_MOVEMENT;
						fightLine = Constants.getLines()[0];
						return;
					case 1:
						SkillsLearning.currentSkillsToLearn = SkillsLearning.FIRE_RANGE_MOVEMENT;
						fightLine = Constants.getLines()[0];
						return;
					case 2:
						SkillsLearning.currentSkillsToLearn = SkillsLearning.FROST_MOVEMENT_RANGE;
						fightLine = Constants.getLines()[1];
						return;
					case 3:
						SkillsLearning.currentSkillsToLearn = SkillsLearning.MOVEMENT_FROST_RANGE;
						fightLine = Constants.getLines()[1];
						return;
					case 4:
						SkillsLearning.currentSkillsToLearn = SkillsLearning.MOVEMENT_FROST_RANGE;
						fightLine = Constants.getLines()[2];
						return;
				}
				break;
			// anti rush
			case 1:
				switch (myNom) {
					case 0:
						SkillsLearning.currentSkillsToLearn = SkillsLearning.FIRE_RANGE_MOVEMENT;
						fightLine = Constants.getLines()[0];
						return;
					case 1:
						SkillsLearning.currentSkillsToLearn = SkillsLearning.FIRE_RANGE_MOVEMENT;
						fightLine = Constants.getLines()[1];
						return;
					case 2:
						SkillsLearning.currentSkillsToLearn = SkillsLearning.RANGE_FIRE_MOVEMENT;
						fightLine = Constants.getLines()[1];
						return;
					case 3:
						SkillsLearning.currentSkillsToLearn = SkillsLearning.MOVEMENT_FROST_RANGE;
						fightLine = Constants.getLines()[1];
						return;
					case 4:
						SkillsLearning.currentSkillsToLearn = SkillsLearning.SHIELD_FIRE_RANGE;
						fightLine = Constants.getLines()[1];
						return;
				}
				break;
		}
	}

	public static BaseLine fightLineSelect(BaseLine previousLine, World world, EnemyPositionCalc enemyPositionCalc, Wizard self) {
		return fightLine;
	}
}
