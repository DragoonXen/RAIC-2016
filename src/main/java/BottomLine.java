import model.Unit;
import model.World;

/**
 * Created by by.dragoon on 11/8/16.
 */
public class BottomLine extends BaseLine {

	private final static double CRITICAL_MULT = Math.sqrt(1.5 * 1.5 * 2);

	private double lineDistance;

	private double cornerCompare;

	private double sqrtOfTwo = 1. / Math.sqrt(2.); //ax + by + c = 0, a=1, b=1, c = -centerLine

	private double[] moveDirection = new double[]{0., -Math.PI / 4., -Math.PI / 2.};

	private Point[] centralSegmentPoints = new Point[]{new Point(3300, 3825), new Point(3825, 3300)};

	public static Point[] detectPoint;

	public BottomLine() {
		cornerCompare = (Constants.getGame().getMapSize() * .1 - 50.) * .5;
		lineDistance = Constants.getGame().getMapSize() - cornerCompare;
		detectPoint = new Point[3];
		detectPoint[0] = new Point(Constants.getGame().getMapSize() - cornerCompare * (4. + CRITICAL_MULT), Constants.getGame().getMapSize() - cornerCompare);
		detectPoint[1] = new Point(Constants.getGame().getMapSize() - 2.5 * cornerCompare, Constants.getGame().getMapSize() - 2.5 * cornerCompare);
		detectPoint[2] = new Point(Constants.getGame().getMapSize() - cornerCompare, Constants.getGame().getMapSize() - cornerCompare * (4. + CRITICAL_MULT));

		cornerCompare = Constants.getGame().getMapSize() * 2. - cornerCompare * 5.;

		fightPoint.update(3562.5, 3562.5);
	}

	public double getLineDistance() {
		return lineDistance;
	}

	public double getCornerCompare() {
		return cornerCompare;
	}

	@Override
	public double getDistanceTo(Unit unit) {
		return getDistanceTo(unit.getX(), unit.getY());
	}

	@Override
	public double getDistanceTo(double x, double y) {
		switch (getCurrentPartNo(x, y)) {
			case 0:
				return Math.abs(y - lineDistance);
			case 1:
				return Math.abs(x + y - cornerCompare) * sqrtOfTwo;
			case 2:
				return Math.abs(x - lineDistance);
		}
		return Double.MAX_VALUE; //unreachable code
	}

	@Override
	public Point getNearestPoint(double x, double y) {
		switch (getCurrentPartNo(x, y)) {
			case 0:
				return new Point(x, lineDistance);
			case 1:
				return Utils.nearestSegmentPoint(new Point(x, y), centralSegmentPoints[0], centralSegmentPoints[1]);
			case 2:
				return new Point(lineDistance, y);
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
		Point nearestPoint = null;
		for (MinionPhantom minionPhantom : enemyPositionCalc.getDetectedMinions().values()) {
			if (minionPhantom.getLine() != 2) {
				continue;
			}
			double tmp = FastMath.hypot(minionPhantom.getPosition().getX(), 4000 - minionPhantom.getPosition().getY());
			if (tmp < minDistance) {
				minDistance = tmp;
				nearestPoint = minionPhantom.getPosition();
			}
		}
		for (WizardPhantom wizardPhantom : enemyPositionCalc.getDetectedWizards().values()) {
			if (wizardPhantom.getLastSeenTick() == 0 || Variables.wizardsInfo.getWizardInfo(wizardPhantom.getId()).getLineNo() != 2) {
				continue;
			}
			double tmp = FastMath.hypot(wizardPhantom.getPosition().getX(), 4000 - wizardPhantom.getPosition().getY());
			if (tmp < minDistance) {
				minDistance = tmp;
				nearestPoint = wizardPhantom.getPosition();
			}
		}
		if (nearestPoint != null) {
			fightPoint.update(getNearestPoint(nearestPoint.getX(), nearestPoint.getY()));
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
