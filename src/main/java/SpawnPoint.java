/**
 * Created by dragoon on 11/18/16.
 */
public class SpawnPoint {

	double ptX;
	double ptY;

	private static int size;

	public SpawnPoint(double ptX, double ptY) {
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
		int ticksToSpawn = Constants.getGame().getFactionMinionAppearanceIntervalTicks() -
				(tick - 1) % Constants.getGame().getFactionMinionAppearanceIntervalTicks();
		if (ticksToSpawn > Constants.TICKS_TO_LEAVE_SPAWN) {
			size = 0;
			return;
		}
		size = Constants.SPAWN_POINT_SIZE;
	}
}
