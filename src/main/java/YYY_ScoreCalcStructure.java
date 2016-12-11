import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragoon on 15.11.16.
 */
public class YYY_ScoreCalcStructure {

	private List<ScoreItem> itemsToApply;

	private double maxScoreDistance;

	public YYY_ScoreCalcStructure() {
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

	public void applyScores(YYY_ScanMatrixItem item, double distance) {
		for (ScoreItem scoreItem : itemsToApply) {
			if (scoreItem.distance < distance) {
				return;
			}
			scoreItem.applyScore(item);
		}
	}

	public static ScoreItem createExpBonusApplyer(double score) {
		return new ScoreItem(YYY_Constants.EXPERIENCE_DISTANCE, score) {
			@Override
			public void applyScore(YYY_ScanMatrixItem item) {
				item.addExpBonus(this.score);
			}
		};
	}

	public static ScoreItem createExpBonusApplyer(double distance, double score) {
		return new ScoreItem(distance, score) {
			@Override
			public void applyScore(YYY_ScanMatrixItem item) {
				item.addExpBonus(this.score);
			}
		};
	}

	public static ScoreItem createMinionDangerApplyer(double distance, double score) {
		return new ScoreItem(distance, score) {
			@Override
			public void applyScore(YYY_ScanMatrixItem item) {
				item.addMinionsDanger(this.score);
			}
		};
	}

	public static ScoreItem createAttackBonusApplyer(double distance, double score) {
		return new ScoreItem(distance, score) {
			@Override
			public void applyScore(YYY_ScanMatrixItem item) {
				item.putAttackBonus(this.score);
			}
		};
	}

	public static ScoreItem createMeleeAttackBonusApplyer(double distance, double score) {
		return new ScoreItem(distance, score) {
			@Override
			public void applyScore(YYY_ScanMatrixItem item) {
				item.putMeleeAttackBonus(this.score);
			}
		};
	}

	public static ScoreItem createWizardsDangerApplyer(double distance, double score) {
		return new ScoreItem(distance, score) {
			@Override
			public void applyScore(YYY_ScanMatrixItem item) {
				item.addWizardsDanger(this.score);
			}
		};
	}

	public static ScoreItem createBuildingDangerApplyer(double distance, double score) {
		return new ScoreItem(distance, score) {
			@Override
			public void applyScore(YYY_ScanMatrixItem item) {
				item.addBuildingsDanger(this.score);
			}
		};
	}

	public static ScoreItem createOtherBonusApplyer(double distance, double score) {
		return new ScoreItem(distance, score) {
			@Override
			public void applyScore(YYY_ScanMatrixItem item) {
				item.addOtherBonus(this.score);
			}
		};
	}

	public static ScoreItem createOtherDangerApplyer(double distance, double score) {
		return new ScoreItem(distance, score) {
			@Override
			public void applyScore(YYY_ScanMatrixItem item) {
				item.addOtherDanger(this.score);
			}
		};
	}


	public abstract static class ScoreItem {

		private double distance;
		protected double score;

		public ScoreItem() {
		}

		public ScoreItem(double distance) {
			this.distance = distance;
		}

		public ScoreItem(double distance, double score) {
			this.distance = distance;
			this.score = score;
		}

		public void setDistance(double distance) {
			this.distance = distance;
		}

		public void setScore(double score) {
			this.score = score;
		}

		public double getScore() {
			return score;
		}

		public abstract void applyScore(YYY_ScanMatrixItem item);
	}
}
