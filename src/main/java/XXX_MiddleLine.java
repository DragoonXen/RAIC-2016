import model.Unit;
import model.World;

/**
 * Created by by.dragoon on 11/8/16.
 */
public class XXX_MiddleLine extends XXX_BaseLine {

	private double centerLine;

	private double moveDirection = -Math.PI / 4.; //always up right

	private double sqrtOfTwo = 1. / Math.sqrt(2.); //ax + by + c = 0, a=1, b=1, c = -centerLine

	private XXX_Point[] segmentPoints = new XXX_Point[]{new XXX_Point(0, 4000), new XXX_Point(4000, 0)};

	public XXX_MiddleLine() {
		centerLine = XXX_Constants.getGame().getMapSize();
		fightPoint.update(2000, 2000);
	}

	public double getCenterLine() {
		return centerLine;
	}

	@Override
	public XXX_Point getNearestPoint(double x, double y) {
		return XXX_Utils.nearestSegmentPoint(new XXX_Point(x, y), segmentPoints[0], segmentPoints[1]);
	}

	@Override
	public double getDistanceTo(Unit unit) {
		return getDistanceTo(unit.getX(), unit.getY());
	}

	@Override
	public double getDistanceTo(double x, double y) {
		return Math.abs(x + y - centerLine) * sqrtOfTwo;
	}

	public double getMoveDirection(Unit unit) {
		return moveDirection;
	}

	@Override
	public double getMoveDirection(XXX_Point point) {
		return moveDirection;
	}

	@Override
	public void updateFightPoint(World world, XXX_EnemyPositionCalc enemyPositionCalc) {
		double minDistance = 1e6;
		XXX_MinionPhantom closestMinionPhantom = null;
		for (XXX_MinionPhantom minionPhantom : enemyPositionCalc.getDetectedMinions().values()) {
			if (minionPhantom.getLine() != 1) {
				continue;
			}
			double tmp = XXX_FastMath.hypot(minionPhantom.getPosition().getX(), 4000 - minionPhantom.getPosition().getY());
			if (tmp < minDistance) {
				minDistance = tmp;
				closestMinionPhantom = minionPhantom;
			}
		}
		if (closestMinionPhantom != null) {
			fightPoint.update(getNearestPoint(closestMinionPhantom.getPosition().getX(), closestMinionPhantom.getPosition().getY()));
		}
	}
}
