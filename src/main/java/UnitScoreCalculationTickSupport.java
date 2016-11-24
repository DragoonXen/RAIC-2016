import model.Wizard;

import java.util.HashMap;

/**
 * Created by dragoon on 11/24/16.
 */
public class UnitScoreCalculationTickSupport {

	private HashMap<Integer, UnitScoreCalculation> unitScoreCalculations;

	public UnitScoreCalculationTickSupport(UnitScoreCalculation current) {
		unitScoreCalculations = new HashMap<>();
		unitScoreCalculations.put(0, current);
	}

	public UnitScoreCalculation getScores(FilteredWorld filteredWorld, Wizard self, boolean enemyFound, AgressiveNeutralsCalcs agressiveCalcs, int addTicks) {
		UnitScoreCalculation result = unitScoreCalculations.get(addTicks);
		if (result != null) {
			return result;
		}

		result = new UnitScoreCalculation();
		result.updateScores(filteredWorld, self, enemyFound, agressiveCalcs, addTicks);
		unitScoreCalculations.put(addTicks, result);
		return result;
	}
}
