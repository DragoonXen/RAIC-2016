/**
 * Created by dragoon on 11/10/16.
 */
public class Point {

	protected double x;
	protected double y;

	public final static Point EMPTY_POINT = new Point();

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

	public void update(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void update(Point point) {
		update(point.getX(), point.getY());
	}

	public double scalarMult(Point other) {
		return x * other.x + y * other.y;
	}

	public Point mult(double value) {
		return new Point(x * value, y * value);
	}

	public Point addWithCopy(Point other) {
		return new Point(x + other.x, y + other.y);
	}

	public void add(Point other) {
		this.x += other.x;
		this.y += other.y;
	}

	public double vectorNorm() {
		return FastMath.hypot(x, y);
	}

	public double segmentNorm(Point other) {
		return negateCopy(other).vectorNorm();
	}

	public Point negateCopy(Point other) {
		return new Point(x - other.x, y - other.y);
	}

	public void negate(Point other) {
		this.x -= other.x;
		this.y -= other.y;
	}

	public Point clonePoint() {
		return new Point(x, y);
	}

	public void fixVectorLength(double newLength) {
		double multiplyValue = newLength / FastMath.hypot(x, y);
		x *= multiplyValue;
		y *= multiplyValue;
	}

	@Override
	public String toString() {
		return "Point{" +
				"x=" + x +
				", y=" + y +
				'}';
	}
}
