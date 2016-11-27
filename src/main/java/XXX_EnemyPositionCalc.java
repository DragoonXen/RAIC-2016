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
public class XXX_EnemyPositionCalc {

	private HashMap<Long, XXX_MinionPhantom> detectedMinions;
	private HashMap<Long, XXX_WizardPhantom> detectedWizards;
	private XXX_BuildingPhantom[] buildingPhantoms = new XXX_BuildingPhantom[0];
	private static List<Long> visibleIds = new ArrayList<>();

	private List<XXX_WizardPhantom> deadEnemyWizards;
	private TreeMap<Long, Integer> lastBornTime;

	private int[] minionsOnLine;

	public XXX_EnemyPositionCalc() {
		detectedMinions = new HashMap<>();
		detectedWizards = new HashMap<>();
		deadEnemyWizards = new ArrayList<>();
		lastBornTime = new TreeMap<>();
		minionsOnLine = new int[]{0, 0, 0};
	}

	public XXX_EnemyPositionCalc(HashMap<Long, XXX_MinionPhantom> detectedMinions,
								 HashMap<Long, XXX_WizardPhantom> detectedWizards,
								 int[] minionsOnLine,
								 XXX_BuildingPhantom[] buildingPhantoms,
								 List<XXX_WizardPhantom> deadEnemyWizards,
								 TreeMap<Long, Integer> lastBornTime) {
		this.detectedMinions = new HashMap<>();
		for (Map.Entry<Long, XXX_MinionPhantom> entry : detectedMinions.entrySet()) {
			this.detectedMinions.put(entry.getKey(), entry.getValue().clone());
		}
		this.detectedWizards = new HashMap<>();
		for (Map.Entry<Long, XXX_WizardPhantom> entry : detectedWizards.entrySet()) {
			this.detectedWizards.put(entry.getKey(), entry.getValue().clone());
		}
		this.minionsOnLine = Arrays.copyOf(minionsOnLine, minionsOnLine.length);

		this.buildingPhantoms = new XXX_BuildingPhantom[buildingPhantoms.length];
		for (int i = 0; i != buildingPhantoms.length; ++i) {
			this.buildingPhantoms[i] = new XXX_BuildingPhantom(buildingPhantoms[i], false);
		}
		this.deadEnemyWizards = new ArrayList<>(deadEnemyWizards.size());
		for (XXX_WizardPhantom w : deadEnemyWizards) {
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
			buildingPhantoms = new XXX_BuildingPhantom[14];
			for (Building building : world.getBuildings()) {
				buildingPhantoms[phantomIdx++] = new XXX_BuildingPhantom(building, false);
				buildingPhantoms[phantomIdx++] = new XXX_BuildingPhantom(building, true);
			}
		} else {
			this.buildingPhantoms = XXX_Utils.updateBuildingPhantoms(world, buildingPhantoms);
		}
	}

	private void updateWizards(World world) {
		if (world.getTickIndex() == 0) {
			for (Wizard wizard : world.getWizards()) {
				XXX_WizardPhantom phantom = new XXX_WizardPhantom(wizard, 0, true);
				detectedWizards.put(phantom.getId(), phantom);
				lastBornTime.put(phantom.getId(), 0);
			}
		} else {
			Iterator<XXX_WizardPhantom> iterator = deadEnemyWizards.iterator();
			while (iterator.hasNext()) {
				XXX_WizardPhantom deadEnemyWizard = iterator.next();
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

		for (XXX_WizardPhantom wizardPhantom : detectedWizards.values()) {
			wizardPhantom.resetUpdate();
		}

		for (Wizard wizard : world.getWizards()) {
			if (wizard.getFaction() != XXX_Constants.getEnemyFaction()) {
				continue;
			}
			XXX_WizardPhantom wizardPhantom = detectedWizards.get(wizard.getId());
			if (wizardPhantom == null) { // unexpected, but... why not
				Iterator<XXX_WizardPhantom> iterator = deadEnemyWizards.iterator();
				while (iterator.hasNext()) {
					XXX_WizardPhantom deadEnemyWizard = iterator.next();
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
			XXX_WizardPhantom wizardPhantom = detectedWizards.get(visibleId);
			if (wizardPhantom.isUpdated()) {
				continue;
			}
			double checkDistance = XXX_Constants.getGame().getWizardForwardSpeed() * 1.5 * (world.getTickIndex() - wizardPhantom.getLastSeenTick()) + 1.;

			if (checkDistance < 600. && XXX_Utils.isPositionVisible(wizardPhantom.getPosition(),
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

		for (XXX_MinionPhantom minionPhantom : detectedMinions.values()) {
			minionPhantom.resetUpdate();
		}

		for (Minion minion : world.getMinions()) {
			if (minion.getFaction() != XXX_Constants.getEnemyFaction()) {
				continue;
			}
			XXX_MinionPhantom minionPhantom = detectedMinions.get(minion.getId());
			if (minionPhantom == null) {
				int line = XXX_Utils.whichLine(minion);
				detectedMinions.put(minion.getId(), new XXX_MinionPhantom(minion, world.getTickIndex(), line));
				++minionsOnLine[line];
				continue;
			}
			minionPhantom.updateInfo(minion, world.getTickIndex());
		}

		for (Long visibleId : visibleIds) {
			XXX_MinionPhantom minionPhantom = detectedMinions.get(visibleId);
			if (minionPhantom.isUpdated()) {
				continue;
			}
			if (minionPhantom.getLastSeenTick() + XXX_Constants.ENEMY_MINIONS_LOST_TIME <= world.getTickIndex() ||
					XXX_Utils.isPositionVisible(minionPhantom.getPosition(),
												XXX_Constants.getGame().getMinionSpeed() * (world.getTickIndex() - minionPhantom.getLastSeenTick()) + .1,
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

	public HashMap<Long, XXX_MinionPhantom> getDetectedMinions() {
		return detectedMinions;
	}

	public HashMap<Long, XXX_WizardPhantom> getDetectedWizards() {
		return detectedWizards;
	}

	public XXX_BuildingPhantom[] getBuildingPhantoms() {
		return buildingPhantoms;
	}

	public int[] getMinionsOnLine() {
		return minionsOnLine;
	}

	public XXX_EnemyPositionCalc clone() {
		return new XXX_EnemyPositionCalc(detectedMinions, detectedWizards, minionsOnLine, buildingPhantoms, deadEnemyWizards, lastBornTime);
	}


}
