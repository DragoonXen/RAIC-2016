import model.Bonus;
import model.Building;
import model.CircularUnit;
import model.LivingUnit;
import model.Minion;
import model.Player;
import model.Projectile;
import model.Tree;
import model.Wizard;
import model.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragoon on 11/12/16.
 */
public class FilteredWorld extends World {

	private List<CircularUnit> allBlocksList;

	private List<LivingUnit> aimsList;

	public FilteredWorld(int tickIndex,
						 int tickCount,
						 double width,
						 double height,
						 Player[] players,
						 List<Wizard> wizards,
						 List<Minion> minions,
						 List<Projectile> projectiles,
						 List<Bonus> bonuses,
						 List<Building> buildings,
						 List<Tree> trees,
						 Point point) {
		super(tickIndex,
			  tickCount,
			  width,
			  height,
			  players,
			  wizards.toArray(new Wizard[wizards.size()]),
			  minions.toArray(new Minion[minions.size()]),
			  projectiles.toArray(new Projectile[projectiles.size()]),
			  bonuses.toArray(new Bonus[bonuses.size()]),
			  buildings.toArray(new Building[buildings.size()]),
			  trees.toArray(new Tree[trees.size()]));
		allBlocksList = new ArrayList<>();
		allBlocksList.addAll(Utils.filterUnit(getWizards(), point, FilterType.MOVE));
		allBlocksList.addAll(Utils.filterUnit(getMinions(), point, FilterType.MOVE));
		allBlocksList.addAll(Utils.filterUnit(getBuildings(), point, FilterType.MOVE));
		allBlocksList.addAll(Utils.filterUnit(getTrees(), point, FilterType.MOVE));
		aimsList = new ArrayList<>();
		Point aimFilterPoint = new Point(Variables.self.getX(), Variables.self.getY());
		aimsList.addAll(Utils.filterUnit(getWizards(), aimFilterPoint, FilterType.AIM));
		aimsList.addAll(Utils.filterUnit(getMinions(), aimFilterPoint, FilterType.AIM));
		aimsList.addAll(Utils.filterUnit(getBuildings(), aimFilterPoint, FilterType.AIM));
		aimsList.addAll(Utils.filterUnit(getTrees(), aimFilterPoint, FilterType.AIM));
	}

	public List<CircularUnit> getAllBlocksList() {
		return allBlocksList;
	}

	public List<LivingUnit> getAimsList() {
		return aimsList;
	}

	public static enum FilterType {
		FIGHT,
		MOVE,
		AIM
	}
}
