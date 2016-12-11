import model.Unit;

/**
 * Created by dragoon on 11/10/16.
 */
public class YYY_Point {

	protected double x;
	protected double y;

	public final static YYY_Point EMPTY_POINT = new YYY_Point();

	public YYY_Point() {
	}

	public YYY_Point(double x, double y) {
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

	public void update(YYY_Point point) {
		update(point.getX(), point.getY());
	}

	public double scalarMult(YYY_Point other) {
		return x * other.x + y * other.y;
	}

	public YYY_Point mult(double value) {
		return new YYY_Point(x * value, y * value);
	}

	public YYY_Point addWithCopy(YYY_Point other) {
		return new YYY_Point(x + other.x, y + other.y);
	}

	public void add(YYY_Point other) {
		this.x += other.x;
		this.y += other.y;
	}

	public double vectorNorm() {
		return YYY_FastMath.hypot(x, y);
	}

	public double segmentNorm(YYY_Point other) {
		return negateCopy(other).vectorNorm();
	}

	public YYY_Point negateCopy(Unit other) {
		return new YYY_Point(x - other.getX(), y - other.getY());
	}

	public YYY_Point negateCopy(YYY_Point other) {
		return new YYY_Point(x - other.x, y - other.y);
	}

	public void negate(YYY_Point other) {
		this.x -= other.x;
		this.y -= other.y;
	}

	public YYY_Point clonePoint() {
		return new YYY_Point(x, y);
	}

	public void fixVectorLength(double newLength) {
		double multiplyValue = newLength / YYY_FastMath.hypot(x, y);
		x *= multiplyValue;
		y *= multiplyValue;
	}

	@Override
	public String toString() {
		return "YYY_Point{" +
				"x=" + x +
				", y=" + y +
				'}';
	}
}
