/**
 * Created by dragoon on 11/18/16.
 */
public class YYY_SpawnPoint {

	double ptX;
	double ptY;

	private static int size;

	public YYY_SpawnPoint(double ptX, double ptY) {
		this.ptX = ptX;
		this.ptY = ptY;
	}

	public boolean isPointInDanger(double x, double y) {
		return Math.abs(x - ptX) <= size && Math.abs(y - ptY) <= size;
	}

	public static boolean checkSpawnPoints() {
		return size > 0;
	}

	public static void updateTick(int tick) {
		int ticksToSpawn = YYY_Constants.getGame().getFactionMinionAppearanceIntervalTicks() -
				(tick - 1) % YYY_Constants.getGame().getFactionMinionAppearanceIntervalTicks();
		if (ticksToSpawn > YYY_Constants.TICKS_TO_LEAVE_SPAWN) {
			size = 0;
			return;
		}
		size = YYY_Constants.SPAWN_POINT_SIZE;
	}
}
