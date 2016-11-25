/**
 * Created by dragoon on 11/13/16.
 */
public class WayPoint implements Comparable<WayPoint> {

	private double dangerOnWay;
	private double scoresOnWay;

	private int distanceFromStart;

	private ScanMatrixItem point;

	private WayPoint prev;

	public WayPoint(int distanceFromStart, ScanMatrixItem point, WayPoint prev) {
		this.distanceFromStart = distanceFromStart;
		this.point = point;
		this.prev = prev;
		this.scoresOnWay = point.getTotalScore(Variables.self) - Variables.maxDangerMatrixScore;
		this.dangerOnWay = point.getAllDangers();
		if (prev != null) {
			this.scoresOnWay += prev.scoresOnWay;
			this.dangerOnWay += prev.dangerOnWay;
		}
		this.point.setWayPoint(this);
	}

	public ScanMatrixItem getPoint() {
		return point;
	}

	public WayPoint getPrev() {
		return prev;
	}

	public double getDangerOnWay() {
		return dangerOnWay;
	}

	public double getScoresOnWay() {
		return scoresOnWay;
	}

	public int getDistanceFromStart() {
		return distanceFromStart;
	}

	@Override
	public int compareTo(WayPoint o) {
		if (scoresOnWay != o.scoresOnWay) {
			return Double.compare(o.scoresOnWay, scoresOnWay);
		}
		if (distanceFromStart != o.distanceFromStart) {
			return Integer.compare(distanceFromStart, o.distanceFromStart);
		}
		return Double.compare(dangerOnWay, o.dangerOnWay);
	}
}
