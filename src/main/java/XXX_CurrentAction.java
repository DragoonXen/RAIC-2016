/**
 * Created by dragoon on 11/19/16.
 */
public class XXX_CurrentAction {

	private XXX_CurrentAction.ActionType actionType;

	public XXX_CurrentAction() {
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

	public XXX_CurrentAction clone() {
		XXX_CurrentAction instance = new XXX_CurrentAction();
		instance.actionType = actionType;
		return instance;
	}

	@Override
	public String toString() {
		return "XXX_CurrentAction{" +
				"actionType=" + actionType +
				'}';
	}
}
