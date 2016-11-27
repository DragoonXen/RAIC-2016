import model.Unit;

/**
 * Created by dragoon on 11/10/16.
 */
public class XXX_Point {

	protected double x;
	protected double y;

	public final static XXX_Point EMPTY_POINT = new XXX_Point();

	public XXX_Point() {
	}

	public XXX_Point(double x, double y) {
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

	public void update(XXX_Point point) {
		update(point.getX(), point.getY());
	}

	public double scalarMult(XXX_Point other) {
		return x * other.x + y * other.y;
	}

	public XXX_Point mult(double value) {
		return new XXX_Point(x * value, y * value);
	}

	public XXX_Point addWithCopy(XXX_Point other) {
		return new XXX_Point(x + other.x, y + other.y);
	}

	public void add(XXX_Point other) {
		this.x += other.x;
		this.y += other.y;
	}

	public double vectorNorm() {
		return XXX_FastMath.hypot(x, y);
	}

	public double segmentNorm(XXX_Point other) {
		return negateCopy(other).vectorNorm();
	}

	public XXX_Point negateCopy(Unit other) {
		return new XXX_Point(x - other.getX(), y - other.getY());
	}

	public XXX_Point negateCopy(XXX_Point other) {
		return new XXX_Point(x - other.x, y - other.y);
	}

	public void negate(XXX_Point other) {
		this.x -= other.x;
		this.y -= other.y;
	}

	public XXX_Point clonePoint() {
		return new XXX_Point(x, y);
	}

	public void fixVectorLength(double newLength) {
		double multiplyValue = newLength / XXX_FastMath.hypot(x, y);
		x *= multiplyValue;
		y *= multiplyValue;
	}

	@Override
	public String toString() {
		return "XXX_Point{" +
				"x=" + x +
				", y=" + y +
				'}';
	}
}
