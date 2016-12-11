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
public class YYY_EnemyPositionCalc {

	private HashMap<Long, YYY_MinionPhantom> detectedMinions;
	private HashMap<Long, YYY_WizardPhantom> detectedWizards;
	private YYY_BuildingPhantom[] buildingPhantoms = new YYY_BuildingPhantom[0];
	private static List<Long> visibleIds = new ArrayList<>();

	private List<YYY_WizardPhantom> deadEnemyWizards;
	private TreeMap<Long, Integer> lastBornTime;

	private int[] minionsOnLine;

	public YYY_EnemyPositionCalc() {
		detectedMinions = new HashMap<>();
		detectedWizards = new HashMap<>();
		deadEnemyWizards = new ArrayList<>();
		lastBornTime = new TreeMap<>();
		minionsOnLine = new int[]{0, 0, 0};
	}

	public YYY_EnemyPositionCalc(HashMap<Long, YYY_MinionPhantom> detectedMinions,
								 HashMap<Long, YYY_WizardPhantom> detectedWizards,
								 int[] minionsOnLine,
								 YYY_BuildingPhantom[] buildingPhantoms,
								 List<YYY_WizardPhantom> deadEnemyWizards,
								 TreeMap<Long, Integer> lastBornTime) {
		this.detectedMinions = new HashMap<>();
		for (Map.Entry<Long, YYY_MinionPhantom> entry : detectedMinions.entrySet()) {
			this.detectedMinions.put(entry.getKey(), entry.getValue().clone());
		}
		this.detectedWizards = new HashMap<>();
		for (Map.Entry<Long, YYY_WizardPhantom> entry : detectedWizards.entrySet()) {
			this.detectedWizards.put(entry.getKey(), entry.getValue().clone());
		}
		this.minionsOnLine = Arrays.copyOf(minionsOnLine, minionsOnLine.length);

		this.buildingPhantoms = new YYY_BuildingPhantom[buildingPhantoms.length];
		for (int i = 0; i != buildingPhantoms.length; ++i) {
			this.buildingPhantoms[i] = new YYY_BuildingPhantom(buildingPhantoms[i], false);
			if (!buildingPhantoms[i].isInvulnerable()) {
				this.buildingPhantoms[i].makeVulnerable();
			}
		}

		this.deadEnemyWizards = new ArrayList<>(deadEnemyWizards.size());
		for (YYY_WizardPhantom w : deadEnemyWizards) {
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
			buildingPhantoms = new YYY_BuildingPhantom[14];
			for (Building building : world.getBuildings()) {
				buildingPhantoms[phantomIdx++] = new YYY_BuildingPhantom(building, false);
				buildingPhantoms[phantomIdx++] = new YYY_BuildingPhantom(building, true);
			}
		} else {
			for (YYY_BuildingPhantom phantom : buildingPhantoms) {
				phantom.resetUpdate();
			}

			for (Building building : world.getBuildings()) {
				for (YYY_BuildingPhantom phantom : buildingPhantoms) {
					if (phantom.getId() == building.getId()) {
						phantom.updateInfo(building);
						break;
					}
				}
			}

			int hasBroken = 0;
			for (YYY_BuildingPhantom phantom : buildingPhantoms) {
				if (phantom.isUpdated()) {
					continue;
				}
				if (phantom.getFaction() == YYY_Constants.getCurrentFaction()) {
					phantom.setBroken(true);
					++hasBroken;
					continue;
				}
				if (YYY_Utils.isPositionVisible(phantom.getPosition(), .1, world.getWizards(), world.getMinions(), null)) {
					phantom.setBroken(true);
					++hasBroken;
				} else {
					if (phantom.getRemainingActionCooldownTicks() == 0 && YYY_Utils.hasAllyNearby(phantom, world, phantom.getAttackRange() + .1)) {
						phantom.fixRemainingActionCooldownTicks();
					}
					phantom.nextTick();
				}
			}
			if (hasBroken != 0) {
				YYY_BuildingPhantom[] updated = new YYY_BuildingPhantom[buildingPhantoms.length - hasBroken];
				int idx = 0;
				for (YYY_BuildingPhantom phantom : buildingPhantoms) {
					if (!phantom.isBroken()) {
						updated[idx++] = phantom;
					} else {
						long vulnerableId = YYY_BuildingPhantom.VULNERABLE_LINK_MAPPING.get(phantom.getId());
						for (YYY_BuildingPhantom phantomV : buildingPhantoms) {
							if (phantomV.getId() == vulnerableId) {
								phantomV.makeVulnerable();
							}
						}
					}
				}
				this.buildingPhantoms = updated;
			}
		}
	}

