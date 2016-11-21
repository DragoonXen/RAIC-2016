import model.Bonus;
import model.World;

/**
 * Created by dragoon on 11/21/16.
 */
public class BonusesPossibilityCalcs {

	public final static Point[] BONUSES_POINTS = new Point[]{new Point(1200, 1200), new Point(2800, 2800)};

	private double score[] = new double[]{0., 0.};
	private boolean visible[] = new boolean[2];

	public void updateTick(World world, EnemyPositionCalc enemyPositionCalc) {
		if (world.getTickIndex() < 2500) {
			return;
		}
		visible[0] = visible[1] = false;
		if (world.getTickIndex() % 2500 == 1) {
			score[0] = score[1] = 1.;
		} else {
			for (Bonus bonus : world.getBonuses()) {
				if (bonus.getX() < 2000.) {
					visible[0] = true;
					score[0] = 1.;
				} else {
					visible[1] = true;
					score[1] = 1.;
				}
			}
		}

		for (int i = 0; i != 2; ++i) {
			if (score[i] == 0.) {
				continue;
			}
			if (Utils.isUnitVisible(BONUSES_POINTS[i], .1, world.getWizards(), world.getMinions(), null) && !visible[i]) {
				score[i] = 0.;
				continue;
			}


		}
	}
}
