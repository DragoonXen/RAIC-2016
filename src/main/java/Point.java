/**
 * Created by dragoon on 11/10/16.
 */
public class Point {

	protected double x;
	protected double y;

	public Point() {
	}

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double scalarMult(Point other) {
		return x * other.x + y * other.y;
	}

	public Point mult(double value) {
		return new Point(x * value, y * value);
	}

	public Point add(Point other) {
		return new Point(x + other.x, y + other.y);
	}

	public double norm() {
		return Math.sqrt(scalarMult(this));
	}

	public double norm(Point other) {
		return negate(other).norm();
	}

	public Point negate(Point other) {
		return new Point(x - other.x, y - other.y);
	}
}
