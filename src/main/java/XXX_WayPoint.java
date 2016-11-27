/**
 * Created by dragoon on 11/13/16.
 */
public class XXX_WayPoint implements Comparable<XXX_WayPoint> {

	private double dangerOnWay;
	private double scoresOnWay;

	private int distanceFromStart;

	private XXX_ScanMatrixItem point;

	private XXX_WayPoint prev;

	public XXX_WayPoint(int distanceFromStart, XXX_ScanMatrixItem point, XXX_WayPoint prev) {
		this.distanceFromStart = distanceFromStart;
		this.point = point;
		this.prev = prev;
		this.scoresOnWay = point.getTotalScore(XXX_Variables.self) - XXX_Variables.maxDangerMatrixScore;
		this.dangerOnWay = point.getAllDangers();
		if (prev != null) {
			this.scoresOnWay += prev.scoresOnWay;
			this.dangerOnWay += prev.dangerOnWay;
		}
		this.point.setWayPoint(this);
	}

	public XXX_ScanMatrixItem getPoint() {
		return point;
	}

	public XXX_WayPoint getPrev() {
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
	public int compareTo(XXX_WayPoint o) {
		if (scoresOnWay != o.scoresOnWay) {
			return Double.compare(o.scoresOnWay, scoresOnWay);
		}
		if (distanceFromStart != o.distanceFromStart) {
			return Integer.compare(distanceFromStart, o.distanceFromStart);
		}
		return Double.compare(dangerOnWay, o.dangerOnWay);
	}
}
