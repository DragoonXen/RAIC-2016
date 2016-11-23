import model.Wizard;

/**
 * Created by dragoon on 11/20/16.
 */
public class WizardPhantom extends Wizard {

	private int remainingActionCooldownTicks;
	private int lastSeenTick;
	private int life;
	private int maxLife;
	private boolean updated;
	private Point position;

	public WizardPhantom(Wizard wizard, int tick) {
		super(wizard.getId(),
			  wizard.getX(),
			  wizard.getY(),
			  wizard.getSpeedX(),
			  wizard.getSpeedY(),
			  wizard.getAngle(),
			  wizard.getFaction(),
			  wizard.getRadius(),
			  wizard.getLife(),
			  wizard.getMaxLife(),
			  wizard.getStatuses(),
			  wizard.getOwnerPlayerId(),
			  wizard.isMe(),
			  wizard.getMana(),
			  wizard.getMaxMana(),
			  wizard.getVisionRange(),
			  wizard.getCastRange(),
			  wizard.getXp(),
			  wizard.getLevel(),
			  wizard.getSkills(),
			  wizard.getRemainingActionCooldownTicks(),
			  wizard.getRemainingCooldownTicksByAction(),
			  wizard.isMaster(),
			  wizard.getMessages());
		this.position = new Point(getX(), getY());
		this.remainingActionCooldownTicks = wizard.getRemainingActionCooldownTicks();
		this.lastSeenTick = tick;
		this.updated = true;
		this.life = wizard.getLife();
		this.maxLife = wizard.getMaxLife();
	}

	public WizardPhantom(Wizard wizard, int tick, boolean enemy) {
		super(wizard.getId() + (enemy ? (wizard.getId() > 5 ? -5 : 5) : 0),
			  enemy ? Constants.getGame().getMapSize() - wizard.getX() : wizard.getX(),
			  enemy ? Constants.getGame().getMapSize() - wizard.getY() : wizard.getY(),
			  wizard.getSpeedX(),
			  wizard.getSpeedY(),
			  wizard.getAngle(),
			  wizard.getFaction(),
			  wizard.getRadius(),
			  wizard.getLife(),
			  wizard.getMaxLife(),
			  wizard.getStatuses(),
			  wizard.getOwnerPlayerId(),
			  wizard.isMe(),
			  wizard.getMana(),
			  wizard.getMaxMana(),
			  wizard.getVisionRange(),
			  wizard.getCastRange(),
			  wizard.getXp(),
			  wizard.getLevel(),
			  wizard.getSkills(),
			  wizard.getRemainingActionCooldownTicks(),
			  wizard.getRemainingCooldownTicksByAction(),
			  wizard.isMaster(),
			  wizard.getMessages());
		this.position = new Point(getX(), getY());
		this.remainingActionCooldownTicks = wizard.getRemainingActionCooldownTicks();
		this.lastSeenTick = tick;
		this.updated = true;
		this.life = wizard.getLife();
		this.maxLife = wizard.getMaxLife();
	}

	@Override
	public int getRemainingActionCooldownTicks() {
		return remainingActionCooldownTicks;
	}

	@Override
	public int getLife() {
		return life;
	}

	@Override
	public int getMaxLife() {
		return maxLife;
	}

	public void updateInfo(Wizard wizard, int tick) {
		this.lastSeenTick = tick;
		this.life = wizard.getLife();
		this.maxLife = wizard.getMaxLife();
		this.remainingActionCooldownTicks = wizard.getRemainingActionCooldownTicks();
		this.updated = true;
		this.position.update(wizard.getX(), wizard.getY());
	}

	public int getLastSeenTick() {
		return lastSeenTick;
	}

	public void resetUpdate() {
		updated = false;
	}

	public void nextTick() {
		this.remainingActionCooldownTicks = Math.max(0, this.remainingActionCooldownTicks - 1);
	}

	public boolean isUpdated() {
		return updated;
	}

	public Point getPosition() {
		return position;
	}

	public void reborn(int tick) {
		this.position.update(getX(), getY());
		this.lastSeenTick = tick - 10;
	}

	public WizardPhantom clone() {
		WizardPhantom wp = new WizardPhantom(this, lastSeenTick);
		wp.position = new Point(position.getX(), position.getY());
		return wp;
	}
}
