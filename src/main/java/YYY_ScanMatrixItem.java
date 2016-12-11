import model.Wizard;

/**
 * Created by dragoon on 11/12/16.
 */
public class YYY_ScanMatrixItem extends YYY_Point {

	private int i, j;

	private YYY_ScanMatrixItem[] neighbours;

	private YYY_WayPoint wayPoint;

	private int distance;
	private double distanceFromSelf;

	private double wizardsDanger;
	private double minionsDanger;
	private double buildingsDanger;
	private double otherDanger;
	private double otherBonus;
	private double attackBonus;
	private double meleeAttackBonus;
	private double expBonus;
	private double totalScore;
	private boolean available;

	private double forwardDistanceDivision;

	public YYY_ScanMatrixItem(int i, int j, double forwardDistanceDivision) {
		this.i = i;
		this.j = j;
		this.forwardDistanceDivision = forwardDistanceDivision;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public int getDistance() {
		return distance;
	}

	public double getDistanceFromSelf() {
		return distanceFromSelf;
	}

	public void setDistanceFromSelf(double distanceFromSelf) {
		this.distanceFromSelf = distanceFromSelf;
	}

	public void setPoint(double x, double y) {
		this.x = x;
		this.y = y;
		this.wizardsDanger = 0.;
		this.minionsDanger = 0.;
		this.buildingsDanger = 0.;
		this.otherDanger = 0.;
		if (x + y < 627 || x + y + 627 > YYY_Constants.getDoubledMapSize()) {
			if (x > 2000) {
				x = YYY_Constants.getDoubledMapSize() - x - y;
			} else {
				x += y;
			}
			otherDanger = (627 - x) / 100 + 1.;
			otherDanger = (otherDanger * otherDanger - 1) * YYY_Constants.MAP_CORNER_DANGER_FACTOR;
		} else {
			checkMapSideDanger(x);
			checkMapSideDanger(y);
		}
		this.attackBonus = 0.;
		this.expBonus = 0.;
		this.distance = -1;
		this.meleeAttackBonus = 0.;
		this.otherBonus = 0.;
		this.wayPoint = null;
		this.totalScore = Double.MIN_VALUE;
		this.available = true;
	}

	private void checkMapSideDanger(double coord) {
		if (coord > YYY_Constants.getGame().getMapSize() / 2) {
			coord = YYY_Constants.getGame().getMapSize() - coord - 1;
		}
		if (coord < YYY_Constants.MAP_SIDE_DANGER_DISTANCE) {
			otherDanger += (YYY_Constants.MAP_SIDE_DANGER_DISTANCE - coord) * YYY_Constants.MAP_SIDE_DANGER_FACTOR;
		}
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public void setNeighbours(YYY_ScanMatrixItem[] neighbours) {
		this.neighbours = neighbours;
	}

	public double getWizardsDanger() {
		return wizardsDanger;
	}

	public double getMinionsDanger() {
		return minionsDanger;
	}

	public double getBuildingsDanger() {
		return buildingsDanger;
	}

	public double getOtherDanger() {
		return otherDanger;
	}

	public double getAttackBonus() {
		return attackBonus;
	}

	public double getExpBonus() {
		return expBonus;
	}

	public void addWizardsDanger(double wizardsDanger) {
		this.wizardsDanger += wizardsDanger;
	}

	public void addMinionsDanger(double minionsDanger) {
		this.minionsDanger += minionsDanger;
	}

	public void addBuildingsDanger(double buildingsDanger) {
		this.buildingsDanger += buildingsDanger;
	}

	public void addOtherDanger(double otherDanger) {
		this.otherDanger += otherDanger;
	}

	public void putAttackBonus(double attackBonus) {
		this.attackBonus = Math.max(this.attackBonus, attackBonus);
	}

	public void putMeleeAttackBonus(double attackBonus) {
		this.meleeAttackBonus = Math.max(this.meleeAttackBonus, attackBonus);
	}

	public void addExpBonus(double bonus) {
		this.expBonus += bonus;
	}

	public void addExpBonus(int life, int maxLife) {
		addExpBonus(life, maxLife, 1.);
	}

	public void addExpBonus(int life, int maxLife, double factor) {
		this.expBonus += calcExpBonus(life, maxLife, factor);
	}

	public static double calcExpBonus(int life, int maxLife, double factor) {
		double tmp = ((maxLife - life) / (double) maxLife) * (maxLife / 100.);
		return tmp * tmp * factor;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean blocked) {
		this.available = blocked;
	}

	public double getAllDangers() {
		return buildingsDanger + wizardsDanger + minionsDanger;
	}

	public double getTotalScore(Wizard self) {
		if (totalScore != Double.MIN_VALUE) {
			return totalScore;
		}

		double total = 0.;
		double dangers = getAllDangers();
		dangers /= self.getLife();
		total -= dangers * YYY_Constants.DANGER_PENALTY;

		total += expBonus;
		total += meleeAttackBonus;
		total += attackBonus;
		total -= otherDanger;
		total += otherBonus;

		return totalScore = total;
	}

	public void setWayPoint(YYY_WayPoint wayPoint) {
		this.wayPoint = wayPoint;
	}

	public YYY_WayPoint getWayPoint() {
		return wayPoint;
	}

	public void addOtherBonus(double otherBonus) {
		this.otherBonus += otherBonus;
	}

	public int getI() {
		return i;
	}

	public int getJ() {
		return j;
	}

	public double getForwardDistanceDivision() {
		return forwardDistanceDivision;
	}

	public YYY_ScanMatrixItem[] getNeighbours() {
		return neighbours;
	}
}