	private void updateWizards(World world) {
		if (world.getTickIndex() == 0) {
			for (Wizard wizard : world.getWizards()) {
				YYY_WizardPhantom phantom = new YYY_WizardPhantom(wizard, 0, true);
				detectedWizards.put(phantom.getId(), phantom);
				lastBornTime.put(phantom.getId(), 0);
			}
		} else {
			Iterator<YYY_WizardPhantom> iterator = deadEnemyWizards.iterator();
			while (iterator.hasNext()) {
				YYY_WizardPhantom deadEnemyWizard = iterator.next();
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

		for (YYY_WizardPhantom wizardPhantom : detectedWizards.values()) {
			wizardPhantom.resetUpdate();
		}

		for (Wizard wizard : world.getWizards()) {
			if (wizard.getFaction() != YYY_Constants.getEnemyFaction()) {
				continue;
			}
			YYY_WizardPhantom wizardPhantom = detectedWizards.get(wizard.getId());
			if (wizardPhantom == null) { // unexpected, but... why not
				Iterator<YYY_WizardPhantom> iterator = deadEnemyWizards.iterator();
				while (iterator.hasNext()) {
					YYY_WizardPhantom deadEnemyWizard = iterator.next();
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
			YYY_WizardPhantom wizardPhantom = detectedWizards.get(visibleId);
			if (wizardPhantom.isUpdated()) {
				continue;
			}
			double checkDistance = YYY_Constants.getGame().getWizardForwardSpeed() * 1.5 * (world.getTickIndex() - wizardPhantom.getLastSeenTick()) + 1.;

			if (checkDistance < 600. && YYY_Utils.isPositionVisible(wizardPhantom.getPosition(),
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

		for (YYY_MinionPhantom minionPhantom : detectedMinions.values()) {
			minionPhantom.resetUpdate();
		}

		for (Minion minion : world.getMinions()) {
			if (minion.getFaction() != YYY_Constants.getEnemyFaction()) {
				continue;
			}
			YYY_MinionPhantom minionPhantom = detectedMinions.get(minion.getId());
			if (minionPhantom == null) {
				int line = YYY_Utils.whichLine(minion);
				detectedMinions.put(minion.getId(), new YYY_MinionPhantom(minion, world.getTickIndex(), line));
				++minionsOnLine[line];
				continue;
			}
			minionPhantom.updateInfo(minion, world.getTickIndex());
		}

		for (Long visibleId : visibleIds) {
			YYY_MinionPhantom minionPhantom = detectedMinions.get(visibleId);
			if (minionPhantom.isUpdated()) {
				continue;
			}
			if (minionPhantom.getLastSeenTick() + YYY_Constants.ENEMY_MINIONS_LOST_TIME <= world.getTickIndex() ||
					YYY_Utils.isPositionVisible(minionPhantom.getPosition(),
												YYY_Constants.getGame().getMinionSpeed() * (world.getTickIndex() - minionPhantom.getLastSeenTick()) + .1,
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

	public HashMap<Long, YYY_MinionPhantom> getDetectedMinions() {
		return detectedMinions;
	}

	public HashMap<Long, YYY_WizardPhantom> getDetectedWizards() {
		return detectedWizards;
	}

	public YYY_BuildingPhantom[] getBuildingPhantoms() {
		return buildingPhantoms;
	}

	public int[] getMinionsOnLine() {
		return minionsOnLine;
	}

	public YYY_EnemyPositionCalc clone() {
		return new YYY_EnemyPositionCalc(detectedMinions, detectedWizards, minionsOnLine, buildingPhantoms, deadEnemyWizards, lastBornTime);
	}


}
