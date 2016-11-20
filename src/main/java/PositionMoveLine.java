import model.Unit;

/**
 * Created by dragoon on 11/20/16.
 */
public class PositionMoveLine extends BaseLine {

	private Point positionToMove;
	private double minDistanceTo;

	public PositionMoveLine() {
		this.minDistanceTo = 0.;
	}

	public void updatePointToMove(Point newPoint) {
		this.positionToMove = newPoint;
	}

	public void updatePointToMove(double x, double y) {
		this.positionToMove.update(x, y);
	}

	@Override
	public double getDistanceTo(Unit unit) {
		return 0;
	}

	@Override
	public double getDistanceTo(double x, double y) {
		return 0;
	}

	@Override
	public double getMoveDirection(Unit unit) {
		return Utils.normalizeAngle(unit.getAngleTo(positionToMove.getX(), positionToMove.getY()) + unit.getAngle());
	}

	@Override
	public double calcLineDistanceOtherDanger(Unit unit) {
		return 0.;
	}

	@Override
	public double calcLineDistanceOtherDanger(Point point) {
		return 0.;
	}
}
