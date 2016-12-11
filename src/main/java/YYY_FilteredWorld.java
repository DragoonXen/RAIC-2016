import model.Bonus;
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
public class YYY_FilteredWorld extends World {

	private List<CircularUnit> allBlocksList;

	private List<Tree> shootingTreeList;

	private YYY_BuildingPhantom[] castedBuildings;

	public static YYY_FilteredWorld lastInstance;

	public YYY_FilteredWorld(int tickIndex,
							 int tickCount,
							 double width,
							 double height,
							 Player[] players,
							 List<Wizard> wizards,
							 List<Minion> minions,
							 List<Projectile> projectiles,
							 List<Bonus> bonuses,
							 List<YYY_BuildingPhantom> buildings,
							 List<Tree> trees,
							 List<Tree> shootingTreeList,
							 YYY_Point point) {
		super(tickIndex,
			  tickCount,
			  width,
			  height,
			  players,
			  wizards.toArray(new Wizard[wizards.size()]),
			  minions.toArray(new Minion[minions.size()]),
			  projectiles.toArray(new Projectile[projectiles.size()]),
			  bonuses.toArray(new Bonus[bonuses.size()]),
			  buildings.toArray(new YYY_BuildingPhantom[buildings.size()]),
			  trees.toArray(new Tree[trees.size()]));
		this.castedBuildings = (YYY_BuildingPhantom[]) super.getBuildings();
		this.shootingTreeList = shootingTreeList;
		allBlocksList = new ArrayList<>();
		allBlocksList.addAll(YYY_Utils.filterUnit(getWizards(), point, FilterType.MOVE));
		allBlocksList.addAll(YYY_Utils.filterUnit(getMinions(), point, FilterType.MOVE));
		allBlocksList.addAll(YYY_Utils.filterUnit(getBuildings(), point, FilterType.MOVE));
		allBlocksList.addAll(YYY_Utils.filterUnit(getTrees(), point, FilterType.MOVE));
		lastInstance = this;
	}

	public List<CircularUnit> getAllBlocksList() {
		return allBlocksList;
	}

	@Override
	public YYY_BuildingPhantom[] getBuildings() {
		return castedBuildings;
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
