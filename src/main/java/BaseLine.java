import model.Unit;
import model.World;

/**
 * Created by by.dragoon on 11/8/16.
 */
public abstract class BaseLine {

    protected Point fightPoint = new Point();

    public Point getFightPoint() {
        return fightPoint;
    }

    public abstract double getDistanceTo(Unit unit);

    public abstract double getDistanceTo(double x, double y);

    public abstract double getMoveDirection(Unit unit);

    public abstract double getMoveDirection(Point point);

    public double calcLineDistanceOtherDanger(Unit unit) {
        return calcLineDistanceOtherDanger(new Point(unit.getX(), unit.getY()));
    }

    public double calcLineDistanceOtherDanger(Point point) {
        double distanceTo = getDistanceTo(point.getX(), point.getY()) -
                Constants.getTopLine().getLineDistance();
        if (distanceTo > 0.) {
            distanceTo /= Constants.getTopLine().getLineDistance();
            return distanceTo * distanceTo * distanceTo;
        }
        return 0.;
    }

    public abstract void updateFightPoint(World world, EnemyPositionCalc enemyPositionCalc);

    public abstract Point getNearestPoint(double x, double y);

    public Point getPreFightPoint() {
        double angle = Utils.normalizeAngle(getMoveDirection(fightPoint) + Math.PI);
        return getNearestPoint(fightPoint.getX() + Math.cos(angle) * Constants.PRE_POINT_DISTANCE,
                               fightPoint.getY() + Math.sin(angle) * Constants.PRE_POINT_DISTANCE);
    }
}
