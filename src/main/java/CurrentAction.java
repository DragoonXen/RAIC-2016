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
		EVADE_PROJECTILE, RUN_FROM_PROJECTILE, MOVE, FIGHT
	}

	public CurrentAction clone() {
		CurrentAction instance = new CurrentAction();
		instance.actionType = actionType;
		return instance;
	}
}
