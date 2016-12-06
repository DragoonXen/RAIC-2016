import model.ActionType;
import model.Bonus;
import model.Building;
import model.CircularUnit;
import model.Faction;
import model.LaneType;
import model.LivingUnit;
import model.Minion;
import model.MinionType;
import model.Projectile;
import model.ProjectileType;
import model.StatusType;
import model.Tree;
import model.Unit;
import model.Wizard;
import model.World;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by dragoon on 11/9/16.
 */
public class Utils {

	public final static Comparator<Pair<Double, CircularUnit>> AIM_SORT_COMPARATOR = (o1, o2) -> o2.getFirst().compareTo(o1.getFirst());
	public final static Comparator<Pair<Double, Point>> POINT_AIM_SORT_COMPARATOR = (o1, o2) -> o2.getFirst().compareTo(o1.getFirst());

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

	public static int whichLine(Point point) {
		int min = 0;
		lineDistance[0] = Constants.getLines()[0].getDistanceTo(point.getX(), point.getY());
		for (int i = 1; i != Constants.getLines().length; ++i) {
			lineDistance[i] = Constants.getLines()[i].getDistanceTo(point.getX(), point.getY());
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

	public static FilteredWorld filterWorld(World world, Point point, BuildingPhantom[] buildings, TeammateIdsContainer teammateIdsContainer) {
		List<Projectile> projectiles = new ArrayList<>();
		for (Projectile projectile : world.getProjectiles()) {
			if (projectile.getOwnerUnitId() == Variables.self.getId() || teammateIdsContainer.isTeammate(projectile.getOwnerUnitId())) {
				continue;
			}
			projectiles.add(projectile);
		}
		Projectile[] filteredProjectiles = new Projectile[projectiles.size()];
		projectiles.toArray(filteredProjectiles);

		return new FilteredWorld(
				world.getTickIndex(),
				world.getTickCount(),
				world.getWidth(),
				world.getHeight(),
				world.getPlayers(),
				removeMe(filterUnit(world.getWizards(), point, FilteredWorld.FilterType.FIGHT)),
				filterUnit(world.getMinions(), point, FilteredWorld.FilterType.FIGHT),
				filterUnit(filteredProjectiles, point, FilteredWorld.FilterType.FIGHT),
				filterUnit(world.getBonuses(), point, FilteredWorld.FilterType.FIGHT),
				filterUnit(buildings, point, FilteredWorld.FilterType.FIGHT),
				filterUnit(world.getTrees(), point, FilteredWorld.FilterType.MOVE),
				filterUnit(world.getTrees(), point, FilteredWorld.FilterType.AIM_OBSTACLE),
				point);
	}

	public static <T extends CircularUnit> List<T> filterUnit(T[] units, Point point, FilteredWorld.FilterType filterType) {
		List<T> unitsList = new ArrayList<>();
		if (units.length == 0) {
			return unitsList;
		}
		double distance = 0.;
		switch (filterType) {
			case FIGHT:
				distance = Constants.getFightDistanceFilter();
				break;
			case MOVE:
				distance = Constants.MOVE_DISTANCE_FILTER + Constants.getGame().getWizardRadius();
				break;
			case AIM:
			case AIM_OBSTACLE:
				distance = Variables.self.getCastRange();
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
				for (T unit : units) {
					if (unit.getDistanceTo(point.getX(), point.getY()) - unit.getRadius() + 50 <= distance) {
						unitsList.add(unit);
					}
				}
				break;
			case AIM_OBSTACLE:
				for (T unit : units) {
					if (unit.getDistanceTo(point.getX(), point.getY()) <= distance) {
						unitsList.add(unit);
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

		double maxBonus = Math.pow(FastMath.hypot(-matrix.length - 30, -Constants.CURRENT_PT_Y), Constants.FORWARD_MOVE_FROM_DISTANCE_POWER);
		for (int i = 0; i != matrix.length; ++i) {
			for (int j = 0; j != matrix[0].length; ++j) {
				matrix[i][j] = new ScanMatrixItem(i,
												  j,
												  maxBonus - Math.pow(FastMath.hypot(i - matrix.length - 30, j - Constants.CURRENT_PT_Y),
																	  Constants.FORWARD_MOVE_FROM_DISTANCE_POWER));
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
		for (Map.Entry<Double, ScanMatrixItem> doubleScanMatrixItemEntry : pointsMap) {
			ScanMatrixItem item = doubleScanMatrixItemEntry.getValue();
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

	public static double distancePointToSegment(Point point, Point segA, Point segB) {
		return point.segmentNorm(nearestSegmentPoint(point, segA, segB));
	}

	public static Point nearestSegmentPoint(Point point, Point segA, Point segB) {
		Point vectorV = segA.negateCopy(segB);
		Point vectorW = point.negateCopy(segB);

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
		double radius = Constants.getGame().getWizardRadius();
		if (x < radius ||
				y < radius ||
				x + radius > Constants.getGame().getMapSize() ||
				y + radius > Constants.getGame().getMapSize()) {
			return false;
		}
		Iterator<CircularUnit> iterator = units.iterator();
		while (iterator.hasNext()) {
			CircularUnit unit = iterator.next();
			if (FastMath.hypot(unit.getX() - x, unit.getY() - y) < unit.getRadius() + radius + Constants.STUCK_FIX_RADIUS_ADD) {
				return false;
			}
		}
		return true;
	}

	public static void calcTilesAvailable(List<CircularUnit> units, ScanMatrixItem[] items) {
		double radius = Constants.getGame().getWizardRadius();
		double x, y;
		List<CircularUnit> filteredUnits = new ArrayList<>(units.size());
		for (CircularUnit unit : units) {
			x = Utils.distancePointToSegment(new Point(unit.getX(), unit.getY()), items[0], items[items.length - 1]);
			if (x < unit.getRadius() + radius + Constants.STUCK_FIX_RADIUS_ADD) {
				filteredUnits.add(unit);
			}
		}
		blockUnit = null;
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
				if (FastMath.hypot(blockUnit.getX() - x, blockUnit.getY() - y) < blockUnit.getRadius() + radius + Constants.STUCK_FIX_RADIUS_ADD) {
					item.setAvailable(false);
					continue;
				}
				filteredUnits.remove(blockUnit);
				blockUnit = null;
			}
			for (CircularUnit unit : filteredUnits) {
				if (FastMath.hypot(unit.getX() - x, unit.getY() - y) < unit.getRadius() + radius + Constants.STUCK_FIX_RADIUS_ADD) {
					item.setAvailable(false);
					blockUnit = unit;
					break;
				}
			}
		}
	}

	public static void calcTileScore(ScanMatrixItem item,
									 FilteredWorld filteredWorld,
									 BaseLine myLineCalc,
									 Wizard self,
									 UnitScoreCalculation unitScoreCalculation,
									 FightStatus fightStatus) {
		item.setAvailable(Utils.isAvailableTile(filteredWorld.getAllBlocksList(), item.getX(), item.getY()));
		if (!item.isAvailable()) {
			return;
		}
		double distanceTo = myLineCalc.calcLineDistanceOtherDanger(item);
		if (distanceTo > 0.) {
			item.addOtherDanger(distanceTo);
		}

		for (Bonus bonus : filteredWorld.getBonuses()) {
			if (FastMath.hypot(self, bonus) > Constants.getFightDistanceFilter()) {
				continue;
			}
			ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(bonus.getId());
			structure.applyScores(item, FastMath.hypot(bonus, item));
		}

		if (Utils.getTicksToBonusSpawn(filteredWorld.getTickIndex()) < 250) {
			for (int i = 0; i != BonusesPossibilityCalcs.BONUSES_POINTS.length; ++i) {
				if (FastMath.hypot(self, BonusesPossibilityCalcs.BONUSES_POINTS[i]) > Constants.getFightDistanceFilter()) {
					continue;
				}
				ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(i - 5);
				structure.applyScores(item, FastMath.hypot(BonusesPossibilityCalcs.BONUSES_POINTS[i], item));
			}
		}

		if (fightStatus == FightStatus.NO_ENEMY) {
			return;
		}

		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}
			ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(minion.getId());
			structure.applyScores(item, FastMath.hypot(minion, item));
		}

		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}

			ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(wizard.getId());
			structure.applyScores(item, FastMath.hypot(wizard, item));
		}

		for (Building building : filteredWorld.getBuildings()) {
			if (building.getFaction() == Constants.getCurrentFaction()) {
				continue;
			}

			ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(building.getId());
			structure.applyScores(item, FastMath.hypot(building.getX() - item.getX(), building.getY() - item.getY()));
		}
	}

	public static boolean hasEnemy(Minion[] units, AgressiveNeutralsCalcs agressiveNeutralsCalcs) {
		for (LivingUnit unit : units) {
			if (unit.getFaction() == Constants.getEnemyFaction() ||
					unit.getFaction() == Faction.NEUTRAL && agressiveNeutralsCalcs.isMinionAgressive(unit.getId())) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasEnemy(LivingUnit[] units) {
		for (LivingUnit unit : units) {
			if (unit.getFaction() == Constants.getEnemyFaction()) {
				return true;
			}
		}
		return false;
	}

	public static boolean wizardHasStatus(Wizard wizard, StatusType statusType) {
		return wizardStatusTicks(wizard, statusType) != 0;
	}

	public static int wizardStatusTicks(Wizard wizard, StatusType statusType) {
		WizardsInfo.WizardInfo wizardInfo = Variables.wizardsInfo.getWizardInfo(wizard.getId());
		switch (statusType) {
			case BURNING:
				return 0;
			case EMPOWERED:
				return wizardInfo.getEmpowered();
			case FROZEN:
				return wizardInfo.getFrozen();
			case HASTENED:
				return wizardInfo.getHastened();
			case SHIELDED:
				return wizardInfo.getShielded();
		}
		return 0;
	}

	public final static double[] PROJECTIVE_SPEED = new double[]
			{Constants.getGame().getMagicMissileSpeed(),
					Constants.getGame().getFrostBoltSpeed(),
					Constants.getGame().getFireballSpeed(),
					Constants.getGame().getDartSpeed()};

	public final static int[] PROJECTIVE_DAMAGE = new int[]
			{Constants.getGame().getMagicMissileDirectDamage(),
					Constants.getGame().getFrostBoltDirectDamage(),
					Constants.getGame().getBurningSummaryDamage(),
					Constants.getGame().getDartDirectDamage()};

	public final static double[] PROJECTIVE_RADIUS = new double[]
			{Constants.getGame().getMagicMissileRadius(),
					Constants.getGame().getFrostBoltRadius(),
					Constants.getGame().getFireballRadius(),
					Constants.getGame().getDartRadius()};

	public static int getProjectileDamage(Projectile projectile, double distance) {
		int damage = PROJECTIVE_DAMAGE[projectile.getType().ordinal()];
		if (projectile.getType() == ProjectileType.FIREBALL) {
			if (distance < Constants.getGame().getFireballExplosionMaxDamageRange()) {
				return damage + Constants.getGame().getFireballExplosionMaxDamage();
			} else {
				return damage + Constants.getGame().getFireballExplosionMinDamage();
			}
		}
		return damage;
	}

	public static int getIntDamage(double damage) {
		return (int) Math.floor(damage);
	}

	public static void fillProjectilesSim(FilteredWorld filteredWorld, HashMap<Long, Double> projectilesDTL) {
		Variables.projectilesSim.clear();
		for (Projectile projectile : filteredWorld.getProjectiles()) {
			Variables.projectilesSim.add(new AbstractMap.SimpleEntry<Projectile, Double>(projectile, projectilesDTL.get(projectile.getId())));
		}
	}

	public static double checkProjectiveCollision(Point point, int ticks) {
		double selfRadius = Variables.self.getRadius();
		double damage = 0.;
		Iterator<AbstractMap.SimpleEntry<Projectile, Double>> iterator = Variables.projectilesSim.iterator();
		Variables.fireballHitDamageCheck.clear();
		while (iterator.hasNext()) {
			AbstractMap.SimpleEntry<Projectile, Double> next = iterator.next();
			Projectile projectile = next.getKey();
			Point projectileVector = new Point(projectile.getSpeedX(), projectile.getSpeedY());
			Point startProjectilePoint = new Point(projectile.getX() + projectile.getSpeedX() * ticks, projectile.getY() + projectile.getSpeedY() * ticks);
			boolean remove = false;
			if (projectileVector.vectorNorm() >= next.getValue() - .0001) {
				projectileVector.fixVectorLength(next.getValue());
				remove = true;
			} else {
				next.setValue(next.getValue() - Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()]);
			}
			projectileVector.add(startProjectilePoint);
			double projectiveRadius = projectile.getRadius();
			if (projectile.getType() == ProjectileType.FIREBALL) {
				projectiveRadius = Constants.getGame().getFireballExplosionMinDamageRange();
			}
			double distanceToProjective = Utils.distancePointToSegment(point, startProjectilePoint, projectileVector);
			if (distanceToProjective < projectiveRadius + selfRadius + .001) {
				if (projectile.getType() == ProjectileType.FIREBALL) {
					if (distanceToProjective + selfRadius < Constants.getGame().getFireballExplosionMaxDamageRange()) {
						damage += getProjectileDamage(projectile, distanceToProjective);
						remove = true;
						Variables.fireballHitDamageCheck.remove(projectile.getId());
					} else {
						Variables.fireballHitDamageCheck.add(projectile.getId());
					}
				} else {
					damage += getProjectileDamage(projectile, distanceToProjective);
					remove = true;
				}
			}
			if (remove) {
				iterator.remove();
			}
		}
		damage += Variables.fireballHitDamageCheck.size() * (Constants.getGame().getBurningSummaryDamage() + Constants.getGame().getFireballExplosionMinDamage());
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
			if (FastMath.hypot(unit.getX() - unitCountFor.getX(), unit.getY() - unitCountFor.getY()) < distance) {
				++result;
			}
		}
		return result;
	}

	public static int unitsCountCloseToDestination(LivingUnit[] units, Point destination) {
		if (destination == null) {
			return 0;
		}
		int result = 0;
		// modify both this and bottom functions
		for (LivingUnit unit : units) {
			if (FastMath.hypot(unit.getX() - destination.getX(), unit.getY() - destination.getY()) <
					Constants.getGame().getWizardRadius() +
							unit.getRadius() +
							Constants.MOVE_SCAN_DIAGONAL_DISTANCE + .1) {
				++result;
			}
		}
		return result;
	}

	public static int unitsCountCloseToDestination(List<CircularUnit> units, Point destination) {
		if (destination == null) {
			return 0;
		}
		int result = 0;
		// modify both this and upper functions
		for (CircularUnit unit : units) {
			if (FastMath.hypot(unit.getX() - destination.getX(), unit.getY() - destination.getY()) <
					Constants.getGame().getWizardRadius() +
							unit.getRadius() +
							Constants.MOVE_SCAN_DIAGONAL_DISTANCE + .1) {
				++result;
			}
		}
		return result;
	}

	public static boolean isPositionVisible(Point position, double additionalDistance, Wizard[] wizards, Minion[] minions, Building[] buildings) {
		for (Wizard unit : wizards) {
			if (unit.getFaction() != Constants.getCurrentFaction()) {
				continue;
			}
			if (FastMath.hypot(position.getX() - unit.getX(), position.getY() - unit.getY()) + additionalDistance < unit.getVisionRange()) {
				return true;
			}
		}
		for (Minion unit : minions) {
			if (unit.getFaction() != Constants.getCurrentFaction()) {
				continue;
			}
			if (FastMath.hypot(position.getX() - unit.getX(), position.getY() - unit.getY()) + additionalDistance < unit.getVisionRange()) {
				return true;
			}
		}
		if (buildings != null) {
			for (Building unit : buildings) {
				if (unit.getFaction() != Constants.getCurrentFaction()) {
					continue;
				}
				if (FastMath.hypot(position.getX() - unit.getX(), position.getY() - unit.getY()) + additionalDistance < unit.getVisionRange()) {
					return true;
				}
			}
		}
		return false;
	}

	public static double cooldownDistanceMinionsCalculation(double baseDistance, int coolDownRemaining) {
		return baseDistance + Math.min(1, -coolDownRemaining + 3) *
				Constants.getGame().getWizardBackwardSpeed() *
				Variables.wizardsInfo.getMe().getMoveFactor() *
				.66;
	}

	public static double cooldownDistanceWizardCalculation(double moveFactor, int coolDownRemaining) {
		return Math.min(2, -coolDownRemaining + 4) *
				Constants.getGame().getWizardForwardSpeed() *
				moveFactor * .6;
	}

	public static int getTicksToBonusSpawn(int tickNo) {
		if (tickNo > 17501) {
			return 20000;
		}
		return 2500 - (tickNo - 1) % 2500;
	}

	private static int score[] = new int[3];

	public static BaseLine fightLineSelect(BaseLine previousLine, World world, EnemyPositionCalc enemyPositionCalc, Wizard self) {
		for (int i = 0; i != 3; ++i) {
			int currScore = enemyPositionCalc.getMinionsOnLine()[i];
			if (currScore < 4) {
				score[i] = Constants.minionLineScore * 4;
			} else {
				score[i] = Constants.minionLineScore * currScore;
			}
		}

		for (WizardPhantom wizard : enemyPositionCalc.getDetectedWizards().values()) {
			int line = Utils.whichLine(wizard);
			score[line] += Constants.enemyWizardLineScore;
		}

		for (Wizard wizard : world.getWizards()) {
			if (wizard.isMe() || wizard.getFaction() == Constants.getEnemyFaction()) {
				continue;
			}
			int line = Utils.whichLine(wizard);
			if (wizard.getFaction() == Constants.getCurrentFaction()) {
				score[line] *= Constants.wizardLineMult;
			}
		}
		int maxValue = 0;
		for (int i = 0; i != 3; ++i) {
			double distancePenalty = FastMath.hypot(self.getX() - Constants.getLines()[i].getPreFightPoint().getX(),
													self.getY() - Constants.getLines()[i].getPreFightPoint().getY());
			distancePenalty = 1 / Math.max(1, distancePenalty / 450);
			score[i] *= distancePenalty;
			if (Constants.getLines()[i] == previousLine) {
				score[i] *= Constants.CURRENT_LINE_PRIORITY;
			}
			if (score[maxValue] < score[i]) {
				maxValue = i;
			}
		}
		return Constants.getLines()[maxValue];
	}

	public static double getDistanceToNearestAlly(Unit unit, FilteredWorld filteredWorld, double startDistance) {
		for (LivingUnit livingUnit : filteredWorld.getMinions()) {
			if (livingUnit.getFaction() != Constants.getCurrentFaction()) {
				continue;
			}
			startDistance = Math.min(startDistance, FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()));
		}
		for (LivingUnit livingUnit : filteredWorld.getBuildings()) {
			if (livingUnit.getFaction() != Constants.getCurrentFaction()) {
				continue;
			}
			startDistance = Math.min(startDistance, FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()));
		}
		for (Wizard livingUnit : filteredWorld.getWizards()) {
			if (livingUnit.getFaction() != Constants.getCurrentFaction() || livingUnit.isMe()) {
				continue;
			}
			startDistance = Math.min(startDistance, FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()));
		}
		return startDistance;
	}

