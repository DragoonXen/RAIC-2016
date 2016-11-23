import model.Unit;
import model.World;

/**
 * Created by dragoon on 11/20/16.
 */
public class PositionMoveLine extends BaseLine {

	private Point positionToMove;
	private double minDistanceTo;

	public final static PositionMoveLine INSTANCE = new PositionMoveLine();

	private PositionMoveLine() {
		this.minDistanceTo = 0.;
		this.positionToMove = new Point();
	}

	public void updatePointToMove(Point newPoint) {
		updatePointToMove(newPoint.getX(), newPoint.getY());
	}

	public void updatePointToMove(double x, double y) {
		this.positionToMove.update(x, y);
	}

	public Point getPositionToMove() {
		return positionToMove;
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
	public double getMoveDirection(Point point) {
		return 0; // nothing to do
	}

	@Override
	public double calcLineDistanceOtherDanger(Unit unit) {
		return 0.;
	}

	@Override
	public double calcLineDistanceOtherDanger(Point point) {
		return 0.;
	}

	@Override
	public void updateFightPoint(World world, EnemyPositionCalc enemyPositionCalc) {
		// nothing to do
	}

	@Override
	public Point getNearestPoint(double x, double y) {
		return positionToMove;
	}
}
