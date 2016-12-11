/**
 * Created by dragoon on 11/19/16.
 */
public class YYY_CurrentAction {

	private YYY_CurrentAction.ActionType actionType;

	public YYY_CurrentAction() {
		actionType = ActionType.FIGHT;
	}

	public ActionType getActionType() {
		return actionType;
	}

	public void setActionType(ActionType actionType) {
		this.actionType = actionType;
	}

	public enum ActionType {
		EVADE_PROJECTILE(false), RUN_FROM_PROJECTILE(false), MOVE_TO_POSITION(true), FIGHT(true), PURSUIT(false);

		public final boolean moveCalc;

		ActionType(boolean moveCalc) {
			this.moveCalc = moveCalc;
		}
	}

	public YYY_CurrentAction clone() {
		YYY_CurrentAction instance = new YYY_CurrentAction();
		instance.actionType = actionType;
		return instance;
	}

	@Override
	public String toString() {
		return "YYY_CurrentAction{" +
				"actionType=" + actionType +
				'}';
	}
}
