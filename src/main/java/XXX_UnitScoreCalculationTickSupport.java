import model.Wizard;

import java.util.HashMap;

/**
 * Created by dragoon on 11/24/16.
 */
public class XXX_UnitScoreCalculationTickSupport {

	private HashMap<Integer, XXX_UnitScoreCalculation> unitScoreCalculations;

	public XXX_UnitScoreCalculationTickSupport(XXX_UnitScoreCalculation current) {
		unitScoreCalculations = new HashMap<>();
		unitScoreCalculations.put(0, current);
	}

	public XXX_UnitScoreCalculation getScores(XXX_FilteredWorld filteredWorld,
											  Wizard self,
											  boolean enemyFound,
											  XXX_AgressiveNeutralsCalcs agressiveCalcs,
											  int addTicks) {
		XXX_UnitScoreCalculation result = unitScoreCalculations.get(addTicks);
		if (result != null) {
			return result;
		}

		result = new XXX_UnitScoreCalculation();
		result.updateScores(filteredWorld, self, enemyFound, agressiveCalcs, addTicks);
		unitScoreCalculations.put(addTicks, result);
		return result;
	}
}
