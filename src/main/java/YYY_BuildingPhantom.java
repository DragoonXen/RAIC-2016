import model.Building;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by dragoon on 11/16/16.
 */
public class YYY_BuildingPhantom extends Building {

	private static final HashMap<Long, Long> ID_MAPPING = new HashMap<Long, Long>() {{
		put(20L, 22L);
		put(22L, 20L);

		put(19L, 21L);
		put(21L, 19L);

		put(17L, 23L);
		put(23L, 17L);

		put(24L, 18L);
		put(18L, 24L);

		put(13L, 14L);
		put(14L, 13L);

		put(15L, 16L);
		put(16L, 15L);

		put(11L, 12L);
		put(12L, 11L);
	}};

	public static final HashMap<Long, Long> VULNERABLE_LINK_MAPPING = new HashMap<Long, Long>() {{
		put(17L, 18L);
		put(23L, 24L);

		put(19L, 20L);
		put(21L, 22L);

		put(13L, 15L);
		put(14L, 16L);

		put(18L, 11L);
		put(20L, 11L);
		put(15L, 11L);

		put(24L, 12L);
		put(22L, 12L);
		put(16L, 12L);
	}};

	private static final HashSet<Long> VULNERABLE_TOWERS = new HashSet<Long>() {{
		add(17L);
		add(23L);

		add(19L);
		add(21L);

		add(13L);
		add(14L);
	}};

	private int life;
	private int remainingActionCooldownTicks;
	private boolean fixedRemainActionCooldownTicks;
	private boolean updated;
	private boolean broken;
	private YYY_Point position;
	private boolean isInvulnerable;

	public YYY_BuildingPhantom(Building building, boolean enemy) {
		super(enemy ? ID_MAPPING.get(building.getId()) : building.getId(),
			  enemy ? YYY_Constants.getGame().getMapSize() - building.getX() : building.getX(),
			  enemy ? YYY_Constants.getGame().getMapSize() - building.getY() : building.getY(),
			  building.getSpeedX(),
			  building.getSpeedY(),
			  building.getAngle(),
			  enemy ? YYY_Constants.getEnemyFaction() : building.getFaction(),
			  building.getRadius(),
			  building.getLife(),
			  building.getMaxLife(),
			  building.getStatuses(),
			  building.getType(),
			  building.getVisionRange(),
			  building.getAttackRange(),
			  building.getDamage(),
			  building.getCooldownTicks(),
			  building.getRemainingActionCooldownTicks());
		this.life = building.getLife();
		this.remainingActionCooldownTicks = building.getRemainingActionCooldownTicks();
		this.position = new YYY_Point(getX(), getY());
		this.isInvulnerable = !VULNERABLE_TOWERS.contains(getId());
	}

	public void updateInfo(Building building) {
		this.life = building.getLife();
		this.remainingActionCooldownTicks = building.getRemainingActionCooldownTicks();
		updated = true;
		fixedRemainActionCooldownTicks = false;
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

	public boolean isBroken() {
		return broken;
	}

	public void setBroken(boolean broken) {
		this.broken = broken;
	}

	public YYY_Point getPosition() {
		return position;
	}

	public void fixRemainingActionCooldownTicks() {
		if (!fixedRemainActionCooldownTicks) {
			remainingActionCooldownTicks = getCooldownTicks() - 20;
			fixedRemainActionCooldownTicks = true;
		}
	}

	public void makeVulnerable() {
		this.isInvulnerable = false;
	}

	public boolean isInvulnerable() {
		return isInvulnerable;
	}

	@Override
	public int getLife() {
		return life;
	}

	@Override
	public int getRemainingActionCooldownTicks() {
		return remainingActionCooldownTicks;
	}
}
