import model.Unit;
import model.World;

/**
 * Created by dragoon on 11/20/16.
 */
public class XXX_PositionMoveLine extends XXX_BaseLine {

	private XXX_Point positionToMove;
	private double minDistanceTo;

	public final static XXX_PositionMoveLine INSTANCE = new XXX_PositionMoveLine();

	private XXX_PositionMoveLine() {
		this.minDistanceTo = 0.;
		this.positionToMove = new XXX_Point();
	}

	public void updatePointToMove(XXX_Point newPoint) {
		updatePointToMove(newPoint.getX(), newPoint.getY());
	}

	public void updatePointToMove(double x, double y) {
		this.positionToMove.update(x, y);
	}

	public XXX_Point getPositionToMove() {
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
		return XXX_Utils.normalizeAngle(unit.getAngleTo(positionToMove.getX(), positionToMove.getY()) + unit.getAngle());
	}

	@Override
	public double getMoveDirection(XXX_Point point) {
		return 0; // nothing to do
	}

	@Override
	public double calcLineDistanceOtherDanger(Unit unit) {
		return 0.;
	}

	@Override
	public double calcLineDistanceOtherDanger(XXX_Point point) {
		return 0.;
	}

	@Override
	public void updateFightPoint(World world, XXX_EnemyPositionCalc enemyPositionCalc) {
		// nothing to do
		this.fightPoint.update(positionToMove);
	}

	@Override
	public XXX_Point getNearestPoint(double x, double y) {
		return positionToMove;
	}
}
