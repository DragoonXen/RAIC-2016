/**
 * Created by dragoon on 11/17/16.
 */
public class XXX_AccAndSpeedWithFix {

	private double speed;
	private double strafe;
	private double fix;

	public XXX_AccAndSpeedWithFix(double speed, double strafe, double fix) {
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

	public XXX_Point getCoordChange(double selfAngle) {
		return new XXX_Point(Math.cos(selfAngle) * speed + Math.cos(selfAngle + Math.PI / 2.) * strafe,
							 Math.sin(selfAngle) * speed + Math.sin(selfAngle + Math.PI / 2.) * strafe);
	}

	public static XXX_AccAndSpeedWithFix getAccAndSpeedByAngle(double angle, double distance) {
		return getAccAndSpeedByAngle(angle, distance, XXX_Variables.moveFactor);
	}

	public static XXX_AccAndSpeedWithFix getAccAndSpeedByAngle(double angle, double distance, double moveFactor) {
		double strafe = Math.sin(angle) * distance;
		double acc = Math.cos(angle) * distance;
		double fwdLimit = (acc > 0 ? XXX_Constants.getGame().getWizardForwardSpeed() : XXX_Constants.getGame().getWizardBackwardSpeed()) * moveFactor;

		double fix = Math.hypot(acc / fwdLimit, strafe / (XXX_Constants.getGame().getWizardStrafeSpeed() * moveFactor));
		if (fix > 1.) {
			return new XXX_AccAndSpeedWithFix(acc / fix, strafe / fix, fix);
		} else {
			return new XXX_AccAndSpeedWithFix(acc, strafe, fix);
		}
	}
}
