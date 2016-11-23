import model.Unit;
import model.World;

/**
 * Created by by.dragoon on 11/8/16.
 */
public class TopLine extends BaseLine {

	private final static double CRITICAL_MULT = Math.sqrt(1.5 * 1.5 * 2);

	private double lineDistance;

	private double sqrtOfTwo = 1. / Math.sqrt(2.); //ax + by + c = 0, a=1, b=1, c = -centerLine

	private double[] moveDirection = new double[]{-Math.PI / 2., -Math.PI / 4., 0.};

	public static Point[] detectPoint;

	private Point[] centralSegmentPoints = new Point[]{new Point(700, 175), new Point(175, 700)};

	public TopLine() {
		lineDistance = (Constants.getGame().getMapSize() * .1 - 50.) * .5;
		detectPoint = new Point[3];
		detectPoint[0] = new Point(lineDistance, lineDistance * (4. + CRITICAL_MULT));
		detectPoint[1] = new Point(2.5 * lineDistance, 2.5 * lineDistance);
		detectPoint[2] = new Point(lineDistance * (4. + CRITICAL_MULT), lineDistance);

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
	public Point getNearestPoint(double x, double y) {
		switch (getCurrentPartNo(x, y)) {
			case 0:
				return new Point(lineDistance, y);
			case 1:
				return Utils.nearestSegmentPoint(new Point(x, y), centralSegmentPoints[0], centralSegmentPoints[1]);
			case 2:
				return new Point(x, lineDistance);
		}
		return Point.EMPTY_POINT.clonePoint();
	}

	@Override
	public double getMoveDirection(Unit unit) {
		return moveDirection[getCurrentPartNo(unit.getX(), unit.getY())];
	}

	@Override
	public double getMoveDirection(Point point) {
		return moveDirection[getCurrentPartNo(point.getX(), point.getY())];
	}

	@Override
	public void updateFightPoint(World world, EnemyPositionCalc enemyPositionCalc) {
		double minDistance = 1e6;
		MinionPhantom closestMinionPhantom = null;
		for (MinionPhantom minionPhantom : enemyPositionCalc.getDetectedMinions().values()) {
			if (minionPhantom.getLine() != 0) {
				continue;
			}
			double tmp = FastMath.hypot(minionPhantom.getPosition().getX(), 4000 - minionPhantom.getPosition().getY());
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
		double centerDistance = FastMath.hypot(x - detectPoint[1].getX(), y - detectPoint[1].getY());
		double tmpDistance = FastMath.hypot(x - detectPoint[0].getX(), y - detectPoint[0].getY());
		if (tmpDistance < centerDistance) {
			return 0;
		}
		tmpDistance = FastMath.hypot(x - detectPoint[2].getX(), y - detectPoint[2].getY());
		return tmpDistance < centerDistance ? 2 : 1;
	}
}
