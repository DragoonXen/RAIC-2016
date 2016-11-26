/**
 * Created by dragoon on 11/17/16.
 */
public class AccAndSpeedWithFix {

	private double speed;
	private double strafe;
	private double fix;

	public AccAndSpeedWithFix(double speed, double strafe, double fix) {
		this.speed = speed;
		this.strafe = strafe;
		this.fix = fix;
	}

	public double getSpeed() {
		return speed;
	}

	public double getStrafe() {
		return strafe;
	}

	public double getFix() {
		return fix;
	}

	public Point getCoordChange(double selfAngle) {
		return new Point(Math.cos(selfAngle) * speed + Math.cos(selfAngle + Math.PI / 2.) * strafe,
						 Math.sin(selfAngle) * speed + Math.sin(selfAngle + Math.PI / 2.) * strafe);
	}

	public static AccAndSpeedWithFix getAccAndSpeedByAngle(double angle, double distance) {
		return getAccAndSpeedByAngle(angle, distance, Variables.moveFactor);
	}

	public static AccAndSpeedWithFix getAccAndSpeedByAngle(double angle, double distance, double moveFactor) {
		double strafe = Math.sin(angle) * distance;
		double acc = Math.cos(angle) * distance;
		double fwdLimit = (acc > 0 ? Constants.getGame().getWizardForwardSpeed() : Constants.getGame().getWizardBackwardSpeed()) * moveFactor;

		double fix = Math.hypot(acc / fwdLimit, strafe / (Constants.getGame().getWizardStrafeSpeed() * moveFactor));
		if (fix > 1.) {
			return new AccAndSpeedWithFix(acc / fix, strafe / fix, fix);
		} else {
			return new AccAndSpeedWithFix(acc, strafe, fix);
		}
	}
}
