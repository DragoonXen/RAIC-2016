import model.Unit;
import model.World;

/**
 * Created by dragoon on 11/20/16.
 */
public class YYY_PositionMoveLine extends YYY_BaseLine {

	private YYY_Point positionToMove;
	private double minDistanceTo;

	public final static YYY_PositionMoveLine INSTANCE = new YYY_PositionMoveLine();

	private YYY_PositionMoveLine() {
		this.minDistanceTo = 0.;
		this.positionToMove = new YYY_Point();
	}

	public void updatePointToMove(YYY_Point newPoint) {
		updatePointToMove(newPoint.getX(), newPoint.getY());
	}

	public void updatePointToMove(double x, double y) {
		this.positionToMove.update(x, y);
	}

	public YYY_Point getPositionToMove() {
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
		return YYY_Utils.normalizeAngle(unit.getAngleTo(positionToMove.getX(), positionToMove.getY()) + unit.getAngle());
	}

	@Override
	public double getMoveDirection(YYY_Point point) {
		return 0; // nothing to do
	}

	@Override
	public double calcLineDistanceOtherDanger(Unit unit) {
		return 0.;
	}

	@Override
	public double calcLineDistanceOtherDanger(YYY_Point point) {
		return 0.;
	}

	@Override
	public void updateFightPoint(World world, YYY_EnemyPositionCalc enemyPositionCalc) {
		// nothing to do
		this.fightPoint.update(positionToMove);
	}

	@Override
	public YYY_Point getNearestPoint(double x, double y) {
		return positionToMove;
	}
}
