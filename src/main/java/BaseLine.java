import model.Unit;

/**
 * Created by by.dragoon on 11/8/16.
 */
public interface BaseLine {

    double getDistanceTo(Unit unit);

    double getDistanceTo(double x, double y);

    double getMoveDirection(Unit unit);
}
