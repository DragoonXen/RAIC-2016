import model.Unit;

/**
 * Created by dragoon on 11/13/16.
 */
public class XXX_FastMath {

	public static double hypot(Unit unit, Unit pt) {
		return hypot(unit.getX() - pt.getX(), unit.getY() - pt.getY());
	}

	public static double hypot(Unit unit, XXX_Point pt) {
		return hypot(unit.getX() - pt.getX(), unit.getY() - pt.getY());
	}

	public static double hypot(XXX_Point pt1, XXX_Point pt2) {
		return hypot(pt1.getX() - pt2.getX(), pt1.getY() - pt2.getY());
	}

	public static double hypot(Unit unit, double x, double y) {
		return hypot(unit.getX() - x, unit.getY() - y);
	}

	public static double hypot(final double x, final double y) {
		final int expX = getExponent(x);
		final int expY = getExponent(y);
		if (expX > expY + 27) {
			// y is neglectible with respect to x
			return abs(x);
		} else if (expY > expX + 27) {
			// x is neglectible with respect to y
			return abs(y);
		} else {
			// find an intermediate scale to avoid both overflow and underflow
			final int middleExp = (expX + expY) / 2;

			// scale parameters without losing precision
			final double scaledX = scalb(x, -middleExp);
			final double scaledY = scalb(y, -middleExp);

			// compute scaled hypotenuse
			final double scaledH = Math.sqrt(scaledX * scaledX + scaledY * scaledY);

			// remove scaling
			return scalb(scaledH, middleExp);

		}
	}

	public static int getExponent(final double d) {
		return (int) ((Double.doubleToLongBits(d) >>> 52) & 0x7ff) - 1023;
	}

	public static double abs(double x) {
		return (x < 0.0) ? -x : (x == 0.0) ? 0.0 : x; // -0.0 => +0.0
	}

	public static double scalb(final double d, final int n) {

		// first simple and fast handling when 2^n can be represented using normal numbers
		if ((n > -1023) && (n < 1024)) {
			return d * Double.longBitsToDouble(((long) (n + 1023)) << 52);
		}

		// handle special cases
		if (Double.isNaN(d) || Double.isInfinite(d) || (d == 0)) {
			return d;
		}
		if (n < -2098) {
			return (d > 0) ? 0.0 : -0.0;
		}
		if (n > 2097) {
			return (d > 0) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
		}

		// decompose d
		final long bits = Double.doubleToLongBits(d);
		final long sign = bits & 0x8000000000000000L;
		int exponent = ((int) (bits >>> 52)) & 0x7ff;
		long mantissa = bits & 0x000fffffffffffffL;

		// compute scaled exponent
		int scaledExponent = exponent + n;

		if (n < 0) {
			// we are really in the case n <= -1023
			if (scaledExponent > 0) {
				// both the input and the result are normal numbers, we only adjust the exponent
				return Double.longBitsToDouble(sign | (((long) scaledExponent) << 52) | mantissa);
			} else if (scaledExponent > -53) {
				// the input is a normal number and the result is a subnormal number

				// recover the hidden mantissa bit
				mantissa = mantissa | (1L << 52);

				// scales down complete mantissa, hence losing least significant bits
				final long mostSignificantLostBit = mantissa & (1L << (-scaledExponent));
				mantissa = mantissa >>> (1 - scaledExponent);
				if (mostSignificantLostBit != 0) {
					// we need to add 1 bit to round up the result
					mantissa++;
				}
				return Double.longBitsToDouble(sign | mantissa);

			} else {
				// no need to compute the mantissa, the number scales down to 0
				return (sign == 0L) ? 0.0 : -0.0;
			}
		} else {
			// we are really in the case n >= 1024
			if (exponent == 0) {

				// the input number is subnormal, normalize it
				while ((mantissa >>> 52) != 1) {
					mantissa = mantissa << 1;
					--scaledExponent;
				}
				++scaledExponent;
				mantissa = mantissa & 0x000fffffffffffffL;

				if (scaledExponent < 2047) {
					return Double.longBitsToDouble(sign | (((long) scaledExponent) << 52) | mantissa);
				} else {
					return (sign == 0L) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
				}

			} else if (scaledExponent < 2047) {
				return Double.longBitsToDouble(sign | (((long) scaledExponent) << 52) | mantissa);
			} else {
				return (sign == 0L) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
			}
		}

	}
}
