import model.Unit;

/**
 * Created by by.dragoon on 11/8/16.
 */
public abstract class BaseLine {

    public abstract double getDistanceTo(Unit unit);

    public abstract double getDistanceTo(double x, double y);

    public abstract double getMoveDirection(Unit unit);

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
}
