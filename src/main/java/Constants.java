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

    public static double STUCK_FIX_RADIUS_ADD = .001;

    public static double MOVE_SCAN_STEP = 3.;
    public static double MOVE_SCAN_DIAGONAL_DISTANCE = FastMath.hypot(MOVE_SCAN_STEP, MOVE_SCAN_STEP);
    public static int CURRENT_PT_X = (int) Math.round(MOVE_BACK_DISTANCE / MOVE_SCAN_STEP + .1);
    public static int CURRENT_PT_Y = (int) Math.round(MOVE_SIDE_DISTANCE / MOVE_SCAN_STEP + .1);

    public static int MAP_SIDE_DANGER_DISTANCE = 35 + 50; // wizard radius = 35
    public static double MAP_SIDE_DANGER_FACTOR = .05;

    public static double MINION_ATTACK_FACTOR = .25;

    public static double DANGER_PENALTY = 200.;
    public static double DANGER_AT_START_MULT_RUN = .9;

    public static double MOVE_ANGLE_PRECISE = Math.PI / 180. * .2; // 0.2 per calc
    public static double RUN_ANGLE_EXPAND = Math.PI / 180. * 45.; // 45 degrees

	public static int EVADE_CALCULATIONS_COUNT = 90;
	public static double EVADE_DEGREE_STEP = Math.PI * 2. / EVADE_CALCULATIONS_COUNT; // 360 / 90 = 4

    public static int SPAWN_POINT_SIZE = 250;
    public static double SPAWN_POINT_DANGER = 80.;
    public static int TICKS_TO_LEAVE_SPAWN = 80;
    public static SpawnPoint[] SPAWN_POINTS = new SpawnPoint[]{new SpawnPoint(3200, 800), new SpawnPoint(3000, 200), new SpawnPoint(3800, 1000)};

    public static int[] STEP_X_HELP = new int[]{0, 0, -1, 1};
    public static int[] STEP_Y_HELP = new int[]{-1, 1, 0, 0};

    public static double WIZARD_AIM_PROIRITY = 5.;
    public static double BUILDING_AIM_PROIRITY = 6.;
    public static double FETISH_AIM_PROIRITY = 1.;
    public static double ORC_AIM_PROIRITY = 1.;
    public static double LOW_AIM_SCORE = .5;
    public static double NEUTRAL_FACTION_AIM_PROIRITY = 0.2;

    public static double FORWARD_MOVE_FROM_DISTANCE_POWER = 1 / 8.;

    public static double SHIELDENED_AIM_PRIORITY = 0.1;
    public static double EMPOWERED_AIM_PRIORITY = 1.2;
    public static double HASTENED_AIM_PRIORITY = 0.7;

    public static double ENEMY_WIZARD_ATTACK_LIFE = 0.75;

    public static double MAX_SHOOT_ANGLE = Math.PI / 12.;

    public static double EXPERIENCE_DISTANCE = 600.;

    public static double MAX_WIZARDS_FORWARD_SPEED = 6.;
    public static double BONUS_POSSIBILITY_RUN = .7;
    public static double TICKS_BUFFER_RUN_TO_BONUS = 1.2;
    public static double NEAREST_TO_BONUS_CALCULATION_OTHER_MULT = .8;

    public static double PRE_POINT_DISTANCE = 500.;

    public static double MOVE_DISTANCE_FILTER = Math.sqrt((MOVE_BACK_DISTANCE + MOVE_FWD_DISTANCE) * (MOVE_BACK_DISTANCE + MOVE_FWD_DISTANCE) / 4. + MOVE_SIDE_DISTANCE * MOVE_SIDE_DISTANCE);

    public static int TREES_COUNT_TO_CUT = 6;
    public static double TREES_DISTANCE_TO_CUT = 200.;

    public static double CUT_SELF_DISTANCE_PRIORITY = 10.;
    public static double CUT_REACH_POINT_DISTANCE_PTIORITY = 100.;

    public static int ENEMY_MINIONS_LOST_TIME = 750;

    private static Game game;
    private static Faction currentFaction;
    private static Faction enemyFaction;
    private static TopLine topLine;
    private static MiddleLine middleLine;
    private static BottomLine bottomLine;
    private static BaseLine[] lines;
    private static double fightDistanceFilter;
    private static double staffHitSector;

    public static void init(Game game, Wizard self) {
        Constants.game = game;
        topLine = new TopLine();
        middleLine = new MiddleLine();
        bottomLine = new BottomLine();
        lines = new BaseLine[]{topLine, middleLine, bottomLine};
        currentFaction = self.getFaction();
        enemyFaction = self.getFaction() == Faction.ACADEMY ? Faction.RENEGADES : Faction.ACADEMY;
        fightDistanceFilter = MOVE_DISTANCE_FILTER + game.getWizardCastRange() + game.getRangeBonusPerSkillLevel() * 4;
        staffHitSector = game.getStaffSector() / 2.;
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

    public static double getStaffHitSector() {
        return staffHitSector;
    }

    public final static int minionLineScore = 5;
    public final static int enemyWizardLineScore = 25;
    public final static double wizardLineMult = .5;
    public final static int towerLineScore = 20;

    public final static double CURRENT_LINE_PRIORITY = 1.2;

}
