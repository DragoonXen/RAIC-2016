/**
 * Created by dragoon on 11/13/16.
 */
public class YYY_WayPoint implements Comparable<YYY_WayPoint> {

	private double dangerOnWay;
	private double scoresOnWay;

	private int distanceFromStart;

	private YYY_ScanMatrixItem point;

	private YYY_WayPoint prev;

	public YYY_WayPoint(int distanceFromStart, YYY_ScanMatrixItem point, YYY_WayPoint prev) {
		this.distanceFromStart = distanceFromStart;
		this.point = point;
		this.prev = prev;
		this.scoresOnWay = point.getTotalScore(YYY_Variables.self) - YYY_Variables.maxDangerMatrixScore;
		this.dangerOnWay = point.getAllDangers();
		if (prev != null) {
			this.scoresOnWay += prev.scoresOnWay;
			this.dangerOnWay += prev.dangerOnWay;
		}
		this.point.setWayPoint(this);
	}

	public YYY_ScanMatrixItem getPoint() {
		return point;
	}

	public YYY_WayPoint getPrev() {
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
	public int compareTo(YYY_WayPoint o) {
		if (scoresOnWay != o.scoresOnWay) {
			return Double.compare(o.scoresOnWay, scoresOnWay);
		}
		if (distanceFromStart != o.distanceFromStart) {
			return Integer.compare(distanceFromStart, o.distanceFromStart);
		}
		return Double.compare(dangerOnWay, o.dangerOnWay);
	}
}
