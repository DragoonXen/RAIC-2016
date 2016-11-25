import model.Projectile;
import model.Wizard;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dragoon on 14.11.16.
 */
public class Variables {

	public static Wizard self;

	public static double moveFactor = 1.;
	public static double turnFactor = 1.;

	public static double staffDamage = 12.;
	public static int magicDamageBonus = 0;

	public static List<AbstractMap.SimpleEntry<Projectile, Double>> projectilesSim = new LinkedList<>();

	public static List<Long> projectiles = new ArrayList<>();

	public static double maxDangerMatrixScore;
}
