import model.Wizard;

import java.util.HashMap;

/**
 * Created by dragoon on 11/24/16.
 */
public class YYY_UnitScoreCalculationTickSupport {

	private HashMap<Integer, YYY_UnitScoreCalculation> unitScoreCalculations;

	public YYY_UnitScoreCalculationTickSupport(YYY_UnitScoreCalculation current) {
		unitScoreCalculations = new HashMap<>();
		unitScoreCalculations.put(0, current);
	}

	public YYY_UnitScoreCalculation getScores(YYY_FilteredWorld filteredWorld,
											  Wizard self,
											  YYY_FightStatus status,
											  YYY_AgressiveNeutralsCalcs agressiveCalcs,
											  int addTicks) {
		YYY_UnitScoreCalculation result = unitScoreCalculations.get(addTicks);
		if (result != null) {
			return result;
		}

		result = new YYY_UnitScoreCalculation();
		result.updateScores(filteredWorld, self, status, agressiveCalcs, addTicks);
		unitScoreCalculations.put(addTicks, result);
		return result;
	}
}
