import model.Minion;
import model.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by dragoon on 11/20/16.
 */
public class EnemyPositionCalc {

	private HashMap<Long, MinionPhantom> detectedMinions;
	private static List<Long> visibleIds = new ArrayList<>();

	private int[] minionsOnLine;

	public EnemyPositionCalc() {
		detectedMinions = new HashMap<>();
		minionsOnLine = new int[]{0, 0, 0};
	}

	public EnemyPositionCalc(HashMap<Long, MinionPhantom> detectedMinions, int[] minionsOnLine) {
		this.detectedMinions = new HashMap<>(detectedMinions);
		this.minionsOnLine = Arrays.copyOf(minionsOnLine, minionsOnLine.length);
	}

	public void updatePositions(World world) {
		visibleIds.clear();
		visibleIds.addAll(detectedMinions.keySet());


		for (MinionPhantom minionPhantom : detectedMinions.values()) {
			minionPhantom.resetUpdate();
		}

		for (Minion minion : world.getMinions()) {
			if (minion.getFaction() != Constants.getEnemyFaction()) {
				continue;
			}
			MinionPhantom minionPhantom = detectedMinions.get(minion.getId());
			if (minionPhantom == null) {
				int line = Utils.whichLine(minion);
				detectedMinions.put(minion.getId(), new MinionPhantom(minion, world.getTickIndex(), line));
				++minionsOnLine[line];
				continue;
			}
			minionPhantom.updateInfo(minion, world.getTickIndex());
		}

		for (Long visibleId : visibleIds) {
			MinionPhantom minionPhantom = detectedMinions.get(visibleId);
			if (minionPhantom.isUpdated()) {
				continue;
			}
			if (minionPhantom.getLastSeenTick() + Constants.ENEMY_MINIONS_LOST_TIME <= world.getTickIndex() ||
					Utils.isUnitVisible(minionPhantom.getPosition(),
										Constants.getGame().getMinionSpeed() * (world.getTickIndex() - minionPhantom.getLastSeenTick()),
										world.getWizards(),
										world.getMinions(),
										world.getBuildings())) {
				--minionsOnLine[minionPhantom.getLine()];
				detectedMinions.remove(visibleId);
			} else {
				minionPhantom.nextTick();
			}
		}
	}

	public EnemyPositionCalc clone() {
		return new EnemyPositionCalc(detectedMinions, minionsOnLine);
	}
}
