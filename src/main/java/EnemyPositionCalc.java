import model.Building;
import model.Minion;
import model.Wizard;
import model.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by dragoon on 11/20/16.
 */
public class EnemyPositionCalc {

	private HashMap<Long, MinionPhantom> detectedMinions;
	private HashMap<Long, WizardPhantom> detectedWizards;
	private BuildingPhantom[] buildingPhantoms = new BuildingPhantom[0];
	private static List<Long> visibleIds = new ArrayList<>();

	private List<WizardPhantom> deadEnemyWizards;
	private TreeMap<Long, Integer> lastBornTime;

	private int[] minionsOnLine;

	public EnemyPositionCalc() {
		detectedMinions = new HashMap<>();
		detectedWizards = new HashMap<>();
		deadEnemyWizards = new ArrayList<>();
		lastBornTime = new TreeMap<>();
		minionsOnLine = new int[]{0, 0, 0};
	}

	public EnemyPositionCalc(HashMap<Long, MinionPhantom> detectedMinions,
							 HashMap<Long, WizardPhantom> detectedWizards,
							 int[] minionsOnLine,
							 BuildingPhantom[] buildingPhantoms,
							 List<WizardPhantom> deadEnemyWizards,
							 TreeMap<Long, Integer> lastBornTime) {
		this.detectedMinions = new HashMap<>();
		for (Map.Entry<Long, MinionPhantom> entry : detectedMinions.entrySet()) {
			this.detectedMinions.put(entry.getKey(), entry.getValue().clone());
		}
		this.detectedWizards = new HashMap<>();
		for (Map.Entry<Long, WizardPhantom> entry : detectedWizards.entrySet()) {
			this.detectedWizards.put(entry.getKey(), entry.getValue().clone());
		}
		this.minionsOnLine = Arrays.copyOf(minionsOnLine, minionsOnLine.length);

		this.buildingPhantoms = new BuildingPhantom[buildingPhantoms.length];
		for (int i = 0; i != buildingPhantoms.length; ++i) {
			this.buildingPhantoms[i] = new BuildingPhantom(buildingPhantoms[i], false);
		}
		this.deadEnemyWizards = new ArrayList<>(deadEnemyWizards.size());
		for (WizardPhantom w : deadEnemyWizards) {
			this.deadEnemyWizards.add(w.clone());
		}

		this.lastBornTime = new TreeMap<>(lastBornTime);
	}

	public void updatePositions(World world) {
		updateMinions(world);
		updateWizards(world);
		updateBuildings(world);
	}

	private void updateBuildings(World world) {
		if (world.getTickIndex() == 0) {
			int phantomIdx = 0;
			buildingPhantoms = new BuildingPhantom[14];
			for (Building building : world.getBuildings()) {
				buildingPhantoms[phantomIdx++] = new BuildingPhantom(building, false);
				buildingPhantoms[phantomIdx++] = new BuildingPhantom(building, true);
			}
		} else {
			this.buildingPhantoms = Utils.updateBuildingPhantoms(world, buildingPhantoms);
		}
	}

	private void updateWizards(World world) {
		if (world.getTickIndex() == 0) {
			for (Wizard wizard : world.getWizards()) {
				WizardPhantom phantom = new WizardPhantom(wizard, 0, true);
				detectedWizards.put(phantom.getId(), phantom);
				lastBornTime.put(phantom.getId(), 0);
			}
		} else {
			Iterator<WizardPhantom> iterator = deadEnemyWizards.iterator();
			while (iterator.hasNext()) {
				WizardPhantom deadEnemyWizard = iterator.next();
				if (world.getTickIndex() - lastBornTime.get(deadEnemyWizard.getId()) >= 2400 && world.getTickIndex() - deadEnemyWizard.getLastSeenTick() >= 1200) {
					lastBornTime.put(deadEnemyWizard.getId(), world.getTickIndex());
					iterator.remove();
					detectedWizards.put(deadEnemyWizard.getId(), deadEnemyWizard);
					deadEnemyWizard.reborn(world.getTickIndex());
				}
			}
		}
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
			if (wizardPhantom == null) { // unexpected, but... why not
				Iterator<WizardPhantom> iterator = deadEnemyWizards.iterator();
				while (iterator.hasNext()) {
					WizardPhantom deadEnemyWizard = iterator.next();
					if (deadEnemyWizard.getId() == wizard.getId()) {
						iterator.remove();
						detectedWizards.put(wizard.getId(), deadEnemyWizard);
						deadEnemyWizard.updateInfo(wizard, world.getTickIndex());
						break;
					}
				}
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
				deadEnemyWizards.add(wizardPhantom);
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

	public BuildingPhantom[] getBuildingPhantoms() {
		return buildingPhantoms;
	}

	public EnemyPositionCalc clone() {
		return new EnemyPositionCalc(detectedMinions, detectedWizards, minionsOnLine, buildingPhantoms, deadEnemyWizards, lastBornTime);
	}


}
