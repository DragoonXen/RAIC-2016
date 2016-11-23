import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragoon on 15.11.16.
 */
public class ScoreCalcStructure {

	private List<ScoreItem> itemsToApply;

	private double maxScoreDistance;

	public ScoreCalcStructure() {
		itemsToApply = new ArrayList<>();
		maxScoreDistance = 0.;
	}

	public void clear() {
		itemsToApply.clear();
		maxScoreDistance = 0.;
	}

	public void putItem(ScoreItem item) {
		int idx = 0;
		while (idx < itemsToApply.size() && itemsToApply.get(idx).distance > item.distance) {
			++idx;
		}
		itemsToApply.add(idx, item);
		maxScoreDistance = Math.max(maxScoreDistance, item.distance);
	}

	public double getMaxScoreDistance() {
		return maxScoreDistance;
	}

	public void applyScores(ScanMatrixItem item, double distance) {
		for (ScoreItem scoreItem : itemsToApply) {
			if (scoreItem.distance < distance) {
				return;
			}
			scoreItem.applyScore(item);
		}
	}

	public final static ScoreItem EXP_BONUS_APPLYER = new ScoreItem(Constants.EXPERIENCE_DISTANCE) {

		@Override
		public void applyScore(ScanMatrixItem item) {
			item.addExpBonus(this.score);
		}
	};

	public final static ScoreItem MINION_DANGER_APPLYER = new ScoreItem() {

		@Override
		public void applyScore(ScanMatrixItem item) {
			item.addMinionsDanger(this.score);
		}
	};

	public final static ScoreItem ATTACK_BONUS_APPLYER = new ScoreItem() {

		@Override
		public void applyScore(ScanMatrixItem item) {
			item.putAttackBonus(this.score);
		}
	};

	public final static ScoreItem MELEE_ATTACK_BONUS_APPLYER = new ScoreItem() {

		@Override
		public void applyScore(ScanMatrixItem item) {
			item.putMeleeAttackBonus(this.score);
		}
	};

	public final static ScoreItem WIZARDS_DANGER_BONUS_APPLYER = new ScoreItem() {

		@Override
		public void applyScore(ScanMatrixItem item) {
			item.addWizardsDanger(this.score);
		}
	};

	public final static ScoreItem BUILDING_DANGER_BONUS_APPLYER = new ScoreItem() {

		@Override
		public void applyScore(ScanMatrixItem item) {
			item.addBuildingsDanger(this.score);
		}
	};

	public final static ScoreItem OTHER_BONUS_APPLYER = new ScoreItem() {

		@Override
		public void applyScore(ScanMatrixItem item) {
			item.addExpBonus(this.score);
		}
	};

	public final static ScoreItem OTHER_DANGER_APPLYER = new ScoreItem() {

		@Override
		public void applyScore(ScanMatrixItem item) {
			item.addExpBonus(this.score);
		}
	};


	public abstract static class ScoreItem {
		private double distance;

		protected double score;

		public ScoreItem() {
		}

		public ScoreItem(double distance) {
			this.distance = distance;
		}

		public void setDistance(double distance) {
			this.distance = distance;
		}

		public void setScore(double score) {
			this.score = score;
		}

		public abstract void applyScore(ScanMatrixItem item);
	}
}
