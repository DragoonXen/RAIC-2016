import model.Wizard;

/**
 * Created by dragoon on 11/12/16.
 */
public class ScanMatrixItem extends Point {

	private int i, j;

	private ScanMatrixItem[] neighbours;

	private WayPoint wayPoint;

	private int distance;
	private double distanceFromSelf;

	private double wizardsDanger;
	private double minionsDanger;
	private double buildingsDanger;
	private double otherDanger;
	private double otherBonus;
	private double attackBonus;
	private double expBonus;
	private double totalScore;
	private boolean available;

	public ScanMatrixItem(int i, int j) {
		this.i = i;
		this.j = j;
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
		this.attackBonus = 0.;
		this.expBonus = 0.;
		this.distance = -1;
		this.otherBonus = 0.;
		this.wayPoint = null;
		this.totalScore = Double.MIN_VALUE;
		//this.available = false;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public void setNeighbours(ScanMatrixItem[] neighbours) {
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

	public void addExpBonus(int life, int maxLife) {
		addExpBonus(life, maxLife, 1.);
	}

	public void addExpBonus(int life, int maxLife, double factor) {
		double tmp = ((maxLife - life) / (double) maxLife) * (maxLife / 100.);
		this.expBonus += tmp * tmp * factor;
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
		if (dangers >= self.getLife()) {
			dangers = self.getLife();
		}
		dangers /= self.getLife();
		total -= dangers * dangers * Constants.DANGER_PENALTY;

		total += expBonus;
		total += attackBonus;
		total -= otherDanger;
		total += otherBonus;

		return totalScore = total;
	}

	public void setWayPoint(WayPoint wayPoint) {
		this.wayPoint = wayPoint;
	}

	public WayPoint getWayPoint() {
		return wayPoint;
	}

	public void setOtherBonus(double otherBonus) {
		this.otherBonus = otherBonus;
	}

	public int getI() {
		return i;
	}

	public int getJ() {
		return j;
	}

	public ScanMatrixItem[] getNeighbours() {
		return neighbours;
	}
}
