import model.Bonus;
import model.Building;
import model.BuildingType;
import model.CircularUnit;
import model.Faction;
import model.LaneType;
import model.LivingUnit;
import model.Minion;
import model.Projectile;
import model.ProjectileType;
import model.SkillType;
import model.Status;
import model.StatusType;
import model.Unit;
import model.Wizard;
import model.World;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class XXX_Utils {
	public final static Comparator<Map.Entry<Double, CircularUnit>> AIM_SORT_COMPARATOR = (o1, o2) -> o2.getKey().compareTo(o1.getKey());
	private static double[] lineDistance = new double[XXX_Constants.getLines().length];

	public static int whichLine(Unit unit) {
		int min = 0;
		lineDistance[0] = XXX_Constants.getLines()[0].getDistanceTo(unit);
		for (int i = 1; i != XXX_Constants.getLines().length; ++i) {
			lineDistance[i] = XXX_Constants.getLines()[i].getDistanceTo(unit);
			if (lineDistance[min] > lineDistance[i]) {
				min = i;
			}
		}
		return min;
	}

	public static LaneType getDefaultMyLine(int selfId) {
		switch (selfId) {
			case 1:
			case 2:
			case 6:
			case 7:
				return LaneType.TOP;
			case 5:
			case 4:
			case 9:
			case 10:
				return LaneType.BOTTOM;
			default:
				return LaneType.MIDDLE;
		}
	}

	public static XXX_FilteredWorld filterWorld(World world, XXX_Point point, Building[] buildings) {
		List<Projectile> projectiles = new ArrayList<>();
		for (Projectile projectile : world.getProjectiles()) {
			if (projectile.getOwnerUnitId() == XXX_Variables.self.getId()) {
				continue;
			}
			projectiles.add(projectile);
		}
		Projectile[] filteredProjectiles = new Projectile[projectiles.size()];
		projectiles.toArray(filteredProjectiles);
		return new XXX_FilteredWorld(world.getTickIndex(),
									 world.getTickCount(),
									 world.getWidth(),
									 world.getHeight(),
									 world.getPlayers(),
									 removeMe(filterUnit(world.getWizards(), point, XXX_FilteredWorld.FilterType.FIGHT)),
									 filterUnit(world.getMinions(), point, XXX_FilteredWorld.FilterType.FIGHT),
									 filterUnit(filteredProjectiles, point, XXX_FilteredWorld.FilterType.FIGHT),
									 filterUnit(world.getBonuses(), point, XXX_FilteredWorld.FilterType.FIGHT),
									 filterUnit(buildings, point, XXX_FilteredWorld.FilterType.FIGHT),
									 filterUnit(world.getTrees(), point, XXX_FilteredWorld.FilterType.MOVE),
									 point);
	}

	public static <T extends CircularUnit> List<T> filterUnit(T[] units, XXX_Point point, XXX_FilteredWorld.FilterType filterType) {
		List<T> unitsList = new ArrayList<>();
		if (units.length == 0) {
			return unitsList;
		}
		double distance = 0.;
		boolean isBuilding = false;
		switch (filterType) {
			case FIGHT:
				distance = XXX_Constants.getFightDistanceFilter();
				break;
			case MOVE:
				distance = XXX_Constants.MOVE_DISTANCE_FILTER + XXX_Constants.getGame().getWizardRadius();
				break;
			case AIM:
				distance = XXX_Variables.self.getCastRange();
				if (units[0] instanceof Building) {
					isBuilding = true;
				}
				break;
		}
		switch (filterType) {
			case MOVE:
				for (T unit : units) {
					if (unit.getDistanceTo(point.getX(), point.getY()) < distance + unit.getRadius()) {
						unitsList.add(unit);
					}
				}
				break;
			case FIGHT:
				for (T unit : units) {
					if (unit.getDistanceTo(point.getX(), point.getY()) <= distance) {
						unitsList.add(unit);
					}
				}
				break;
			case AIM:
				if (isBuilding) {
					for (T unit : units) {
						if (unit.getDistanceTo(point.getX(), point.getY()) - unit.getRadius() <= distance) {
							unitsList.add(unit);
						}
					}
				} else {
					for (T unit : units) {
						if (unit.getDistanceTo(point.getX(), point.getY()) <= distance) {
							unitsList.add(unit);
						}
					}
				}
				break;
		}
		return unitsList;
	}

	private static List<Wizard> removeMe(List<Wizard> wizards) {
		Iterator<Wizard> iterator = wizards.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().isMe()) {
				iterator.remove();
				return wizards;
			}
		}
		return wizards;
	}

	public static double getDirectionTo(Unit unit, XXX_Point point) {
		return Math.atan2(point.getY() - unit.getY(), point.getX() - unit.getX());
	}

	public static XXX_ScanMatrixItem[][] createScanMatrix() {
		XXX_ScanMatrixItem[][] matrix = new XXX_ScanMatrixItem[(int) Math.round((XXX_Constants.MOVE_FWD_DISTANCE + XXX_Constants.MOVE_BACK_DISTANCE) / XXX_Constants.MOVE_SCAN_STEP + 1.01)]
				[(int) Math.round((XXX_Constants.MOVE_SIDE_DISTANCE + XXX_Constants.MOVE_SIDE_DISTANCE) / XXX_Constants.MOVE_SCAN_STEP + 1.01)];
		double maxBonus = Math.pow(XXX_FastMath.hypot(-matrix.length - 30, -XXX_Constants.CURRENT_PT_Y), XXX_Constants.FORWARD_MOVE_FROM_DISTANCE_POWER);
		for (int i = 0; i != matrix.length; ++i) {
			for (int j = 0; j != matrix[0].length; ++j) {
				matrix[i][j] = new XXX_ScanMatrixItem(i,
													  j,
													  maxBonus - Math.pow(XXX_FastMath.hypot(i - matrix.length - 30, j - XXX_Constants.CURRENT_PT_Y),
																		  XXX_Constants.FORWARD_MOVE_FROM_DISTANCE_POWER));
			}
		}
		int[] di = new int[]{1, 0, 0, -1};
		int[] dj = new int[]{0, 1, -1, 0};
		for (int i = 0; i != matrix.length; ++i) {
			for (int j = 0; j != matrix[0].length; ++j) {
				List<XXX_ScanMatrixItem> neighbour = new ArrayList<>();
				for (int k = 0; k != 4; ++k) {
					int nx = i + di[k];
					int ny = j + dj[k];
					if (nx < 0 || nx >= matrix.length || ny < 0 || ny >= matrix[0].length) {
						continue;
					}
					neighbour.add(matrix[nx][ny]);
				}
				matrix[i][j].setNeighbours(neighbour.toArray(new XXX_ScanMatrixItem[neighbour.size()]));
			}
		}
		List<Map.Entry<Double, XXX_ScanMatrixItem>> pointsMap = new ArrayList<>();
		for (int i = 0; i != matrix.length; ++i) {
			for (int j = 0; j != matrix[0].length; ++j) {
				pointsMap.add(new AbstractMap.SimpleEntry<>(XXX_FastMath.hypot(XXX_Constants.CURRENT_PT_X - i, XXX_Constants.CURRENT_PT_Y - j), matrix[i][j]));
			}
		}
		Collections.sort(pointsMap, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
		int i = 0;
		for (Map.Entry<Double, XXX_ScanMatrixItem> doubleScanMatrixItemEntry : pointsMap) {
			XXX_ScanMatrixItem item = doubleScanMatrixItemEntry.getValue();
			item.setDistanceFromSelf(doubleScanMatrixItemEntry.getKey());
		}
		return matrix;
	}

	public static double normalizeAngle(Double angle) {
		while (angle > Math.PI) {
			angle -= Math.PI * 2;
		}
		while (angle < -Math.PI) {
			angle += Math.PI * 2;
		}
		return angle;
	}

	public static double distancePointToSegment(XXX_Point point, XXX_Point segA, XXX_Point segB) {
		return point.segmentNorm(nearestSegmentPoint(point, segA, segB));
	}

	public static XXX_Point nearestSegmentPoint(XXX_Point point, XXX_Point segA, XXX_Point segB) {
		XXX_Point vectorV = segA.negateCopy(segB);
		XXX_Point vectorW = point.negateCopy(segB);
		double c1 = vectorV.scalarMult(vectorW);
		if (c1 <= 0)
			return segB;
		double c2 = vectorV.scalarMult(vectorV);
		if (c2 <= c1)
			return segA;
		return segB.addWithCopy(vectorV.mult(c1 / c2));
	}

	private static CircularUnit blockUnit;

	public static boolean isAvailableTile(List<CircularUnit> units, double x, double y) {
		double radius = XXX_Constants.getGame().getWizardRadius();
		if (x < radius || y < radius || x + radius > XXX_Constants.getGame().getMapSize() || y + radius > XXX_Constants.getGame().getMapSize()) {
			return false;
		}
		Iterator<CircularUnit> iterator = units.iterator();
		while (iterator.hasNext()) {
			CircularUnit unit = iterator.next();
			if (XXX_FastMath.hypot(unit.getX() - x, unit.getY() - y) < unit.getRadius() + radius + XXX_Constants.STUCK_FIX_RADIUS_ADD) {
				return false;
			}
		}
		return true;
	}

	public static void calcTilesAvailable(List<CircularUnit> units, XXX_ScanMatrixItem[] items) {
		double radius = XXX_Constants.getGame().getWizardRadius();
		double x, y;
		List<CircularUnit> filteredUnits = new ArrayList<>(units.size());
		for (CircularUnit unit : units) {
			x = XXX_Utils.distancePointToSegment(new XXX_Point(unit.getX(), unit.getY()), items[0], items[items.length - 1]);
			if (x < unit.getRadius() + radius + XXX_Constants.STUCK_FIX_RADIUS_ADD) {
				filteredUnits.add(unit);
			}
		}
		blockUnit = null;
		for (int i = 0; i != items.length; ++i) {
			XXX_ScanMatrixItem item = items[i];
			x = item.getX();
			y = item.getY();
			if (x < radius || y < radius || x + radius > XXX_Constants.getGame().getMapSize() || y + radius > XXX_Constants.getGame().getMapSize()) {
				item.setAvailable(false);
				continue;
			}
			if (blockUnit != null) {
				if (XXX_FastMath.hypot(blockUnit.getX() - x, blockUnit.getY() - y) < blockUnit.getRadius() + radius + XXX_Constants.STUCK_FIX_RADIUS_ADD) {
					item.setAvailable(false);
					continue;
				}
				filteredUnits.remove(blockUnit);
				blockUnit = null;
			}
			for (CircularUnit unit : filteredUnits) {
				if (XXX_FastMath.hypot(unit.getX() - x, unit.getY() - y) < unit.getRadius() + radius + XXX_Constants.STUCK_FIX_RADIUS_ADD) {
					item.setAvailable(false);
					blockUnit = unit;
					break;
				}
			}
		}
	}

	public static void calcTileScore(XXX_ScanMatrixItem item,
									 XXX_FilteredWorld filteredWorld,
									 XXX_BaseLine myLineCalc,
									 Wizard self,
									 XXX_UnitScoreCalculation unitScoreCalculation,
									 boolean enemyFound) {
		item.setAvailable(XXX_Utils.isAvailableTile(filteredWorld.getAllBlocksList(), item.getX(), item.getY()));
		if (!item.isAvailable()) {
			return;
		}
		double distanceTo = myLineCalc.calcLineDistanceOtherDanger(item);
		if (distanceTo > 0.) {
			item.addOtherDanger(distanceTo);
		}
		for (Bonus bonus : filteredWorld.getBonuses()) {
			if (XXX_FastMath.hypot(self, bonus) > XXX_Constants.getFightDistanceFilter()) {
				continue;
			}
			XXX_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(bonus.getId());
			structure.applyScores(item, XXX_FastMath.hypot(bonus, item));
		}
		if (XXX_Utils.getTicksToBonusSpawn(filteredWorld.getTickIndex()) < 250) {
			for (int i = 0; i != XXX_BonusesPossibilityCalcs.BONUSES_POINTS.length; ++i) {
				if (XXX_FastMath.hypot(self, XXX_BonusesPossibilityCalcs.BONUSES_POINTS[i]) > XXX_Constants.getFightDistanceFilter()) {
					continue;
				}
				XXX_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(i - 5);
				structure.applyScores(item, XXX_FastMath.hypot(XXX_BonusesPossibilityCalcs.BONUSES_POINTS[i], item));
			}
		}
		if (!enemyFound) {
			return;
		}
		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == XXX_Constants.getCurrentFaction()) {
				continue;
			}
			XXX_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(minion.getId());
			structure.applyScores(item, XXX_FastMath.hypot(minion, item));
		}
		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == XXX_Constants.getCurrentFaction()) {
				continue;
			}
			XXX_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(wizard.getId());
			structure.applyScores(item, XXX_FastMath.hypot(wizard, item));
		}
		for (Building building : filteredWorld.getBuildings()) {
			if (building.getFaction() == XXX_Constants.getCurrentFaction()) {
				continue;
			}
			XXX_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(building.getId());
			structure.applyScores(item, XXX_FastMath.hypot(building.getX() - item.getX(), building.getY() - item.getY()));
		}
	}

	public static boolean hasEnemy(Minion[] units, XXX_AgressiveNeutralsCalcs agressiveNeutralsCalcs) {
		for (LivingUnit unit : units) {
			if (unit.getFaction() == XXX_Constants.getEnemyFaction() || unit.getFaction() == Faction.NEUTRAL && agressiveNeutralsCalcs.isMinionAgressive(unit.getId())) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasEnemy(LivingUnit[] units) {
		for (LivingUnit unit : units) {
			if (unit.getFaction() == XXX_Constants.getEnemyFaction()) {
				return true;
			}
		}
		return false;
	}

	public static boolean wizardHasStatus(Wizard wizard, StatusType statusType) {
		for (Status status : wizard.getStatuses()) {
			if (status.getType() == statusType) {
				return true;
			}
		}
		return false;
	}

	public static int wizardStatusTicks(Wizard wizard, StatusType statusType) {
		int hasteStatusTicks = -1;
		for (Status status : wizard.getStatuses()) {
			if (status.getType() == statusType) {
				hasteStatusTicks = Math.max(status.getRemainingDurationTicks(), hasteStatusTicks);
			}
		}
		return hasteStatusTicks;
	}

	private static int[] skillsCount = new int[5];
	private static int[] aurasCount = new int[5];

	public static void calcCurrentSkillBonuses(Wizard self, XXX_FilteredWorld filteredWorld) {
		Arrays.fill(skillsCount, 0);
		Arrays.fill(aurasCount, 0);
		for (SkillType skillType : self.getSkills()) {
			switch (skillType) {
				case RANGE_BONUS_PASSIVE_1:
					skillsCount[XXX_SkillFork.RANGE.ordinal()] = Math.max(skillsCount[XXX_SkillFork.RANGE.ordinal()], 1);
					break;
				case RANGE_BONUS_AURA_1:
					aurasCount[XXX_SkillFork.RANGE.ordinal()] = Math.max(aurasCount[XXX_SkillFork.RANGE.ordinal()], 1);
					break;
				case RANGE_BONUS_PASSIVE_2:
					skillsCount[XXX_SkillFork.RANGE.ordinal()] = 2;
					break;
				case RANGE_BONUS_AURA_2:
					aurasCount[XXX_SkillFork.RANGE.ordinal()] = 2;
					break;
				case MAGICAL_DAMAGE_BONUS_PASSIVE_1:
					skillsCount[XXX_SkillFork.MAGICAL_DAMAGE.ordinal()] = Math.max(skillsCount[XXX_SkillFork.MAGICAL_DAMAGE.ordinal()], 1);
					break;
				case MAGICAL_DAMAGE_BONUS_AURA_1:
					aurasCount[XXX_SkillFork.MAGICAL_DAMAGE.ordinal()] = Math.max(aurasCount[XXX_SkillFork.MAGICAL_DAMAGE.ordinal()], 1);
					break;
				case MAGICAL_DAMAGE_BONUS_PASSIVE_2:
					skillsCount[XXX_SkillFork.MAGICAL_DAMAGE.ordinal()] = 2;
					break;
				case MAGICAL_DAMAGE_BONUS_AURA_2:
					aurasCount[XXX_SkillFork.MAGICAL_DAMAGE.ordinal()] = 2;
					break;
				case STAFF_DAMAGE_BONUS_PASSIVE_1:
					skillsCount[XXX_SkillFork.STAFF_DAMAGE.ordinal()] = Math.max(skillsCount[XXX_SkillFork.STAFF_DAMAGE.ordinal()], 1);
					break;
				case STAFF_DAMAGE_BONUS_AURA_1:
					aurasCount[XXX_SkillFork.STAFF_DAMAGE.ordinal()] = Math.max(aurasCount[XXX_SkillFork.STAFF_DAMAGE.ordinal()], 1);
					break;
				case STAFF_DAMAGE_BONUS_PASSIVE_2:
					skillsCount[XXX_SkillFork.STAFF_DAMAGE.ordinal()] = 2;
					break;
				case STAFF_DAMAGE_BONUS_AURA_2:
					aurasCount[XXX_SkillFork.STAFF_DAMAGE.ordinal()] = 2;
					break;
				case MOVEMENT_BONUS_FACTOR_PASSIVE_1:
					skillsCount[XXX_SkillFork.MOVEMENT.ordinal()] = Math.max(skillsCount[XXX_SkillFork.MOVEMENT.ordinal()], 1);
					break;
				case MOVEMENT_BONUS_FACTOR_AURA_1:
					aurasCount[XXX_SkillFork.MOVEMENT.ordinal()] = Math.max(aurasCount[XXX_SkillFork.MOVEMENT.ordinal()], 1);
					break;
				case MOVEMENT_BONUS_FACTOR_PASSIVE_2:
					skillsCount[XXX_SkillFork.MOVEMENT.ordinal()] = 2;
					break;
				case MOVEMENT_BONUS_FACTOR_AURA_2:
					aurasCount[XXX_SkillFork.MOVEMENT.ordinal()] = 2;
					break;
				case MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1:
					skillsCount[XXX_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = Math.max(skillsCount[XXX_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()],
																							  1);
					break;
				case MAGICAL_DAMAGE_ABSORPTION_AURA_1:
					aurasCount[XXX_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = Math.max(aurasCount[XXX_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()], 1);
					break;
				case MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2:
					skillsCount[XXX_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = 2;
					break;
				case MAGICAL_DAMAGE_ABSORPTION_AURA_2:
					aurasCount[XXX_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = 2;
					break;
			}
		}
		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() != XXX_Constants.getCurrentFaction()) {
				continue;
			}
			if (wizard.getDistanceTo(self) < XXX_Constants.getGame().getAuraSkillRange()) {
				for (SkillType skillType : wizard.getSkills()) {
					switch (skillType) {
						case RANGE_BONUS_AURA_1:
							aurasCount[XXX_SkillFork.RANGE.ordinal()] = Math.max(aurasCount[XXX_SkillFork.RANGE.ordinal()], 1);
							break;
						case RANGE_BONUS_AURA_2:
							aurasCount[XXX_SkillFork.RANGE.ordinal()] = 2;
							break;
						case MAGICAL_DAMAGE_BONUS_AURA_1:
							aurasCount[XXX_SkillFork.MAGICAL_DAMAGE.ordinal()] = Math.max(aurasCount[XXX_SkillFork.MAGICAL_DAMAGE.ordinal()], 1);
							break;
						case MAGICAL_DAMAGE_BONUS_AURA_2:
							aurasCount[XXX_SkillFork.MAGICAL_DAMAGE.ordinal()] = 2;
							break;
						case STAFF_DAMAGE_BONUS_AURA_1:
							aurasCount[XXX_SkillFork.STAFF_DAMAGE.ordinal()] = Math.max(aurasCount[XXX_SkillFork.STAFF_DAMAGE.ordinal()], 1);
							break;
						case STAFF_DAMAGE_BONUS_AURA_2:
							aurasCount[XXX_SkillFork.STAFF_DAMAGE.ordinal()] = 2;
							break;
						case MOVEMENT_BONUS_FACTOR_AURA_1:
							aurasCount[XXX_SkillFork.MOVEMENT.ordinal()] = Math.max(aurasCount[XXX_SkillFork.MOVEMENT.ordinal()], 1);
							break;
						case MOVEMENT_BONUS_FACTOR_AURA_2:
							aurasCount[XXX_SkillFork.MOVEMENT.ordinal()] = 2;
							break;
						case MAGICAL_DAMAGE_ABSORPTION_AURA_1:
							aurasCount[XXX_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = Math.max(aurasCount[XXX_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()],
																									 1);
							break;
						case MAGICAL_DAMAGE_ABSORPTION_AURA_2:
							aurasCount[XXX_SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = 2;
							break;
					}
				}
			}
		}
		XXX_Variables.turnFactor = 1.;
		XXX_Variables.moveFactor = 1. + XXX_Constants.getGame().getMovementBonusFactorPerSkillLevel() * (aurasCount[XXX_SkillFork.MOVEMENT.ordinal()] + skillsCount[XXX_SkillFork.MOVEMENT.ordinal()]);
		if (XXX_Utils.wizardHasStatus(self, StatusType.HASTENED)) {
			XXX_Variables.turnFactor += XXX_Constants.getGame().getHastenedRotationBonusFactor();
			XXX_Variables.moveFactor += XXX_Constants.getGame().getHastenedMovementBonusFactor();
		}
		XXX_Variables.staffDamage = XXX_Constants.getGame().getStaffDamage() +
				XXX_Constants.getGame().getStaffDamageBonusPerSkillLevel() * (aurasCount[XXX_SkillFork.STAFF_DAMAGE.ordinal()] + skillsCount[XXX_SkillFork.STAFF_DAMAGE.ordinal()]);
		XXX_Variables.magicDamageBonus = XXX_Constants.getGame().getMagicalDamageBonusPerSkillLevel() * (aurasCount[XXX_SkillFork.MAGICAL_DAMAGE.ordinal()] + skillsCount[XXX_SkillFork.MAGICAL_DAMAGE.ordinal()]);
	}

	public static XXX_BuildingPhantom[] updateBuildingPhantoms(World world, XXX_BuildingPhantom[] phantoms) {
		for (XXX_BuildingPhantom phantom : phantoms) {
			phantom.resetUpdate();
		}
		for (Building building : world.getBuildings()) {
			for (XXX_BuildingPhantom phantom : phantoms) {
				if (phantom.getId() == building.getId()) {
					phantom.updateInfo(building);
					break;
				}
			}
		}
		int hasBroken = 0;
		for (XXX_BuildingPhantom phantom : phantoms) {
			if (phantom.isUpdated()) {
				continue;
			}
			if (phantom.getFaction() == XXX_Constants.getCurrentFaction()) {
				phantom.setBroken(true);
				++hasBroken;
				continue;
			}
			if (XXX_Utils.isPositionVisible(phantom.getPosition(), .1, world.getWizards(), world.getMinions(), null)) {
				phantom.setBroken(true);
				++hasBroken;
			} else {
				if (phantom.getRemainingActionCooldownTicks() == 0 && XXX_Utils.hasAllyNearby(phantom, world, phantom.getAttackRange() + .1)) {
					phantom.fixRemainingActionCooldownTicks();
				}
				phantom.nextTick();
			}
		}
		XXX_BuildingPhantom[] response = phantoms;
		if (hasBroken != 0) {
			response = new XXX_BuildingPhantom[phantoms.length - hasBroken];
			int idx = 0;
			for (XXX_BuildingPhantom phantom : phantoms) {
				if (!phantom.isBroken()) {
					response[idx++] = phantom;
				}
			}
		}
		return response;
	}

	public final static double[] PROJECTIVE_SPEED = new double[]
			{XXX_Constants.getGame().getMagicMissileSpeed(), XXX_Constants.getGame().getFrostBoltSpeed(), XXX_Constants.getGame().getFireballSpeed(), XXX_Constants.getGame().getDartSpeed()};
	public final static int[] PROJECTIVE_DAMAGE = new int[]
			{XXX_Constants.getGame().getMagicMissileDirectDamage(), XXX_Constants.getGame().getFrostBoltDirectDamage(), XXX_Constants.getGame().getFireballExplosionMaxDamage(), XXX_Constants.getGame().getDartDirectDamage()};

	public static int getProjectileDamage(Projectile projectile) {
		return getProjectileDamage(projectile.getType());
	}

	public static int getProjectileDamage(ProjectileType projectileType) {
		return PROJECTIVE_DAMAGE[projectileType.ordinal()];
	}

	public static double getSelfProjectileDamage(ProjectileType projectileType) {
		return (getProjectileDamage(projectileType) + XXX_Variables.magicDamageBonus) * (wizardHasStatus(XXX_Variables.self,
																										 StatusType.EMPOWERED) ? XXX_Constants.getGame().getEmpoweredDamageFactor() : 1);
	}

	public static void fillProjectilesSim(XXX_FilteredWorld filteredWorld, HashMap<Long, Double> projectilesDTL) {
		XXX_Variables.projectilesSim.clear();
		for (Projectile projectile : filteredWorld.getProjectiles()) {
			XXX_Variables.projectilesSim.add(new AbstractMap.SimpleEntry<Projectile, Double>(projectile, projectilesDTL.get(projectile.getId())));
		}
	}

	public static double checkProjectiveCollision(XXX_Point point, int ticks) {
		double selfRadius = XXX_Variables.self.getRadius();
		double damage = 0.;
		Iterator<AbstractMap.SimpleEntry<Projectile, Double>> iterator = XXX_Variables.projectilesSim.iterator();
		while (iterator.hasNext()) {
			AbstractMap.SimpleEntry<Projectile, Double> next = iterator.next();
			Projectile projectile = next.getKey();
			XXX_Point projectileVector = new XXX_Point(projectile.getSpeedX(), projectile.getSpeedY());
			XXX_Point startProjectilePoint = new XXX_Point(projectile.getX() + projectile.getSpeedX() * ticks,
														   projectile.getY() + projectile.getSpeedY() * ticks);
			boolean remove = false;
			if (projectileVector.vectorNorm() >= next.getValue() - .0001) {
				projectileVector.fixVectorLength(next.getValue());
				remove = true;
			} else {
				next.setValue(next.getValue() - XXX_Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()]);
			}
			projectileVector.add(startProjectilePoint);
			if (XXX_Utils.distancePointToSegment(point, startProjectilePoint, projectileVector) < projectile.getRadius() + selfRadius + .001) {
				damage += getProjectileDamage(projectile);
				remove = true;
			}
			if (remove) {
				iterator.remove();
			}
		}
		return damage;
	}

	public static double updateMaxModule(double value, double maxModule) {
		if (Math.abs(value) <= maxModule) {
			return value;
		}
		return value > 0. ? maxModule : -maxModule;
	}

	public static int unitsCountAtDistance(LivingUnit[] units, Unit unitCountFor, double distance) {
		int result = 0;
		for (LivingUnit unit : units) {
			if (XXX_FastMath.hypot(unit.getX() - unitCountFor.getX(), unit.getY() - unitCountFor.getY()) < distance) {
				++result;
			}
		}
		return result;
	}

	public static int unitsCountCloseToDestination(LivingUnit[] units, XXX_Point destination) {
		if (destination == null) {
			return 0;
		}
		int result = 0;
		for (LivingUnit unit : units) {
			if (XXX_FastMath.hypot(unit.getX() - destination.getX(), unit.getY() - destination.getY()) < XXX_Constants.getGame().getWizardRadius() +
					unit.getRadius() +
					XXX_Constants.MOVE_SCAN_DIAGONAL_DISTANCE + .1) {
				++result;
			}
		}
		return result;
	}

	public static int unitsCountCloseToDestination(List<CircularUnit> units, XXX_Point destination) {
		if (destination == null) {
			return 0;
		}
		int result = 0;
		for (CircularUnit unit : units) {
			if (XXX_FastMath.hypot(unit.getX() - destination.getX(), unit.getY() - destination.getY()) < XXX_Constants.getGame().getWizardRadius() +
					unit.getRadius() +
					XXX_Constants.MOVE_SCAN_DIAGONAL_DISTANCE + .1) {
				++result;
			}
		}
		return result;
	}

	public static boolean isPositionVisible(XXX_Point position, double additionalDistance, Wizard[] wizards, Minion[] minions, Building[] buildings) {
		for (Wizard unit : wizards) {
			if (unit.getFaction() != XXX_Constants.getCurrentFaction()) {
				continue;
			}
			if (XXX_FastMath.hypot(position.getX() - unit.getX(), position.getY() - unit.getY()) + additionalDistance < unit.getVisionRange()) {
				return true;
			}
		}
		for (Minion unit : minions) {
			if (unit.getFaction() != XXX_Constants.getCurrentFaction()) {
				continue;
			}
			if (XXX_FastMath.hypot(position.getX() - unit.getX(), position.getY() - unit.getY()) + additionalDistance < unit.getVisionRange()) {
				return true;
			}
		}
		if (buildings != null) {
			for (Building unit : buildings) {
				if (unit.getFaction() != XXX_Constants.getCurrentFaction()) {
					continue;
				}
				if (XXX_FastMath.hypot(position.getX() - unit.getX(), position.getY() - unit.getY()) + additionalDistance < unit.getVisionRange()) {
					return true;
				}
			}
		}
		return false;
	}

	public static double cooldownDistanceCalculation(double baseDistance, int coolDownRemaining) {
		return baseDistance + Math.min(1, -coolDownRemaining + 3) * XXX_Constants.getGame().getWizardBackwardSpeed() * XXX_Variables.moveFactor * .66;
	}

	public static int getTicksToBonusSpawn(int tickNo) {
		if (tickNo > 17501) {
			return 20000;
		}
		return 2500 - (tickNo - 1) % 2500;
	}

	private static int score[] = new int[3];

	public static XXX_BaseLine fightLineSelect(XXX_BaseLine previousLine, World world, XXX_EnemyPositionCalc enemyPositionCalc, Wizard self) {
		for (int i = 0; i != 3; ++i) {
			int currScore = enemyPositionCalc.getMinionsOnLine()[i];
			if (currScore < 4) {
				score[i] = XXX_Constants.minionLineScore * 4;
			} else {
				score[i] = XXX_Constants.minionLineScore * currScore;
			}
		}
		for (Building building : world.getBuildings()) {
			if (building.getType() == BuildingType.FACTION_BASE || building.getFaction() != XXX_Constants.getCurrentFaction()) {
				continue;
			}
			int line = XXX_Utils.whichLine(building);
			score[line] -= XXX_Constants.towerLineScore;
		}
		for (XXX_BuildingPhantom buildingPhantom : enemyPositionCalc.getBuildingPhantoms()) {
			score[XXX_Utils.whichLine(buildingPhantom)] += XXX_Constants.towerLineScore;
		}
		for (XXX_WizardPhantom wizard : enemyPositionCalc.getDetectedWizards().values()) {
			int line = XXX_Utils.whichLine(wizard);
			score[line] += XXX_Constants.enemyWizardLineScore;
		}
		for (Wizard wizard : world.getWizards()) {
			if (wizard.isMe() || wizard.getFaction() == XXX_Constants.getEnemyFaction()) {
				continue;
			}
			int line = XXX_Utils.whichLine(wizard);
			if (wizard.getFaction() == XXX_Constants.getCurrentFaction()) {
				score[line] *= XXX_Constants.wizardLineMult;
			}
		}
		int maxValue = 0;
		for (int i = 0; i != 3; ++i) {
			double distancePenalty = XXX_FastMath.hypot(self.getX() - XXX_Constants.getLines()[i].getPreFightPoint().getX(),
														self.getY() - XXX_Constants.getLines()[i].getPreFightPoint().getY());
			distancePenalty = 1 / Math.max(1, distancePenalty / 700);
			score[i] *= distancePenalty;
			if (XXX_Constants.getLines()[i] == previousLine) {
				score[i] *= XXX_Constants.CURRENT_LINE_PRIORITY;
			}
			if (score[maxValue] < score[i]) {
				maxValue = i;
			}
		}
		return XXX_Constants.getLines()[maxValue];
	}

	public static double getDistanceToNearestAlly(Unit unit, XXX_FilteredWorld filteredWorld, double startDistance) {
		for (LivingUnit livingUnit : filteredWorld.getMinions()) {
			if (livingUnit.getFaction() != XXX_Constants.getCurrentFaction()) {
				continue;
			}
			startDistance = Math.min(startDistance, XXX_FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()));
		}
		for (LivingUnit livingUnit : filteredWorld.getBuildings()) {
			if (livingUnit.getFaction() != XXX_Constants.getCurrentFaction()) {
				continue;
			}
			startDistance = Math.min(startDistance, XXX_FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()));
		}
		for (Wizard livingUnit : filteredWorld.getWizards()) {
			if (livingUnit.getFaction() != XXX_Constants.getCurrentFaction() || livingUnit.isMe()) {
				continue;
			}
			startDistance = Math.min(startDistance, XXX_FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()));
		}
		return startDistance;
	}

	public static int getPrefferedUnitsCountInRange(Unit unit, XXX_FilteredWorld filteredWorld, double distance, int damage, int life) {
		int cnt = 0;
		for (LivingUnit livingUnit : filteredWorld.getMinions()) {
			if (livingUnit.getFaction() != XXX_Constants.getCurrentFaction()) {
				continue;
			}
			if (livingUnit.getLife() < damage || livingUnit.getLife() >= life) {
				continue;
			}
			if (XXX_FastMath.hypot(unit, livingUnit) < distance) {
				++cnt;
			}
		}
		for (Wizard livingUnit : filteredWorld.getWizards()) {
			if (livingUnit.getFaction() != XXX_Constants.getCurrentFaction() || livingUnit.isMe()) {
				continue;
			}
			if (livingUnit.getLife() < damage || livingUnit.getLife() >= life) {
				continue;
			}
			if (XXX_FastMath.hypot(unit, livingUnit) < distance) {
				++cnt;
			}
		}
		return cnt;
	}

	public static boolean hasAllyNearby(Unit unit, World filteredWorld, double checkDistance) {
		for (LivingUnit livingUnit : filteredWorld.getMinions()) {
			if (livingUnit.getFaction() != XXX_Constants.getCurrentFaction()) {
				continue;
			}
			if (XXX_FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()) < checkDistance) {
				return true;
			}
		}
		for (Wizard livingUnit : filteredWorld.getWizards()) {
			if (livingUnit.getFaction() != XXX_Constants.getCurrentFaction() || livingUnit.isMe()) {
				continue;
			}
			if (XXX_FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()) < checkDistance) {
				return true;
			}
		}
		return false;
	}

	public static boolean isUnitActive(Minion previuosPosition, Minion newPosition) {
		return previuosPosition.getX() != newPosition.getX() || previuosPosition.getY() != newPosition.getY() || previuosPosition.getAngle() != newPosition.getAngle() || newPosition.getLife() != newPosition.getMaxLife() || newPosition.getRemainingActionCooldownTicks() != 0;
	}
}