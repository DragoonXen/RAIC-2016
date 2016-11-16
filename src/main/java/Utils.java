import model.Building;
import model.CircularUnit;
import model.LaneType;
import model.LivingUnit;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by dragoon on 11/9/16.
 */
public class Utils {

	public final static Comparator<Map.Entry<Double, CircularUnit>> AIM_SORT_COMPARATOR = (o1, o2) -> o2.getKey().compareTo(o1.getKey());

	private static double[] lineDistance = new double[Constants.getLines().length];

	public static int whichLine(Unit unit) {
		int min = 0;
		lineDistance[0] = Constants.getLines()[0].getDistanceTo(unit);
		for (int i = 1; i != Constants.getLines().length; ++i) {
			lineDistance[i] = Constants.getLines()[i].getDistanceTo(unit);
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

	public static FilteredWorld filterWorld(World world, Point point) {
		return new FilteredWorld(
				world.getTickIndex(),
				world.getTickCount(),
				world.getWidth(),
				world.getHeight(),
				world.getPlayers(),
				removeMe(filterUnit(world.getWizards(), point, FilteredWorld.FilterType.FIGHT)),
				filterUnit(world.getMinions(), point, FilteredWorld.FilterType.FIGHT),
				filterUnit(world.getProjectiles(), point, FilteredWorld.FilterType.FIGHT),
				filterUnit(world.getBonuses(), point, FilteredWorld.FilterType.FIGHT),
				filterUnit(world.getBuildings(), point, FilteredWorld.FilterType.FIGHT),
				filterUnit(world.getTrees(), point, FilteredWorld.FilterType.MOVE),
				point);
	}

	public static <T extends CircularUnit> List<T> filterUnit(T[] units, Point point, FilteredWorld.FilterType filterType) {
		List<T> unitsList = new ArrayList<>();
		if (units.length == 0) {
			return unitsList;
		}
		double distance = 0.;
		boolean isBuilding = false;
		switch (filterType) {
			case FIGHT:
				distance = Constants.getFightDistanceFilter();
				break;
			case MOVE:
				distance = Constants.MOVE_DISTANCE_FILTER + Constants.getGame().getWizardRadius();
				break;
			case AIM:
				distance = Variables.self.getCastRange();
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

	public static double getDirectionTo(Unit unit, Point point) {
		return Math.atan2(point.getY() - unit.getY(), point.getX() - unit.getX());
	}

	public static ScanMatrixItem[][] createScanMatrix() {
		ScanMatrixItem[][] matrix = new ScanMatrixItem[(int) Math.round((Constants.MOVE_FWD_DISTANCE + Constants.MOVE_BACK_DISTANCE) / Constants.MOVE_SCAN_STEP + 1.01)]
				[(int) Math.round((Constants.MOVE_SIDE_DISTANCE + Constants.MOVE_SIDE_DISTANCE) / Constants.MOVE_SCAN_STEP + 1.01)];

		for (int i = 0; i != matrix.length; ++i) {
			for (int j = 0; j != matrix[0].length; ++j) {
				matrix[i][j] = new ScanMatrixItem(i, j);
			}
		}

		int[] di = new int[]{1, 0, 0, -1};//, 1, 1, -1, -1};
		int[] dj = new int[]{0, 1, -1, 0};//, 1, -1, -1, 1};
//		double diagonalDistance = Math.sqrt(Constants.MOVE_SCAN_STEP * Constants.MOVE_SCAN_STEP);
		for (int i = 0; i != matrix.length; ++i) {
			for (int j = 0; j != matrix[0].length; ++j) {
//				List<Double> neighbourDistances = new ArrayList<>();
				List<ScanMatrixItem> neighbour = new ArrayList<>();
				for (int k = 0; k != 4; ++k) {
					int nx = i + di[k];
					int ny = j + dj[k];
					if (nx < 0 || nx >= matrix.length || ny < 0 || ny >= matrix[0].length) {
						continue;
					}
//					neighbourDistances.add(Constants.MOVE_SCAN_STEP);
					neighbour.add(matrix[nx][ny]);
				}
//				double[] distances = new double[neighbourDistances.size()];
//				for (int k = 0; k != neighbourDistances.size(); ++k) {
//					distances[k] = neighbourDistances.get(k);
//				}

				matrix[i][j].setNeighbours(neighbour.toArray(new ScanMatrixItem[neighbour.size()]));
			}
		}

		List<Map.Entry<Double, ScanMatrixItem>> pointsMap = new ArrayList<>();
		for (int i = 0; i != matrix.length; ++i) {
			for (int j = 0; j != matrix[0].length; ++j) {
				pointsMap.add(new AbstractMap.SimpleEntry<>(FastMath.hypot(Constants.CURRENT_PT_X - i,
																		   Constants.CURRENT_PT_Y - j),
															matrix[i][j]));
			}
		}
		Collections.sort(pointsMap, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
		int i = 0;
		for (Map.Entry<Double, ScanMatrixItem> doubleScanMatrixItemEntry : pointsMap) {
			ScanMatrixItem item = doubleScanMatrixItemEntry.getValue();
			item.setDistanceFromSelf(doubleScanMatrixItemEntry.getKey());
		}

		return matrix;
	}

	public static double distancePointToSegment(Point point, Point segA, Point segB) {
		return point.norm(nearestSegmentPoint(point, segA, segB));
	}

	public static Point nearestSegmentPoint(Point point, Point segA, Point segB) {
		Point vectorV = segA.negate(segB);
		Point vectorW = point.negate(segB);

		double c1 = vectorV.scalarMult(vectorW);
		if (c1 <= 0)
			return segB;

		double c2 = vectorV.scalarMult(vectorV);
		if (c2 <= c1)
			return segA;

		return segB.add(vectorV.mult(c1 / c2));
	}

	private static CircularUnit blockUnit;

	public static void prepareNewStep() {
		blockUnit = null;
	}

	public static boolean isAvailableTile(List<CircularUnit> units, double x, double y) {
		double radius = Constants.getGame().getWizardRadius();
		if (x < radius ||
				y < radius ||
				x + radius > Constants.getGame().getMapSize() ||
				y + radius > Constants.getGame().getMapSize()) {
			return false;
		}
		if (blockUnit != null) {
			if (FastMath.hypot(blockUnit.getX() - x, blockUnit.getY() - y) + .001 < blockUnit.getRadius() + radius) {
				return false;
			}
		}
		Iterator<CircularUnit> iterator = units.iterator();
		while (iterator.hasNext()) {
			CircularUnit unit = iterator.next();
			if (FastMath.hypot(unit.getX() - x, unit.getY() - y) < unit.getRadius() + radius) {
				blockUnit = unit;
				return false;
			}
		}

		blockUnit = null;
		return true;
	}

	public static void calcTilesAvailable(List<CircularUnit> units, ScanMatrixItem[] items) {
		double radius = Constants.getGame().getWizardRadius();
		double x, y;
		List<CircularUnit> filteredUnits = new ArrayList<>(units.size());
		for (CircularUnit unit : units) {
			x = Utils.distancePointToSegment(new Point(unit.getX(), unit.getY()), items[0], items[items.length - 1]);
			if (x < unit.getRadius() + radius) {
				filteredUnits.add(unit);
			}
		}
		for (int i = 0; i != items.length; ++i) {
			ScanMatrixItem item = items[i];
			x = item.getX();
			y = item.getY();
			if (x < radius ||
					y < radius ||
					x + radius > Constants.getGame().getMapSize() ||
					y + radius > Constants.getGame().getMapSize()) {
				item.setAvailable(false);
				continue;
			}
			if (blockUnit != null) {
				if (FastMath.hypot(blockUnit.getX() - x, blockUnit.getY() - y) + .001 < blockUnit.getRadius() + radius) {
					item.setAvailable(false);
					continue;
				}
			}
			blockUnit = null;
			for (CircularUnit unit : filteredUnits) {
				if (FastMath.hypot(unit.getX() - x, unit.getY() - y) + .001 < unit.getRadius() + radius) {
					item.setAvailable(false);
					blockUnit = unit;
					break;
				}
			}
		}
	}

	public static double calcLineDistanceOtherDanger(Unit unit, BaseLine myLineCalc) {
		return calcLineDistanceOtherDanger(new Point(unit.getX(), unit.getY()), myLineCalc);
	}

	public static double calcLineDistanceOtherDanger(Point point, BaseLine myLineCalc) {
		double distanceTo = myLineCalc.getDistanceTo(point.getX(), point.getY()) -
				Constants.getTopLine().getLineDistance();
		if (distanceTo > 0.) {
			distanceTo /= Constants.getTopLine().getLineDistance();
			return distanceTo * distanceTo * distanceTo;
		}
		return 0.;
	}

	public static boolean hasEnemy(LivingUnit[] units) {
		for (LivingUnit unit : units) {
			if (unit.getFaction() == Constants.getEnemyFaction() ||
					unit.getFaction() == Constants.getEnemyFaction() && unit.getLife() < unit.getMaxLife()) {
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

	public static void calcCurrentSkillBonuses(Wizard self, FilteredWorld filteredWorld) {
		Arrays.fill(skillsCount, 0);
		Arrays.fill(aurasCount, 0);
		for (SkillType skillType : self.getSkills()) {
			switch (skillType) {
				case RANGE_BONUS_PASSIVE_1:
					skillsCount[SkillFork.RANGE.ordinal()] = Math.max(skillsCount[SkillFork.RANGE.ordinal()], 1);
					break;
				case RANGE_BONUS_AURA_1:
					aurasCount[SkillFork.RANGE.ordinal()] = Math.max(aurasCount[SkillFork.RANGE.ordinal()], 1);
					break;
				case RANGE_BONUS_PASSIVE_2:
					skillsCount[SkillFork.RANGE.ordinal()] = 2;
					break;
				case RANGE_BONUS_AURA_2:
					aurasCount[SkillFork.RANGE.ordinal()] = 2;
					break;

				case MAGICAL_DAMAGE_BONUS_PASSIVE_1:
					skillsCount[SkillFork.MAGICAL_DAMAGE.ordinal()] = Math.max(skillsCount[SkillFork.MAGICAL_DAMAGE.ordinal()], 1);
					break;
				case MAGICAL_DAMAGE_BONUS_AURA_1:
					aurasCount[SkillFork.MAGICAL_DAMAGE.ordinal()] = Math.max(aurasCount[SkillFork.MAGICAL_DAMAGE.ordinal()], 1);
					break;
				case MAGICAL_DAMAGE_BONUS_PASSIVE_2:
					skillsCount[SkillFork.MAGICAL_DAMAGE.ordinal()] = 2;
					break;
				case MAGICAL_DAMAGE_BONUS_AURA_2:
					aurasCount[SkillFork.MAGICAL_DAMAGE.ordinal()] = 2;
					break;

				case STAFF_DAMAGE_BONUS_PASSIVE_1:
					skillsCount[SkillFork.STAFF_DAMAGE.ordinal()] = Math.max(skillsCount[SkillFork.STAFF_DAMAGE.ordinal()], 1);
					break;
				case STAFF_DAMAGE_BONUS_AURA_1:
					aurasCount[SkillFork.STAFF_DAMAGE.ordinal()] = Math.max(aurasCount[SkillFork.STAFF_DAMAGE.ordinal()], 1);
					break;
				case STAFF_DAMAGE_BONUS_PASSIVE_2:
					skillsCount[SkillFork.STAFF_DAMAGE.ordinal()] = 2;
					break;
				case STAFF_DAMAGE_BONUS_AURA_2:
					aurasCount[SkillFork.STAFF_DAMAGE.ordinal()] = 2;
					break;

				case MOVEMENT_BONUS_FACTOR_PASSIVE_1:
					skillsCount[SkillFork.MOVEMENT.ordinal()] = Math.max(skillsCount[SkillFork.MOVEMENT.ordinal()], 1);
					break;
				case MOVEMENT_BONUS_FACTOR_AURA_1:
					aurasCount[SkillFork.MOVEMENT.ordinal()] = Math.max(aurasCount[SkillFork.MOVEMENT.ordinal()], 1);
					break;
				case MOVEMENT_BONUS_FACTOR_PASSIVE_2:
					skillsCount[SkillFork.MOVEMENT.ordinal()] = 2;
					break;
				case MOVEMENT_BONUS_FACTOR_AURA_2:
					aurasCount[SkillFork.MOVEMENT.ordinal()] = 2;
					break;

				case MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1:
					skillsCount[SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = Math.max(skillsCount[SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()], 1);
					break;
				case MAGICAL_DAMAGE_ABSORPTION_AURA_1:
					aurasCount[SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = Math.max(aurasCount[SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()], 1);
					break;
				case MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2:
					skillsCount[SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = 2;
					break;
				case MAGICAL_DAMAGE_ABSORPTION_AURA_2:
					aurasCount[SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = 2;
					break;
			}
		}
		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() != Constants.getCurrentFaction()) {
				continue;
			}
			if (wizard.getDistanceTo(self) < Constants.getGame().getAuraSkillRange()) {
				for (SkillType skillType : wizard.getSkills()) {
					switch (skillType) {
						case RANGE_BONUS_AURA_1:
							aurasCount[SkillFork.RANGE.ordinal()] = Math.max(aurasCount[SkillFork.RANGE.ordinal()], 1);
							break;
						case RANGE_BONUS_AURA_2:
							aurasCount[SkillFork.RANGE.ordinal()] = 2;
							break;

						case MAGICAL_DAMAGE_BONUS_AURA_1:
							aurasCount[SkillFork.MAGICAL_DAMAGE.ordinal()] = Math.max(aurasCount[SkillFork.MAGICAL_DAMAGE.ordinal()], 1);
							break;
						case MAGICAL_DAMAGE_BONUS_AURA_2:
							aurasCount[SkillFork.MAGICAL_DAMAGE.ordinal()] = 2;
							break;

						case STAFF_DAMAGE_BONUS_AURA_1:
							aurasCount[SkillFork.STAFF_DAMAGE.ordinal()] = Math.max(aurasCount[SkillFork.STAFF_DAMAGE.ordinal()], 1);
							break;
						case STAFF_DAMAGE_BONUS_AURA_2:
							aurasCount[SkillFork.STAFF_DAMAGE.ordinal()] = 2;
							break;

						case MOVEMENT_BONUS_FACTOR_AURA_1:
							aurasCount[SkillFork.MOVEMENT.ordinal()] = Math.max(aurasCount[SkillFork.MOVEMENT.ordinal()], 1);
							break;
						case MOVEMENT_BONUS_FACTOR_AURA_2:
							aurasCount[SkillFork.MOVEMENT.ordinal()] = 2;
							break;

						case MAGICAL_DAMAGE_ABSORPTION_AURA_1:
							aurasCount[SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = Math.max(aurasCount[SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()], 1);
							break;
						case MAGICAL_DAMAGE_ABSORPTION_AURA_2:
							aurasCount[SkillFork.MAGICAL_DAMAGE_ABSORPTION.ordinal()] = 2;
							break;
					}
				}
			}
		}

		Variables.turnFactor = 1.;
		Variables.moveFactor = 1. + Constants.getGame().getMovementBonusFactorPerSkillLevel() *
				(aurasCount[SkillFork.MOVEMENT.ordinal()] + skillsCount[SkillFork.MOVEMENT.ordinal()]);

		if (Utils.wizardHasStatus(self, StatusType.HASTENED)) {
			Variables.turnFactor += Constants.getGame().getHastenedRotationBonusFactor();
			Variables.moveFactor += Constants.getGame().getHastenedMovementBonusFactor();
		}
	}
}
