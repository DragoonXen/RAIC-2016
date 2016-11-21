/**
 * Created by dragoon on 11/19/16.
 */
public class CurrentAction {

	private CurrentAction.ActionType actionType;
	private Point movePoint;

	public CurrentAction() {
		actionType = ActionType.FIGHT;
		movePoint = new Point(100, 100);
	}

	public ActionType getActionType() {
		return actionType;
	}

	public void setActionType(ActionType actionType) {
		this.actionType = actionType;
	}

	public void setMovePoint(double x, double y) {
		this.movePoint.update(x, y);
	}

	public Point getMovePoint() {
		return movePoint;
	}

	public enum ActionType {
		EVADE_PROJECTILE(false), RUN_FROM_PROJECTILE(false), MOVE_TO_POSITION(true), FIGHT(true);

		public final boolean moveCalc;

		ActionType(boolean moveCalc) {
			this.moveCalc = moveCalc;
		}
	}

	public CurrentAction clone() {
		CurrentAction instance = new CurrentAction();
		instance.actionType = actionType;
		instance.setMovePoint(movePoint.getX(), movePoint.getY());
		return instance;
	}
}
