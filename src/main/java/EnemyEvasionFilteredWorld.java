import model.Building;
import model.Minion;
import model.Tree;
import model.Wizard;
import model.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragoon on 12/4/16.
 */
public class EnemyEvasionFilteredWorld {

	private List<Tree> treesEvasionCalc;
	private List<Minion> minionsEvasionCalc;
	private List<Wizard> wizardsEvasionCalc;
	private List<Building> buildingsEvasionCalc;

	public EnemyEvasionFilteredWorld(Wizard whichWizardCheck, World world) {
		treesEvasionCalc = new ArrayList<>();
		minionsEvasionCalc = new ArrayList<>();
		wizardsEvasionCalc = new ArrayList<>();
		buildingsEvasionCalc = new ArrayList<>();

		double maxDistance = whichWizardCheck.getRadius() + Constants.getGame().getMinionRadius() + 200.;
		for (Minion minion : world.getMinions()) {
			if (FastMath.hypot(minion, whichWizardCheck) < maxDistance) {
				minionsEvasionCalc.add(minion);
			}
		}
		maxDistance = whichWizardCheck.getRadius() * 2. + 200.;
		for (Wizard wizard : world.getWizards()) {
			if (wizard == whichWizardCheck) {
				continue;
			}
			if (FastMath.hypot(wizard, whichWizardCheck) < maxDistance) {
				wizardsEvasionCalc.add(wizard);
			}
		}
		maxDistance = whichWizardCheck.getRadius() + 200.;
		for (Building building : world.getBuildings()) {
			if (FastMath.hypot(building, whichWizardCheck) < maxDistance + building.getRadius()) {
				buildingsEvasionCalc.add(building);
			}
		}
		for (Tree tree : world.getTrees()) {
			if (FastMath.hypot(tree, whichWizardCheck) < maxDistance + tree.getRadius()) {
				treesEvasionCalc.add(tree);
			}
		}
	}

	public List<Tree> getTreesEvasionCalc() {
		return treesEvasionCalc;
	}

	public List<Minion> getMinionsEvasionCalc() {
		return minionsEvasionCalc;
	}

	public List<Wizard> getWizardsEvasionCalc() {
		return wizardsEvasionCalc;
	}

	public List<Building> getBuildingsEvasionCalc() {
		return buildingsEvasionCalc;
	}
}
