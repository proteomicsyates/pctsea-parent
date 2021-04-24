package edu.scripps.yates.pctsea.correlation;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.scripps.yates.pctsea.model.SingleCell;

public class CorrelationThreshold implements ScoreThreshold {

	private final double correlationThresholdValue;

	public CorrelationThreshold(double correlationThresholdValue) {
		this.correlationThresholdValue = correlationThresholdValue;
	}

	@Override
	public List<SingleCell> getSingleCellsPassingThreshold(Collection<SingleCell> singleCells) {
		if (correlationThresholdValue > 0.0) {
			return singleCells.stream().filter(cell -> cell.getScoreForRanking() > correlationThresholdValue)
					.collect(Collectors.toList());
		} else {
			return singleCells.stream().filter(cell -> cell.getScoreForRanking() < correlationThresholdValue)
					.collect(Collectors.toList());
		}
	}

	@Override
	public List<SingleCell> getSingleCellsPassingThresholdSortedByScore(Collection<SingleCell> singleCells) {
		if (correlationThresholdValue > 0.0) {
			return singleCells.stream().filter(cell -> cell.getScoreForRanking() > correlationThresholdValue)
					.sorted(new Comparator<SingleCell>() {
						@Override
						public int compare(SingleCell o1, SingleCell o2) {
							return Double.compare(o2.getScoreForRanking(), o1.getScoreForRanking());
						}
					}).collect(Collectors.toList());
		} else {
			return singleCells.stream().filter(cell -> cell.getScoreForRanking() < correlationThresholdValue)
					.sorted(new Comparator<SingleCell>() {
						@Override
						public int compare(SingleCell o1, SingleCell o2) {
							return Double.compare(o1.getScoreForRanking(), o2.getScoreForRanking());
						}
					}).collect(Collectors.toList());
		}
	}

	@Override
	public long getCountSingleCellsPassingThreshold(Collection<SingleCell> singleCellList) {
		if (correlationThresholdValue > 0.0) {
			final Stream<SingleCell> filter = singleCellList.stream()
					.filter(sc -> sc.getScoreForRanking() > correlationThresholdValue);
			final int ret = filter.collect(Collectors.toSet()).size();
//			ret= filter.count();
			return ret;
		} else {
			return singleCellList.stream().filter(sc -> sc.getScoreForRanking() < correlationThresholdValue).count();
		}
	}

	@Override
	public String toString() {
		String sign = ">";
		if (correlationThresholdValue < 0.0) {
			sign = "<";
		}
		return sign + " " + correlationThresholdValue;
	}

	@Override
	public boolean passThreshold(SingleCell singleCell) {
		if (correlationThresholdValue > 0.0) {
			return singleCell.getScoreForRanking() > correlationThresholdValue;
		} else {
			return singleCell.getScoreForRanking() < correlationThresholdValue;
		}
	}

	@Override
	public double getThresholdValue() {
		return correlationThresholdValue;
	}

	/**
	 * Sorts the list taking into account the correlation threshold, that is, if the
	 * threshold is negative, the top single cells will be the ones with lower (more
	 * negative correlation).
	 * 
	 * @param singleCellList
	 */
	@Override
	public void sortSingleCellsByScore(List<SingleCell> singleCellList) {

		Collections.sort(singleCellList, new Comparator<SingleCell>() {

			@Override
			public int compare(SingleCell o1, SingleCell o2) {
				// sort by correlation from higher to lower

				if (correlationThresholdValue > 0.0) {
					final double corr1 = Double.isNaN(o1.getScoreForRanking()) ? -Double.MAX_VALUE
							: o1.getScoreForRanking();
					final double corr2 = Double.isNaN(o2.getScoreForRanking()) ? -Double.MAX_VALUE
							: o2.getScoreForRanking();
					return Double.compare(corr2, corr1);
				} else {
					// sort by correlation from lower to higher
					final double corr1 = Double.isNaN(o1.getScoreForRanking()) ? Double.MAX_VALUE
							: o1.getScoreForRanking();
					final double corr2 = Double.isNaN(o2.getScoreForRanking()) ? Double.MAX_VALUE
							: o2.getScoreForRanking();
					return Double.compare(corr1, corr2);
				}
			}
		});
	}
}
