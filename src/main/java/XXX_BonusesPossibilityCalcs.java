import model.Bonus;
import model.World;

import java.util.Arrays;

/**
 * Created by dragoon on 11/21/16.
 */
public class XXX_BonusesPossibilityCalcs {

	public final static XXX_Point[] BONUSES_POINTS = new XXX_Point[]{new XXX_Point(1200, 1200), new XXX_Point(2800, 2800)};

	private double score[];
	private static boolean visible[] = new boolean[2];

	public XXX_BonusesPossibilityCalcs() {
		score = new double[]{0., 0.};
	}

	public XXX_BonusesPossibilityCalcs(double[] score) {
		this.score = Arrays.copyOf(score, score.length);
	}

	public void updateTick(World world, XXX_EnemyPositionCalc enemyPositionCalc) {
		if (world.getTickIndex() < 2500) {
			return;
		}
		visible[0] = visible[1] = false;
		if (world.getTickIndex() % 2500 == 1) {
			score[0] = score[1] = 1.;
		}
		for (Bonus bonus : world.getBonuses()) {
			if (bonus.getX() < 2000.) {
				visible[0] = true;
				score[0] = 1.;
			} else {
				visible[1] = true;
				score[1] = 1.;
			}
		}

		for (int i = 0; i != 2; ++i) {
			if (score[i] == 0.) {
				continue;
			}
			if (XXX_Utils.isPositionVisible(BONUSES_POINTS[i], .1, world.getWizards(), world.getMinions(), null) && !visible[i]) {
				score[i] = 0.;
				continue;
			}
			if (visible[i]) {
				continue;
			}
			for (XXX_WizardPhantom wizardPhantom : enemyPositionCalc.getDetectedWizards().values()) {
				double distanceToBonus = XXX_FastMath.hypot(wizardPhantom.getPosition().getX() - BONUSES_POINTS[i].getX(),
															wizardPhantom.getPosition().getY() - BONUSES_POINTS[i].getY()) -
						XXX_Constants.getGame().getBonusRadius() + wizardPhantom.getRadius();
				if (wizardPhantom.isUpdated()) {
					if (distanceToBonus < .01) {
						score[i] = 0.;
						break;
					}
				} else {
					double walkDistance = XXX_Constants.MAX_WIZARDS_FORWARD_SPEED * (world.getTickIndex() - wizardPhantom.getLastSeenTick());
					if (walkDistance >= distanceToBonus) {
						walkDistance = distanceToBonus / 12.;
						walkDistance = Math.pow(0.5, 1 / walkDistance);
						score[i] *= walkDistance;
					}
				}
			}
		}
	}

	public double[] getScore() {
		return score;
	}

	public XXX_BonusesPossibilityCalcs clone() {
		return new XXX_BonusesPossibilityCalcs(score);
	}
}
