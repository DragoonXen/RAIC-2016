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

public class YYY_Utils {
	public final static Comparator<YYY_Pair<Double, CircularUnit>> AIM_SORT_COMPARATOR = (o1, o2) -> o2.getFirst().compareTo(o1.getFirst());
	public final static Comparator<YYY_Pair<Double, YYY_Point>> POINT_AIM_SORT_COMPARATOR = (o1, o2) -> o2.getFirst().compareTo(o1.getFirst());
	private static double[] lineDistance = new double[YYY_Constants.getLines().length];

	public static int whichLine(Unit unit) {
		int min = 0;
		lineDistance[0] = YYY_Constants.getLines()[0].getDistanceTo(unit);
		for (int i = 1; i != YYY_Constants.getLines().length; ++i) {
			lineDistance[i] = YYY_Constants.getLines()[i].getDistanceTo(unit);
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

	public static YYY_FilteredWorld filterWorld(World world, YYY_Point point, YYY_BuildingPhantom[] buildings, YYY_TeammateIdsContainer teammateIdsContainer) {
		List<Projectile> projectiles = new ArrayList<>();
		for (Projectile projectile : world.getProjectiles()) {
			if (projectile.getOwnerUnitId() == YYY_Variables.self.getId() || teammateIdsContainer.isTeammate(projectile.getOwnerUnitId())) {
				continue;
			}
			projectiles.add(projectile);
		}
		Projectile[] filteredProjectiles = new Projectile[projectiles.size()];
		projectiles.toArray(filteredProjectiles);
		return new YYY_FilteredWorld(world.getTickIndex(),
									 world.getTickCount(),
									 world.getWidth(),
									 world.getHeight(),
									 world.getPlayers(),
									 removeMe(filterUnit(world.getWizards(), point, YYY_FilteredWorld.FilterType.FIGHT)),
									 filterUnit(world.getMinions(), point, YYY_FilteredWorld.FilterType.FIGHT),
									 filterUnit(filteredProjectiles, point, YYY_FilteredWorld.FilterType.FIGHT),
									 filterUnit(world.getBonuses(), point, YYY_FilteredWorld.FilterType.FIGHT),
									 filterUnit(buildings, point, YYY_FilteredWorld.FilterType.FIGHT),
									 filterUnit(world.getTrees(), point, YYY_FilteredWorld.FilterType.MOVE),
									 filterUnit(world.getTrees(), point, YYY_FilteredWorld.FilterType.AIM_OBSTACLE),
									 point);
	}

	public static <T extends CircularUnit> List<T> filterUnit(T[] units, YYY_Point point, YYY_FilteredWorld.FilterType filterType) {
		List<T> unitsList = new ArrayList<>();
		if (units.length == 0) {
			return unitsList;
		}
		double distance = 0.;
		switch (filterType) {
			case FIGHT:
				distance = YYY_Constants.getFightDistanceFilter();
				break;
			case MOVE:
				distance = YYY_Constants.MOVE_DISTANCE_FILTER + YYY_Constants.getGame().getWizardRadius();
				break;
			case AIM:
			case AIM_OBSTACLE:
				distance = YYY_Variables.self.getCastRange();
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

	public static double getDirectionTo(Unit unit, YYY_Point point) {
		return Math.atan2(point.getY() - unit.getY(), point.getX() - unit.getX());
	}

	public static YYY_ScanMatrixItem[][] createScanMatrix() {
		YYY_ScanMatrixItem[][] matrix = new YYY_ScanMatrixItem[(int) Math.round((YYY_Constants.MOVE_FWD_DISTANCE + YYY_Constants.MOVE_BACK_DISTANCE) / YYY_Constants.MOVE_SCAN_STEP + 1.01)]
				[(int) Math.round((YYY_Constants.MOVE_SIDE_DISTANCE + YYY_Constants.MOVE_SIDE_DISTANCE) / YYY_Constants.MOVE_SCAN_STEP + 1.01)];
		double maxBonus = Math.pow(YYY_FastMath.hypot(-matrix.length - 30, -YYY_Constants.CURRENT_PT_Y), YYY_Constants.FORWARD_MOVE_FROM_DISTANCE_POWER);
		for (int i = 0; i != matrix.length; ++i) {
			for (int j = 0; j != matrix[0].length; ++j) {
				matrix[i][j] = new YYY_ScanMatrixItem(i,
													  j,
													  maxBonus - Math.pow(YYY_FastMath.hypot(i - matrix.length - 30, j - YYY_Constants.CURRENT_PT_Y),
																		  YYY_Constants.FORWARD_MOVE_FROM_DISTANCE_POWER));
			}
		}
		int[] di = new int[]{1, 0, 0, -1};
		int[] dj = new int[]{0, 1, -1, 0};
		for (int i = 0; i != matrix.length; ++i) {
			for (int j = 0; j != matrix[0].length; ++j) {
				List<YYY_ScanMatrixItem> neighbour = new ArrayList<>();
				for (int k = 0; k != 4; ++k) {
					int nx = i + di[k];
					int ny = j + dj[k];
					if (nx < 0 || nx >= matrix.length || ny < 0 || ny >= matrix[0].length) {
						continue;
					}
					neighbour.add(matrix[nx][ny]);
				}
				matrix[i][j].setNeighbours(neighbour.toArray(new YYY_ScanMatrixItem[neighbour.size()]));
			}
		}
		List<Map.Entry<Double, YYY_ScanMatrixItem>> pointsMap = new ArrayList<>();
		for (int i = 0; i != matrix.length; ++i) {
			for (int j = 0; j != matrix[0].length; ++j) {
				pointsMap.add(new AbstractMap.SimpleEntry<>(YYY_FastMath.hypot(YYY_Constants.CURRENT_PT_X - i, YYY_Constants.CURRENT_PT_Y - j), matrix[i][j]));
			}
		}
		Collections.sort(pointsMap, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
		for (Map.Entry<Double, YYY_ScanMatrixItem> doubleScanMatrixItemEntry : pointsMap) {
			YYY_ScanMatrixItem item = doubleScanMatrixItemEntry.getValue();
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

	public static double distancePointToSegment(YYY_Point point, YYY_Point segA, YYY_Point segB) {
		return YYY_FastMath.hypot(point, nearestSegmentPoint(point, segA, segB));
	}

	public static YYY_Point nearestSegmentPoint(YYY_Point point, YYY_Point segA, YYY_Point segB) {
		YYY_Point vectorV = segA.negateCopy(segB);
		YYY_Point vectorW = point.negateCopy(segB);
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
		double radius = YYY_Constants.getGame().getWizardRadius();
		if (x < radius || y < radius || x + radius > YYY_Constants.getGame().getMapSize() || y + radius > YYY_Constants.getGame().getMapSize()) {
			return false;
		}
		Iterator<CircularUnit> iterator = units.iterator();
		while (iterator.hasNext()) {
			CircularUnit unit = iterator.next();
			if (YYY_FastMath.hypot(unit.getX() - x, unit.getY() - y) < unit.getRadius() + radius + YYY_Constants.STUCK_FIX_RADIUS_ADD) {
				return false;
			}
		}
		return true;
	}

	public static void calcTilesAvailable(List<CircularUnit> units, YYY_ScanMatrixItem[] items) {
		double radius = YYY_Constants.getGame().getWizardRadius();
		double x, y;
		List<CircularUnit> filteredUnits = new ArrayList<>(units.size());
		for (CircularUnit unit : units) {
			x = YYY_Utils.distancePointToSegment(new YYY_Point(unit.getX(), unit.getY()), items[0], items[items.length - 1]);
			if (x < unit.getRadius() + radius + YYY_Constants.STUCK_FIX_RADIUS_ADD) {
				filteredUnits.add(unit);
			}
		}
		blockUnit = null;
		for (int i = 0; i != items.length; ++i) {
			YYY_ScanMatrixItem item = items[i];
			x = item.getX();
			y = item.getY();
			if (x < radius || y < radius || x + radius > YYY_Constants.getGame().getMapSize() || y + radius > YYY_Constants.getGame().getMapSize()) {
				item.setAvailable(false);
				continue;
			}
			if (blockUnit != null) {
				if (YYY_FastMath.hypot(blockUnit.getX() - x, blockUnit.getY() - y) < blockUnit.getRadius() + radius + YYY_Constants.STUCK_FIX_RADIUS_ADD) {
					item.setAvailable(false);
					continue;
				}
				filteredUnits.remove(blockUnit);
				blockUnit = null;
			}
			for (CircularUnit unit : filteredUnits) {
				if (YYY_FastMath.hypot(unit.getX() - x, unit.getY() - y) < unit.getRadius() + radius + YYY_Constants.STUCK_FIX_RADIUS_ADD) {
					item.setAvailable(false);
					blockUnit = unit;
					break;
				}
			}
		}
	}

	public static void calcTileScore(YYY_ScanMatrixItem item,
									 YYY_FilteredWorld filteredWorld,
									 YYY_BaseLine myLineCalc,
									 Wizard self,
									 YYY_UnitScoreCalculation unitScoreCalculation,
									 YYY_FightStatus fightStatus) {
		item.setAvailable(YYY_Utils.isAvailableTile(filteredWorld.getAllBlocksList(), item.getX(), item.getY()));
		if (!item.isAvailable()) {
			return;
		}
		double distanceTo = myLineCalc.calcLineDistanceOtherDanger(item);
		if (distanceTo > 0.) {
			item.addOtherDanger(distanceTo);
		}
		for (Bonus bonus : filteredWorld.getBonuses()) {
			if (YYY_FastMath.hypot(self, bonus) > YYY_Constants.getFightDistanceFilter()) {
				continue;
			}
			YYY_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(bonus.getId());
			structure.applyScores(item, YYY_FastMath.hypot(bonus, item));
		}
		if (YYY_Utils.getTicksToBonusSpawn(filteredWorld.getTickIndex()) < 250) {
			for (int i = 0; i != YYY_BonusesPossibilityCalcs.BONUSES_POINTS.length; ++i) {
				if (YYY_FastMath.hypot(self, YYY_BonusesPossibilityCalcs.BONUSES_POINTS[i]) > YYY_Constants.getFightDistanceFilter()) {
					continue;
				}
				YYY_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(i - 5);
				structure.applyScores(item, YYY_FastMath.hypot(YYY_BonusesPossibilityCalcs.BONUSES_POINTS[i], item));
			}
		}
		if (fightStatus == YYY_FightStatus.NO_ENEMY) {
			return;
		}
		for (Minion minion : filteredWorld.getMinions()) {
			if (minion.getFaction() == YYY_Constants.getCurrentFaction()) {
				continue;
			}
			YYY_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(minion.getId());
			structure.applyScores(item, YYY_FastMath.hypot(minion, item));
		}
		for (Wizard wizard : filteredWorld.getWizards()) {
			if (wizard.getFaction() == YYY_Constants.getCurrentFaction()) {
				continue;
			}
			YYY_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(wizard.getId());
			structure.applyScores(item, YYY_FastMath.hypot(wizard, item));
		}
		for (Building building : filteredWorld.getBuildings()) {
			if (building.getFaction() == YYY_Constants.getCurrentFaction()) {
				continue;
			}
			YYY_ScoreCalcStructure structure = unitScoreCalculation.getUnitsScoreCalc(building.getId());
			structure.applyScores(item, YYY_FastMath.hypot(building.getX() - item.getX(), building.getY() - item.getY()));
		}
	}

	public static boolean hasEnemy(Minion[] units, YYY_AgressiveNeutralsCalcs agressiveNeutralsCalcs) {
		for (LivingUnit unit : units) {
			if (unit.getFaction() == YYY_Constants.getEnemyFaction() || unit.getFaction() == Faction.NEUTRAL && agressiveNeutralsCalcs.isMinionAgressive(unit.getId())) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasEnemy(LivingUnit[] units) {
		for (LivingUnit unit : units) {
			if (unit.getFaction() == YYY_Constants.getEnemyFaction()) {
				return true;
			}
		}
		return false;
	}

	public static boolean wizardHasStatus(Wizard wizard, StatusType statusType) {
		return wizardStatusTicks(wizard, statusType) != 0;
	}

	public static int wizardStatusTicks(Wizard wizard, StatusType statusType) {
		YYY_WizardsInfo.WizardInfo wizardInfo = YYY_Variables.wizardsInfo.getWizardInfo(wizard.getId());
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
			{YYY_Constants.getGame().getMagicMissileSpeed(), YYY_Constants.getGame().getFrostBoltSpeed(), YYY_Constants.getGame().getFireballSpeed(), YYY_Constants.getGame().getDartSpeed()};
	public final static int[] PROJECTIVE_DAMAGE = new int[]
			{YYY_Constants.getGame().getMagicMissileDirectDamage(), YYY_Constants.getGame().getFrostBoltDirectDamage(), YYY_Constants.getGame().getBurningSummaryDamage(), YYY_Constants.getGame().getDartDirectDamage()};
	public final static double[] PROJECTIVE_RADIUS = new double[]
			{YYY_Constants.getGame().getMagicMissileRadius(), YYY_Constants.getGame().getFrostBoltRadius(), YYY_Constants.getGame().getFireballRadius(), YYY_Constants.getGame().getDartRadius()};

	public static int getProjectileDamage(Projectile projectile, double distance) {
		return getProjectileDamage(projectile.getType(), distance);
	}

	public static int getProjectileDamage(ProjectileType projectileType, double distance) {
		int damage = PROJECTIVE_DAMAGE[projectileType.ordinal()];
		if (projectileType == ProjectileType.FIREBALL) {
			distance -= YYY_Constants.getGame().getWizardRadius();
			if (distance < YYY_Constants.getGame().getFireballExplosionMaxDamageRange()) {
				return damage + YYY_Constants.getGame().getFireballExplosionMaxDamage();
			} else {
				distance -= YYY_Constants.getGame().getFireballExplosionMaxDamageRange();
				return damage + YYY_Constants.getGame().getFireballExplosionMaxDamage() -
						(int) ((YYY_Constants.getFireballLowerindDamageDistance() + distance) / YYY_Constants.getFireballLowerindDamageDistance());
			}
		}
		return damage;
	}

	public static int getIntDamage(double damage) {
		return (int) Math.floor(damage);
	}

	public static void fillProjectilesSim(YYY_FilteredWorld filteredWorld, HashMap<Long, Double> projectilesDTL) {
		YYY_Variables.projectilesSim.clear();
		for (Projectile projectile : filteredWorld.getProjectiles()) {
			YYY_Variables.projectilesSim.add(new AbstractMap.SimpleEntry<Projectile, Double>(projectile, projectilesDTL.get(projectile.getId())));
		}
	}

	public static double finalizeFireballsDamage() {
		double damage = 0;
		for (double val : YYY_Variables.fireballHitDamageCheck.values()) {
			damage += YYY_Utils.getProjectileDamage(ProjectileType.FIREBALL, val);
		}
		YYY_Variables.fireballHitDamageCheck.clear();
		return damage;
	}

	public static double checkProjectiveCollision(YYY_Point point, int ticks) {
		double selfRadius = YYY_Variables.self.getRadius();
		double damage = 0.;
		Iterator<AbstractMap.SimpleEntry<Projectile, Double>> iterator = YYY_Variables.projectilesSim.iterator();
		while (iterator.hasNext()) {
			AbstractMap.SimpleEntry<Projectile, Double> next = iterator.next();
			Projectile projectile = next.getKey();
			YYY_Point projectileVector = new YYY_Point(projectile.getSpeedX(), projectile.getSpeedY());
			YYY_Point startProjectilePoint = new YYY_Point(projectile.getX() + projectile.getSpeedX() * ticks,
														   projectile.getY() + projectile.getSpeedY() * ticks);
			boolean remove = false;
			if (projectileVector.vectorNorm() >= next.getValue() - .0001) {
				projectileVector.fixVectorLength(next.getValue());
				remove = true;
			} else {
				next.setValue(next.getValue() - YYY_Utils.PROJECTIVE_SPEED[projectile.getType().ordinal()]);
			}
			projectileVector.add(startProjectilePoint);
			double projectiveRadius = projectile.getRadius();
			if (projectile.getType() == ProjectileType.FIREBALL) {
				projectiveRadius = YYY_Constants.getGame().getFireballExplosionMinDamageRange();
			}
			double distanceToProjective = YYY_Utils.distancePointToSegment(point, startProjectilePoint, projectileVector);
			if (distanceToProjective < projectiveRadius + selfRadius + .001) {
				if (projectile.getType() == ProjectileType.FIREBALL) {
					if (distanceToProjective < YYY_Constants.getGame().getFireballExplosionMaxDamageRange() + selfRadius) {
						damage += getProjectileDamage(projectile, distanceToProjective);
						remove = true;
						YYY_Variables.fireballHitDamageCheck.remove(projectile.getId());
					} else {
						Double aDouble = YYY_Variables.fireballHitDamageCheck.get(projectile.getId());
						if (aDouble != null) {
							aDouble = Math.min(aDouble, distanceToProjective);
						} else {
							aDouble = distanceToProjective;
						}
						YYY_Variables.fireballHitDamageCheck.put(projectile.getId(), aDouble);
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
			if (YYY_FastMath.hypot(unit.getX() - unitCountFor.getX(), unit.getY() - unitCountFor.getY()) < distance) {
				++result;
			}
		}
		return result;
	}

	public static int unitsCountCloseToDestination(LivingUnit[] units, YYY_Point destination) {
		if (destination == null) {
			return 0;
		}
		int result = 0;
		for (LivingUnit unit : units) {
			if (YYY_FastMath.hypot(unit.getX() - destination.getX(), unit.getY() - destination.getY()) < YYY_Constants.getGame().getWizardRadius() +
					unit.getRadius() +
					YYY_Constants.MOVE_SCAN_DIAGONAL_DISTANCE + .1) {
				++result;
			}
		}
		return result;
	}

	public static int unitsCountCloseToDestination(List<CircularUnit> units, YYY_Point destination) {
		if (destination == null) {
			return 0;
		}
		int result = 0;
		for (CircularUnit unit : units) {
			if (YYY_FastMath.hypot(unit.getX() - destination.getX(), unit.getY() - destination.getY()) < YYY_Constants.getGame().getWizardRadius() +
					unit.getRadius() +
					YYY_Constants.MOVE_SCAN_DIAGONAL_DISTANCE + .1) {
				++result;
			}
		}
		return result;
	}

	public static boolean isPositionVisible(YYY_Point position, double additionalDistance, Wizard[] wizards, Minion[] minions, Building[] buildings) {
		for (Wizard unit : wizards) {
			if (unit.getFaction() != YYY_Constants.getCurrentFaction()) {
				continue;
			}
			if (YYY_FastMath.hypot(position.getX() - unit.getX(), position.getY() - unit.getY()) + additionalDistance < unit.getVisionRange()) {
				return true;
			}
		}
		for (Minion unit : minions) {
			if (unit.getFaction() != YYY_Constants.getCurrentFaction()) {
				continue;
			}
			if (YYY_FastMath.hypot(position.getX() - unit.getX(), position.getY() - unit.getY()) + additionalDistance < unit.getVisionRange()) {
				return true;
			}
		}
		if (buildings != null) {
			for (Building unit : buildings) {
				if (unit.getFaction() != YYY_Constants.getCurrentFaction()) {
					continue;
				}
				if (YYY_FastMath.hypot(position.getX() - unit.getX(), position.getY() - unit.getY()) + additionalDistance < unit.getVisionRange()) {
					return true;
				}
			}
		}
		return false;
	}

	public static double cooldownDistanceMinionsCalculation(double baseDistance, int coolDownRemaining) {
		return baseDistance + Math.min(1,
									   -coolDownRemaining + 3) * YYY_Constants.getGame().getWizardBackwardSpeed() * YYY_Variables.wizardsInfo.getMe().getMoveFactor() * .66;
	}

	public static double cooldownDistanceWizardCalculation(double moveFactor, int coolDownRemaining) {
		return Math.min(2, -coolDownRemaining + 4) * YYY_Constants.getGame().getWizardForwardSpeed() * moveFactor * .6;
	}

	public static int getTicksToBonusSpawn(int tickNo) {
		if (tickNo > 17501) {
			return 20000;
		}
		return 2500 - (tickNo - 1) % 2500;
	}

	private static double score[] = new double[3];

	public static YYY_BaseLine fightLineSelect(YYY_BaseLine previousLine, World world, YYY_EnemyPositionCalc enemyPositionCalc, Wizard self) {
		for (int i = 0; i != 3; ++i) {
			int currScore = enemyPositionCalc.getMinionsOnLine()[i];
			if (currScore < 4) {
				score[i] = YYY_Constants.minionLineScore * 4;
			} else {
				score[i] = YYY_Constants.minionLineScore * currScore;
			}
		}
		YYY_WizardsInfo wizardsInfo = YYY_Variables.wizardsInfo;
		for (YYY_WizardPhantom wizard : enemyPositionCalc.getDetectedWizards().values()) {
			if (wizard.getLastSeenTick() == 0) {
				continue;
			}
			int line = wizardsInfo.getWizardInfo(wizard.getId()).getLineNo();
			score[line] += YYY_Constants.enemyWizardLineScore;
		}
		for (Wizard wizard : world.getWizards()) {
			if (wizard.isMe() || wizard.getFaction() == YYY_Constants.getEnemyFaction()) {
				continue;
			}
			int line = wizardsInfo.getWizardInfo(wizard.getId()).getLineNo();
			if (wizard.getFaction() == YYY_Constants.getCurrentFaction()) {
				score[line] *= YYY_Constants.wizardLineMult;
			}
		}
		int maxValue = 0;
		for (int i = 0; i != 3; ++i) {
			double distancePenalty = YYY_FastMath.hypot(self.getX() - YYY_Constants.getLines()[i].getPreFightPoint().getX(),
														self.getY() - YYY_Constants.getLines()[i].getPreFightPoint().getY());
			distancePenalty = 1 / Math.max(1, distancePenalty / 450);
			score[i] *= distancePenalty;
			if (YYY_Constants.getLines()[i] == previousLine) {
				score[i] *= YYY_Constants.CURRENT_LINE_PRIORITY;
			}
			if (score[maxValue] < score[i]) {
				maxValue = i;
			}
		}
		return YYY_Constants.getLines()[maxValue];
	}

	public static double getDistanceToNearestAlly(Unit unit, YYY_FilteredWorld filteredWorld, double startDistance) {
		for (LivingUnit livingUnit : filteredWorld.getMinions()) {
			if (livingUnit.getFaction() != YYY_Constants.getCurrentFaction()) {
				continue;
			}
			startDistance = Math.min(startDistance, YYY_FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()));
		}
		for (LivingUnit livingUnit : filteredWorld.getBuildings()) {
			if (livingUnit.getFaction() != YYY_Constants.getCurrentFaction()) {
				continue;
			}
			startDistance = Math.min(startDistance, YYY_FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()));
		}
		for (Wizard livingUnit : filteredWorld.getWizards()) {
			if (livingUnit.getFaction() != YYY_Constants.getCurrentFaction() || livingUnit.isMe()) {
				continue;
			}
			startDistance = Math.min(startDistance, YYY_FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()));
		}
		return startDistance;
	}

	public static int getPrefferedUnitsCountInRange(Unit unit, YYY_FilteredWorld filteredWorld, double distance, int damage, int life) {
		int cnt = 0;
		for (LivingUnit livingUnit : filteredWorld.getMinions()) {
			if (livingUnit.getFaction() != YYY_Constants.getCurrentFaction()) {
				continue;
			}
			if (livingUnit.getLife() < damage || livingUnit.getLife() >= life) {
				continue;
			}
			if (YYY_FastMath.hypot(unit, livingUnit) < distance) {
				++cnt;
			}
		}
		for (Wizard livingUnit : filteredWorld.getWizards()) {
			if (livingUnit.getFaction() != YYY_Constants.getCurrentFaction() || livingUnit.isMe()) {
				continue;
			}
			if (livingUnit.getLife() < damage || livingUnit.getLife() >= life) {
				continue;
			}
			if (YYY_FastMath.hypot(unit, livingUnit) < distance) {
				++cnt;
			}
		}
		return cnt;
	}

	public static boolean hasAllyNearby(Unit unit, World filteredWorld, double checkDistance) {
		for (LivingUnit livingUnit : filteredWorld.getMinions()) {
			if (livingUnit.getFaction() != YYY_Constants.getCurrentFaction()) {
				continue;
			}
			if (YYY_FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()) < checkDistance) {
				return true;
			}
		}
		for (Wizard livingUnit : filteredWorld.getWizards()) {
			if (livingUnit.getFaction() != YYY_Constants.getCurrentFaction() || livingUnit.isMe()) {
				continue;
			}
			if (YYY_FastMath.hypot(unit.getX() - livingUnit.getX(), unit.getY() - livingUnit.getY()) < checkDistance) {
				return true;
			}
		}
		return false;
	}

	public static boolean isNeutralActive(Minion previuosPosition, Minion newPosition) {
		return previuosPosition.getX() != newPosition.getX() || previuosPosition.getY() != newPosition.getY() || previuosPosition.getAngle() != newPosition.getAngle() || newPosition.getLife() != newPosition.getMaxLife() || newPosition.getRemainingActionCooldownTicks() != 0;
	}

	public static int getHitsToKill(int life, int damage) {
		return (life + damage - 1) / damage;
	}

	public static double getMinionAttackPriority(Minion minion, int damage, Wizard self) {
		double score = YYY_Constants.LOW_AIM_SCORE;
		double tmp = (minion.getMaxLife() - Math.max(minion.getLife(), damage)) / (double) minion.getMaxLife();
		if (minion.getLife() < damage) {
			tmp -= (damage - minion.getLife()) * .00001;
		}
		score += tmp * tmp;
		if (minion.getType() == MinionType.FETISH_BLOWDART) {
			score *= YYY_Constants.FETISH_AIM_PROIRITY;
		} else {
			score *= YYY_Constants.ORC_AIM_PROIRITY;
		}
		if (minion.getFaction() == Faction.NEUTRAL) {
			if (damage >= minion.getLife()) {
				score *= YYY_Constants.NEUTRAL_LAST_HIT_AIM_PROIRITY;
			} else {
				score *= YYY_Constants.NEUTRAL_FACTION_AIM_PROIRITY;
			}
		}
		if (Math.abs(minion.getAngleTo(self)) <= YYY_Constants.getGame().getMinionMaxTurnAngle() && YYY_FastMath.hypot(self,
																													   minion) < minion.getVisionRange()) {
			score *= YYY_Constants.ATTACKS_ME_PRIORITY;
			if (minion.getType() == MinionType.ORC_WOODCUTTER) {
				score *= YYY_Constants.ORC_ATTACKS_ME_ADD_PRIORITY;
			}
		}
		return score;
	}

	public static void appendStaffTarget(List<YYY_Pair<Double, CircularUnit>> staffTargets, CircularUnit unit, Wizard self, double score) {
		double distanceToTarget = YYY_FastMath.hypot(self, unit);
		if (distanceToTarget < YYY_Constants.getGame().getStaffRange() + unit.getRadius() + 50) {
			distanceToTarget -= YYY_Constants.getGame().getStaffRange() + unit.getRadius();
			if (distanceToTarget > 0) {
				score *= 1 - distanceToTarget * .01;
			}
			staffTargets.add(new YYY_Pair<>(score, unit));
		}
	}

	public static void filterTargets(List<YYY_Pair<Double, CircularUnit>> targets, ProjectileType shotType, Wizard self, YYY_FilteredWorld filteredWorld) {
		YYY_Point pointA = new YYY_Point(self.getX(), self.getY());
		double projectileRadius = PROJECTIVE_RADIUS[shotType.ordinal()];
		int cntFound = 0;
		for (Iterator<YYY_Pair<Double, CircularUnit>> iterator = targets.iterator(); iterator.hasNext(); ) {
			YYY_Pair<Double, CircularUnit> item = iterator.next();
			CircularUnit target = item.getSecond();
			YYY_Point pointB = getShootPoint(target, self, projectileRadius);
			if (YYY_FastMath.hypot(self, pointB) > self.getCastRange()) {
				iterator.remove();
				continue;
			}
			boolean canShot = true;
			for (Tree tree : filteredWorld.getShootingTreeList()) {
				if (tree == target) {
					continue;
				}
				double distance = YYY_Utils.distancePointToSegment(new YYY_Point(tree.getX(), tree.getY()), pointA, pointB);
				if (distance + .01 < tree.getRadius() + projectileRadius) {
					canShot = false;
					break;
				}
			}
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

	public static boolean noTreesOnWay(YYY_Point target, Wizard self, ProjectileType shotType, YYY_FilteredWorld filteredWorld) {
		YYY_Point pointA = new YYY_Point(self.getX(), self.getY());
		double projectileRadius = PROJECTIVE_RADIUS[shotType.ordinal()];
		for (Tree tree : filteredWorld.getShootingTreeList()) {
			double distance = YYY_Utils.distancePointToSegment(new YYY_Point(tree.getX(), tree.getY()), pointA, target);
			if (distance + .01 < tree.getRadius() + projectileRadius) {
				return false;
			}
		}
		return true;
	}

	public static YYY_Point getShootPoint(CircularUnit unit, Wizard self, double projectileRadius) {
		if (unit instanceof Wizard) {
			double preffered = unit.getRadius() + projectileRadius - (unit.getRadius() + projectileRadius) * 6. / 7.;
			return new YYY_Point(unit.getX() + Math.cos(unit.getAngle()) * preffered, unit.getY() + Math.sin(unit.getAngle()) * preffered);
		} else if (unit instanceof Building) {
			double angleToMe = unit.getAngleTo(self);
			return new YYY_Point(unit.getX() + Math.cos(angleToMe) * (projectileRadius + unit.getRadius() - .1),
								 unit.getY() + Math.sin(angleToMe) * (projectileRadius + unit.getRadius() - .1));
		} else {
			return new YYY_Point(unit.getX(), unit.getY());
		}
	}

	public static void applyPreviousContainedModifier(List<YYY_Pair<Double, CircularUnit>> newTargets, List<YYY_Pair<Double, CircularUnit>> prevTargets) {
		int i = 0;
		double min = 0;
		for (YYY_Pair<Double, CircularUnit> newTarget : newTargets) {
			min = Math.min(min, newTarget.getFirst());
		}
		for (YYY_Pair<Double, CircularUnit> newTarget : newTargets) {
			newTarget.setFirst(newTarget.getFirst() - min);
		}
		for (YYY_Pair<Double, CircularUnit> prevTarget : prevTargets) {
			for (YYY_Pair<Double, CircularUnit> newTarget : newTargets) {
				if (prevTarget.getSecond().getId() == newTarget.getSecond().getId()) {
					newTarget.setFirst(newTarget.getFirst() * (1. + (YYY_Constants.PREV_AIM_MODIFIER / ++i)));
					break;
				}
			}
		}
	}

	public static int getTicksToFly(double distance, double speed) {
		return (int) Math.floor(distance / speed + .99999999);
	}

	public static int actionsCoolDown(Wizard wizard, ActionType actionType) {
		return Math.max(wizard.getRemainingActionCooldownTicks(), wizard.getRemainingCooldownTicksByAction()[actionType.ordinal()]);
	}
}