import model.Unit;

/**
 * Created by by.dragoon on 11/8/16.
 */
public class MiddleLine extends BaseLine {

	private double centerLine;

	private double moveDirection = -Math.PI / 4.; //always up right

	private double sqrtOfTwo = 1. / Math.sqrt(2.); //ax + by + c = 0, a=1, b=1, c = -centerLine

	public MiddleLine() {
		centerLine = Constants.getGame().getMapSize();
	}

	public double getCenterLine() {
		return centerLine;
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
}
