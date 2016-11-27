/**
 * Created by dragoon on 11/18/16.
 */
public class XXX_SpawnPoint {

	double ptX;
	double ptY;

	private static int size;

	public XXX_SpawnPoint(double ptX, double ptY) {
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
		int ticksToSpawn = XXX_Constants.getGame().getFactionMinionAppearanceIntervalTicks() -
				(tick - 1) % XXX_Constants.getGame().getFactionMinionAppearanceIntervalTicks();
		if (ticksToSpawn > XXX_Constants.TICKS_TO_LEAVE_SPAWN) {
			size = 0;
			return;
		}
		size = XXX_Constants.SPAWN_POINT_SIZE;
	}
}
