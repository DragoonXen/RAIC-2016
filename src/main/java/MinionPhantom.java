import model.Minion;

/**
 * Created by dragoon on 11/20/16.
 */
public class MinionPhantom extends Minion {

	private int remainingActionCooldownTicks;
	private int lastSeenTick;
	private int life;
	private boolean updated;
	private int line;
	private Point position;

	public MinionPhantom(Minion minion, int tick, int line) {
		super(minion.getId(),
			  minion.getX(),
			  minion.getY(),
			  minion.getSpeedX(),
			  minion.getSpeedY(),
			  minion.getAngle(),
			  minion.getFaction(),
			  minion.getRadius(),
			  minion.getLife(),
			  minion.getMaxLife(),
			  minion.getStatuses(),
			  minion.getType(),
			  minion.getVisionRange(),
			  minion.getDamage(),
			  minion.getCooldownTicks(),
			  minion.getRemainingActionCooldownTicks());
		this.position = new Point(minion.getX(), minion.getY());
		this.remainingActionCooldownTicks = minion.getRemainingActionCooldownTicks();
		this.lastSeenTick = tick;
		this.updated = true;
		this.life = minion.getLife();
		this.line = line;
	}

	@Override
	public int getRemainingActionCooldownTicks() {
		return remainingActionCooldownTicks;
	}

	@Override
	public int getLife() {
		return life;
	}

	public void updateInfo(Minion minion, int tick) {
		this.lastSeenTick = tick;
		this.life = minion.getLife();
		this.remainingActionCooldownTicks = minion.getRemainingActionCooldownTicks();
		this.updated = true;
		this.position.update(minion.getX(), minion.getY());
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

	public int getLine() {
		return line;
	}

	public Point getPosition() {
		return position;
	}

	public MinionPhantom clone() {
		MinionPhantom mp = new MinionPhantom(this, lastSeenTick, line);
		mp.position = new Point(position.getX(), position.getY());
		return mp;
	}
}
