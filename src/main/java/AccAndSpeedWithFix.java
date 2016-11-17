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
}
