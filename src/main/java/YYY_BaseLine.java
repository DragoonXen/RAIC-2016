import model.Unit;
import model.World;

/**
 * Created by by.dragoon on 11/8/16.
 */
public abstract class YYY_BaseLine {

	protected YYY_Point fightPoint = new YYY_Point();

	public YYY_Point getFightPoint() {
		return fightPoint;
	}

	public abstract double getDistanceTo(Unit unit);

	public abstract double getDistanceTo(double x, double y);

	public abstract double getMoveDirection(Unit unit);

	public abstract double getMoveDirection(YYY_Point point);

	public double calcLineDistanceOtherDanger(Unit unit) {
		return calcLineDistanceOtherDanger(new YYY_Point(unit.getX(), unit.getY()));
	}

	public double calcLineDistanceOtherDanger(YYY_Point point) {
		double distanceTo = getDistanceTo(point.getX(), point.getY()) -
				YYY_Constants.getTopLine().getLineDistance();
		if (distanceTo > 0.) {
			distanceTo /= YYY_Constants.getTopLine().getLineDistance();
			return distanceTo * distanceTo * distanceTo;
		}
		return 0.;
	}

	public abstract void updateFightPoint(World world, YYY_EnemyPositionCalc enemyPositionCalc);

	public abstract YYY_Point getNearestPoint(double x, double y);

	public YYY_Point getPreFightPoint() {
		double angle = YYY_Utils.normalizeAngle(getMoveDirection(fightPoint) + Math.PI);
		return getNearestPoint(fightPoint.getX() + Math.cos(angle) * YYY_Constants.PRE_POINT_DISTANCE,
							   fightPoint.getY() + Math.sin(angle) * YYY_Constants.PRE_POINT_DISTANCE);
	}
}
