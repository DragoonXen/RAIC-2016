import model.Faction;
import model.Game;
import model.LaneType;
import model.Wizard;

/**
 * Created by by.dragoon on 11/8/16.
 */
public abstract class Constants {

    public final static LaneType[] WHICH_LINE_NO = new LaneType[]{LaneType.TOP, LaneType.MIDDLE, LaneType.BOTTOM};
    public static double MOVE_FWD_DISTANCE = 351.;
    public static double MOVE_SIDE_DISTANCE = 150.;
    public static double MOVE_BACK_DISTANCE = 150.;
    public static double MOVE_SCAN_FIGURE_CENTER = (MOVE_FWD_DISTANCE - MOVE_BACK_DISTANCE) / 2.;
    public static double MOVE_SCAN_STEP = 3.;
    public static int CURRENT_PT_X = (int) Math.round(MOVE_BACK_DISTANCE / MOVE_SCAN_STEP + .1);
    public static int CURRENT_PT_Y = (int) Math.round(MOVE_SIDE_DISTANCE / MOVE_SCAN_STEP + .1);

    public static double DANGER_PENALTY = 200.;
    public static double DANGER_AT_START_MULT_RUN = .9;

    public static int[] STEP_X_HELP = new int[]{0, 0, -1, 1};
    public static int[] STEP_Y_HELP = new int[]{-1, 1, 0, 0};

    public static double WIZARD_AIM_PROIRITY = 5.;
    public static double BUILDING_AIM_PROIRITY = 6.;
    public static double FETISH_AIM_PROIRITY = 1.;
    public static double ORC_AIM_PROIRITY = 1.;
    public static double LOW_AIM_SCORE = .5;
    public static double NEUTRAL_FACTION_AIM_PROIRITY = 0.2;

    public static double ENEMY_WIZARD_ATTACK_LIFE = 0.75;

    public static double MAX_SHOOT_ANGLE = Math.PI / 12.;

    public static double EXPERIENCE_DISTANCE = 600.;

    public static double MOVE_DISTANCE_FILTER = Math.sqrt((MOVE_BACK_DISTANCE + MOVE_FWD_DISTANCE) * (MOVE_BACK_DISTANCE + MOVE_FWD_DISTANCE) / 4. + MOVE_SIDE_DISTANCE * MOVE_SIDE_DISTANCE);

    private static Game game;
    private static Faction currentFaction;
    private static Faction enemyFaction;
    private static TopLine topLine;
    private static MiddleLine middleLine;
    private static BottomLine bottomLine;
    private static BaseLine[] lines;
    private static double fightDistanceFilter;

    public static void init(Game game, Wizard self) {
        Constants.game = game;
        topLine = new TopLine();
        middleLine = new MiddleLine();
        bottomLine = new BottomLine();
        lines = new BaseLine[]{topLine, middleLine, bottomLine};
        currentFaction = self.getFaction();
        enemyFaction = self.getFaction() == Faction.ACADEMY ? Faction.RENEGADES : Faction.ACADEMY;
        fightDistanceFilter = MOVE_DISTANCE_FILTER + game.getWizardCastRange() + game.getRangeBonusPerSkillLevel() * 4;
    }

    public static Game getGame() {
        return Constants.game;
    }

    public static Faction getCurrentFaction() {
        return currentFaction;
    }

    public static Faction getEnemyFaction() {
        return enemyFaction;
    }

    public static BaseLine[] getLines() {
        return lines;
    }

    public static TopLine getTopLine() {
        return topLine;
    }

    public static MiddleLine getMiddleLine() {
        return middleLine;
    }

    public static BaseLine getLine(LaneType laneType) {
        switch (laneType) {
            case TOP:
                return topLine;
            case MIDDLE:
                return middleLine;
            default:
                return bottomLine;
        }
    }

    public static BottomLine getBottomLine() {
        return bottomLine;
    }

    public static double getFightDistanceFilter() {
        return fightDistanceFilter;
    }

    public final static int minionLineScore = 5;
    public final static int wizardLineScore = 20;
    public final static int towerLineScore = 15;

}
