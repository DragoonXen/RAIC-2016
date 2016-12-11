import model.Unit;
import model.World;

/**
 * Created by by.dragoon on 11/8/16.
 */
public class YYY_TopLine extends YYY_BaseLine {

	private final static double CRITICAL_MULT = Math.sqrt(1.5 * 1.5 * 2);

	private double lineDistance;

	private double sqrtOfTwo = 1. / Math.sqrt(2.); //ax + by + c = 0, a=1, b=1, c = -centerLine

	private double[] moveDirection = new double[]{-Math.PI / 2., -Math.PI / 4., 0.};

	public static YYY_Point[] detectPoint;

	private YYY_Point[] centralSegmentPoints = new YYY_Point[]{new YYY_Point(700, 175), new YYY_Point(175, 700)};

	public YYY_TopLine() {
		lineDistance = (YYY_Constants.getGame().getMapSize() * .1 - 50.) * .5;
		detectPoint = new YYY_Point[3];
		detectPoint[0] = new YYY_Point(lineDistance, lineDistance * (4. + CRITICAL_MULT));
		detectPoint[1] = new YYY_Point(2.5 * lineDistance, 2.5 * lineDistance);
		detectPoint[2] = new YYY_Point(lineDistance * (4. + CRITICAL_MULT), lineDistance);

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
	public YYY_Point getNearestPoint(double x, double y) {
		switch (getCurrentPartNo(x, y)) {
			case 0:
				return new YYY_Point(lineDistance, y);
			case 1:
				return YYY_Utils.nearestSegmentPoint(new YYY_Point(x, y), centralSegmentPoints[0], centralSegmentPoints[1]);
			case 2:
				return new YYY_Point(x, lineDistance);
		}
		return YYY_Point.EMPTY_POINT.clonePoint();
	}

	@Override
	public double getMoveDirection(Unit unit) {
		return moveDirection[getCurrentPartNo(unit.getX(), unit.getY())];
	}

	@Override
	public double getMoveDirection(YYY_Point point) {
		return moveDirection[getCurrentPartNo(point.getX(), point.getY())];
	}

	@Override
	public void updateFightPoint(World world, YYY_EnemyPositionCalc enemyPositionCalc) {
		double minDistance = 1e6;
		YYY_Point nearestPoint = null;
		for (YYY_MinionPhantom minionPhantom : enemyPositionCalc.getDetectedMinions().values()) {
			if (minionPhantom.getLine() != 0) {
				continue;
			}
			double tmp = YYY_FastMath.hypot(minionPhantom.getPosition().getX(), 4000 - minionPhantom.getPosition().getY());
			if (tmp < minDistance) {
				minDistance = tmp;
				nearestPoint = minionPhantom.getPosition();
			}
		}

		for (YYY_WizardPhantom wizardPhantom : enemyPositionCalc.getDetectedWizards().values()) {
			if (wizardPhantom.getLastSeenTick() == 0 || YYY_Variables.wizardsInfo.getWizardInfo(wizardPhantom.getId()).getLineNo() != 0) {
				continue;
			}
			double tmp = YYY_FastMath.hypot(wizardPhantom.getPosition().getX(), 4000 - wizardPhantom.getPosition().getY());
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
		double centerDistance = YYY_FastMath.hypot(x - detectPoint[1].getX(), y - detectPoint[1].getY());
		double tmpDistance = YYY_FastMath.hypot(x - detectPoint[0].getX(), y - detectPoint[0].getY());
		if (tmpDistance < centerDistance) {
			return 0;
		}
		tmpDistance = YYY_FastMath.hypot(x - detectPoint[2].getX(), y - detectPoint[2].getY());
		return tmpDistance < centerDistance ? 2 : 1;
	}
}