	public static int getPrefferedUnitsCountInRange(Unit unit, FilteredWorld filteredWorld, double distance, int damage, int life) {
		int cnt = 0;
		for (LivingUnit livingUnit : filteredWorld.getMinions()) {
			if (livingUnit.getFaction() != Constants.getCurrentFaction()) {
				continue;
			}

			if (livingUnit.getLife() < damage || livingUnit.getLife() >= life) {
				continue;
			}

			if (FastMath.hypot(unit, livingUnit) < distance) {
				++cnt;
			}
		}
		for (Wizard livingUnit : filteredWorld.getWizards()) {
			if (livingUnit.getFaction() != Constants.getCurrentFaction() || livingUnit.isMe()) {
				continue;
			}

			if (livingUnit.getLife() < damage || livingUnit.getLife() >= life) {
				continue;
			}

			if (FastMath.hypot(unit, livingUnit) < distance) {
				++cnt;
			}
		}
		return cnt;
	}

	public static boolean hasAllyNearby(Unit unit, World filteredWorld, double checkDistance) {
		for (LivingUnit livingUnit : filteredWorld.getMinions()) {
			if (livingUnit.getFaction() != Constants.getCurrentFaction()) {
				continue;
			}
			if (FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()) < checkDistance) {
				return true;
			}
		}
		for (Wizard livingUnit : filteredWorld.getWizards()) {
			if (livingUnit.getFaction() != Constants.getCurrentFaction() || livingUnit.isMe()) {
				continue;
			}
			if (FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()) < checkDistance) {
				return true;
			}
		}
		return false;
	}

	public static boolean isNeutralActive(Minion previuosPosition, Minion newPosition) {
		return previuosPosition.getX() != newPosition.getX() ||
				previuosPosition.getY() != newPosition.getY() ||
				previuosPosition.getAngle() != newPosition.getAngle() ||
				newPosition.getLife() != newPosition.getMaxLife() ||
				newPosition.getRemainingActionCooldownTicks() != 0;
	}

	public static int getHitsToKill(int life, int damage) {
		return (life + damage - 1) / damage;
	}

	public static double getMinionAttackPriority(Minion minion, int damage, Wizard self) {
		double score = Constants.LOW_AIM_SCORE;
		double tmp = (minion.getMaxLife() - Math.max(minion.getLife(), damage)) / (double) minion.getMaxLife();
		if (minion.getLife() < damage) {
			tmp -= (damage - minion.getLife()) * .00001;
		}
		score += tmp * tmp;
		if (minion.getType() == MinionType.FETISH_BLOWDART) {
			score *= Constants.FETISH_AIM_PROIRITY;
		} else {
			score *= Constants.ORC_AIM_PROIRITY;
		}

		if (minion.getFaction() == Faction.NEUTRAL) {
			if (damage >= minion.getLife()) {
				score *= Constants.NEUTRAL_LAST_HIT_AIM_PROIRITY;
			} else {
				score *= Constants.NEUTRAL_FACTION_AIM_PROIRITY;
			}
		}
		if (Math.abs(minion.getAngleTo(self)) <= Constants.getGame().getMinionMaxTurnAngle() && FastMath.hypot(self, minion) < minion.getVisionRange()) {
			score *= Constants.ATTACKS_ME_PRIORITY;
			if (minion.getType() == MinionType.ORC_WOODCUTTER) {
				score *= Constants.ORC_ATTACKS_ME_ADD_PRIORITY;
			}
		}
		return score;
	}

	public static void appendStaffTarget(List<Pair<Double, CircularUnit>> staffTargets, CircularUnit unit, Wizard self, double score) {
		double distanceToTarget = FastMath.hypot(self, unit);
		if (distanceToTarget < Constants.getGame().getStaffRange() + unit.getRadius() + 50) {
			distanceToTarget -= Constants.getGame().getStaffRange() + unit.getRadius();
			if (distanceToTarget > 0) {
				score *= 1 - distanceToTarget * .01; // divizion by 100
			}
			staffTargets.add(new Pair<>(score, unit));
		}
	}

	public static void filterTargets(List<Pair<Double, CircularUnit>> targets,
									 ProjectileType shotType,
									 Wizard self,
									 FilteredWorld filteredWorld) {
		Point pointA = new Point(self.getX(), self.getY());
		double projectileRadius = PROJECTIVE_RADIUS[shotType.ordinal()];
//		double projectileSpeed = PROJECTIVE_SPEED[shotType.ordinal()];
		int cntFound = 0;
		for (Iterator<Pair<Double, CircularUnit>> iterator = targets.iterator(); iterator.hasNext(); ) {
			Pair<Double, CircularUnit> item = iterator.next();
			CircularUnit target = item.getSecond();
			Point pointB = getShootPoint(target, self, projectileRadius);

			if (FastMath.hypot(self, pointB) > self.getCastRange()) { // не дострельнем
				iterator.remove();
				continue;
			}

			boolean canShot = true;
			for (Tree tree : filteredWorld.getShootingTreeList()) {
				if (tree == target) {
					continue;
				}
				double distance = Utils.distancePointToSegment(new Point(tree.getX(), tree.getY()), pointA, pointB);
				if (distance + .01 < tree.getRadius() + projectileRadius) {
					canShot = false;
					break;
				}
			}
			// TODO: добавить проверку на уклонение визарда
			if (!canShot) {
				iterator.remove();
			} else {
				++cntFound;
				if (cntFound >= 3) {
					while (iterator.hasNext()) {
						iterator.next();
						iterator.remove();
					}
					return;
				}
			}
		}
	}

	public static boolean noTreesOnWay(Point target,
									   Wizard self,
									   ProjectileType shotType,
									   FilteredWorld filteredWorld) {
		Point pointA = new Point(self.getX(), self.getY());
		double projectileRadius = PROJECTIVE_RADIUS[shotType.ordinal()];
		for (Tree tree : filteredWorld.getShootingTreeList()) {
			double distance = Utils.distancePointToSegment(new Point(tree.getX(), tree.getY()), pointA, target);
			if (distance + .01 < tree.getRadius() + projectileRadius) {
				return false;
			}
		}
		return true;
	}

	public static Point getShootPoint(CircularUnit unit, Wizard self, double projectileRadius) {
		if (unit instanceof Wizard) {
			double preffered = unit.getRadius() + projectileRadius - (unit.getRadius() + projectileRadius) * 6. / 7.;
			return new Point(unit.getX() + Math.cos(unit.getAngle()) * preffered, unit.getY() + Math.sin(unit.getAngle()) * preffered);
		} else if (unit instanceof Building) {
			double angleToMe = unit.getAngleTo(self);
			return new Point(unit.getX() + Math.cos(angleToMe) * (projectileRadius + unit.getRadius() - .1),
							 unit.getY() + Math.sin(angleToMe) * (projectileRadius + unit.getRadius() - .1));
		} else {
			return new Point(unit.getX(), unit.getY());
		}
	}

	public static void applyPreviousContainedModifier(List<Pair<Double, CircularUnit>> newTargets, List<Pair<Double, CircularUnit>> prevTargets) {
		int i = 0;
		double min = 0;
		for (Pair<Double, CircularUnit> newTarget : newTargets) {
			min = Math.min(min, newTarget.getFirst());
		}
		for (Pair<Double, CircularUnit> newTarget : newTargets) {
			newTarget.setFirst(newTarget.getFirst() - min);
		}
		for (Pair<Double, CircularUnit> prevTarget : prevTargets) {
			for (Pair<Double, CircularUnit> newTarget : newTargets) {
				if (prevTarget.getSecond().getId() == newTarget.getSecond().getId()) {
					newTarget.setFirst(newTarget.getFirst() * (1. + (Constants.PREV_AIM_MODIFIER / ++i)));
					break;
				}
			}
		}
	}

	public static int getTicksToFly(double distance, double speed) {
		return (int) Math.floor(distance / speed + .99);
	}

	public static int actionsCoolDown(Wizard wizard, ActionType actionType) {
		return Math.max(wizard.getRemainingActionCooldownTicks(), wizard.getRemainingCooldownTicksByAction()[actionType.ordinal()]);
	}
}
