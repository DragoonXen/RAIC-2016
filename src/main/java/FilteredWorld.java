import model.Bonus;
import model.Building;
import model.CircularUnit;
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

	private List<Tree> shootingTreeList;

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
						 List<Tree> shootingTreeList,
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
		this.shootingTreeList = shootingTreeList;
		allBlocksList = new ArrayList<>();
		allBlocksList.addAll(Utils.filterUnit(getWizards(), point, FilterType.MOVE));
		allBlocksList.addAll(Utils.filterUnit(getMinions(), point, FilterType.MOVE));
		allBlocksList.addAll(Utils.filterUnit(getBuildings(), point, FilterType.MOVE));
		allBlocksList.addAll(Utils.filterUnit(getTrees(), point, FilterType.MOVE));
	}

	public List<CircularUnit> getAllBlocksList() {
		return allBlocksList;
	}

	public List<Tree> getShootingTreeList() {
		return shootingTreeList;
	}

	public enum FilterType {
		FIGHT,
		MOVE,
		AIM,
		AIM_OBSTACLE
	}
}
