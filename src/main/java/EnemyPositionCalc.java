import model.Minion;
import model.Wizard;
import model.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dragoon on 11/20/16.
 */
public class EnemyPositionCalc {

	private HashMap<Long, MinionPhantom> detectedMinions;
	private HashMap<Long, WizardPhantom> detectedWizards;
	private static List<Long> visibleIds = new ArrayList<>();

	private int[] minionsOnLine;

	public EnemyPositionCalc() {
		detectedMinions = new HashMap<>();
		detectedWizards = new HashMap<>();
		minionsOnLine = new int[]{0, 0, 0};
	}

	public EnemyPositionCalc(HashMap<Long, MinionPhantom> detectedMinions, HashMap<Long, WizardPhantom> detectedWizards, int[] minionsOnLine) {
		this.detectedMinions = new HashMap<>();
		for (Map.Entry<Long, MinionPhantom> entry : detectedMinions.entrySet()) {
			this.detectedMinions.put(entry.getKey(), entry.getValue().clone());
		}
		this.detectedWizards = new HashMap<>();
		for (Map.Entry<Long, WizardPhantom> entry : detectedWizards.entrySet()) {
			this.detectedWizards.put(entry.getKey(), entry.getValue().clone());
		}
		this.minionsOnLine = Arrays.copyOf(minionsOnLine, minionsOnLine.length);
	}

	public void updatePositions(World world) {
		updateMinions(world);
		updateWizards(world);
	}

	private void updateWizards(World world) {
		visibleIds.clear();
		visibleIds.addAll(detectedWizards.keySet());

		for (WizardPhantom wizardPhantom : detectedWizards.values()) {
			wizardPhantom.resetUpdate();
		}

		for (Wizard wizard : world.getWizards()) {
			if (wizard.getFaction() != Constants.getEnemyFaction()) {
				continue;
			}
			WizardPhantom wizardPhantom = detectedWizards.get(wizard.getId());
			if (wizardPhantom == null) {
				detectedWizards.put(wizard.getId(), new WizardPhantom(wizard, world.getTickIndex()));
				continue;
			}
			wizardPhantom.updateInfo(wizard, world.getTickIndex());
		}

		for (Long visibleId : visibleIds) {
			WizardPhantom wizardPhantom = detectedWizards.get(visibleId);
			if (wizardPhantom.isUpdated()) {
				continue;
			}
			double checkDistance = Constants.getGame().getWizardForwardSpeed() * 1.5 * (world.getTickIndex() - wizardPhantom.getLastSeenTick()) + 1.;

			if (checkDistance < 600. && Utils.isUnitVisible(wizardPhantom.getPosition(),
															checkDistance,
															world.getWizards(),
															world.getMinions(),
															world.getBuildings())) {
				detectedWizards.remove(visibleId); // yay, dead!
			} else {
				wizardPhantom.nextTick();
			}
		}
	}

	private void updateMinions(World world) {
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
										Constants.getGame().getMinionSpeed() * (world.getTickIndex() - minionPhantom.getLastSeenTick()) + .1,
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

	public HashMap<Long, MinionPhantom> getDetectedMinions() {
		return detectedMinions;
	}

	public HashMap<Long, WizardPhantom> getDetectedWizards() {
		return detectedWizards;
	}

	public EnemyPositionCalc clone() {
		return new EnemyPositionCalc(detectedMinions, detectedWizards, minionsOnLine);
	}


}
