import model.Unit;
import model.World;

/**
 * Created by by.dragoon on 11/8/16.
 */
public abstract class XXX_BaseLine {

	protected XXX_Point fightPoint = new XXX_Point();

	public XXX_Point getFightPoint() {
		return fightPoint;
	}

	public abstract double getDistanceTo(Unit unit);

	public abstract double getDistanceTo(double x, double y);

	public abstract double getMoveDirection(Unit unit);

	public abstract double getMoveDirection(XXX_Point point);

	public double calcLineDistanceOtherDanger(Unit unit) {
		return calcLineDistanceOtherDanger(new XXX_Point(unit.getX(), unit.getY()));
	}

	public double calcLineDistanceOtherDanger(XXX_Point point) {
		double distanceTo = getDistanceTo(point.getX(), point.getY()) -
				XXX_Constants.getTopLine().getLineDistance();
		if (distanceTo > 0.) {
			distanceTo /= XXX_Constants.getTopLine().getLineDistance();
			return distanceTo * distanceTo * distanceTo;
		}
		return 0.;
	}

	public abstract void updateFightPoint(World world, XXX_EnemyPositionCalc enemyPositionCalc);

	public abstract XXX_Point getNearestPoint(double x, double y);

	public XXX_Point getPreFightPoint() {
		double angle = XXX_Utils.normalizeAngle(getMoveDirection(fightPoint) + Math.PI);
		return getNearestPoint(fightPoint.getX() + Math.cos(angle) * XXX_Constants.PRE_POINT_DISTANCE,
							   fightPoint.getY() + Math.sin(angle) * XXX_Constants.PRE_POINT_DISTANCE);
	}
}
