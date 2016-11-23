/**
 * Created by dragoon on 11/19/16.
 */
public class CurrentAction {

	private CurrentAction.ActionType actionType;

	public CurrentAction() {
		actionType = ActionType.FIGHT;
	}

	public ActionType getActionType() {
		return actionType;
	}

	public void setActionType(ActionType actionType) {
		this.actionType = actionType;
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
		return instance;
	}

	@Override
	public String toString() {
		return "CurrentAction{" +
				"actionType=" + actionType +
				'}';
	}
}
