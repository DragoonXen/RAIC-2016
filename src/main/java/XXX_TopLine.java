import model.Unit;
import model.World;

/**
 * Created by by.dragoon on 11/8/16.
 */
public class XXX_TopLine extends XXX_BaseLine {

	private final static double CRITICAL_MULT = Math.sqrt(1.5 * 1.5 * 2);

	private double lineDistance;

	private double sqrtOfTwo = 1. / Math.sqrt(2.); //ax + by + c = 0, a=1, b=1, c = -centerLine

	private double[] moveDirection = new double[]{-Math.PI / 2., -Math.PI / 4., 0.};

	public static XXX_Point[] detectPoint;

	private XXX_Point[] centralSegmentPoints = new XXX_Point[]{new XXX_Point(700, 175), new XXX_Point(175, 700)};

	public XXX_TopLine() {
		lineDistance = (XXX_Constants.getGame().getMapSize() * .1 - 50.) * .5;
		detectPoint = new XXX_Point[3];
		detectPoint[0] = new XXX_Point(lineDistance, lineDistance * (4. + CRITICAL_MULT));
		detectPoint[1] = new XXX_Point(2.5 * lineDistance, 2.5 * lineDistance);
		detectPoint[2] = new XXX_Point(lineDistance * (4. + CRITICAL_MULT), lineDistance);

		fightPoint.update(437.5, 437.5);
	}

	public double getLineDistance() {
		return lineDistance;
	}

	@Override
	public double getDistanceTo(Unit unit) {
		return getDistanceTo(unit.getX(), unit.getY());
	}

	@Override
	public double getDistanceTo(double x, double y) {
		switch (getCurrentPartNo(x, y)) {
			case 0:
				return Math.abs(x - lineDistance);
			case 1:
				return Math.abs(x + y - lineDistance * 5.) * sqrtOfTwo;
			case 2:
				return Math.abs(y - lineDistance);
		}
		return Double.MAX_VALUE; //unreachable code
	}

	@Override
	public XXX_Point getNearestPoint(double x, double y) {
		switch (getCurrentPartNo(x, y)) {
			case 0:
				return new XXX_Point(lineDistance, y);
			case 1:
				return XXX_Utils.nearestSegmentPoint(new XXX_Point(x, y), centralSegmentPoints[0], centralSegmentPoints[1]);
			case 2:
				return new XXX_Point(x, lineDistance);
		}
		return XXX_Point.EMPTY_POINT.clonePoint();
	}

	@Override
	public double getMoveDirection(Unit unit) {
		return moveDirection[getCurrentPartNo(unit.getX(), unit.getY())];
	}

	@Override
	public double getMoveDirection(XXX_Point point) {
		return moveDirection[getCurrentPartNo(point.getX(), point.getY())];
	}

	@Override
	public void updateFightPoint(World world, XXX_EnemyPositionCalc enemyPositionCalc) {
		double minDistance = 1e6;
		XXX_MinionPhantom closestMinionPhantom = null;
		for (XXX_MinionPhantom minionPhantom : enemyPositionCalc.getDetectedMinions().values()) {
			if (minionPhantom.getLine() != 0) {
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

	private int getCurrentPartNo(double x, double y) {
		double centerDistance = XXX_FastMath.hypot(x - detectPoint[1].getX(), y - detectPoint[1].getY());
		double tmpDistance = XXX_FastMath.hypot(x - detectPoint[0].getX(), y - detectPoint[0].getY());
		if (tmpDistance < centerDistance) {
			return 0;
		}
		tmpDistance = XXX_FastMath.hypot(x - detectPoint[2].getX(), y - detectPoint[2].getY());
		return tmpDistance < centerDistance ? 2 : 1;
	}
}
