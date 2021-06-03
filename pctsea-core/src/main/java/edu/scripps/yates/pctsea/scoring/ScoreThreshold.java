package edu.scripps.yates.pctsea.scoring;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.scripps.yates.pctsea.model.SingleCell;

public class ScoreThreshold implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1813446153437834176L;
	private double threshold;

	public ScoreThreshold() {

	}

	public ScoreThreshold(double correlationThresholdValue) {
		threshold = correlationThresholdValue;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public List<SingleCell> getSingleCellsPassingThreshold(Collection<SingleCell> singleCells) {
		if (threshold >= 0.0) {
			return singleCells.stream().filter(cell -> cell.getScoreForRanking() >= threshold)
					.collect(Collectors.toList());
		} else {
			return singleCells.stream().filter(cell -> cell.getScoreForRanking() < threshold)
					.collect(Collectors.toList());
		}
	}

	public int countSingleCellsPassingThreshold(Collection<SingleCell> singleCells) {
		if (threshold >= 0.0) {
			return Long.valueOf(singleCells.stream().filter(cell -> cell.getScoreForRanking() >= threshold).count())
					.intValue();
		} else {
			return Long.valueOf(singleCells.stream().filter(cell -> cell.getScoreForRanking() < threshold).count())
					.intValue();
		}
	}

	public List<SingleCell> getSingleCellsPassingThresholdSortedByScore(Collection<SingleCell> singleCells) {
		if (threshold >= 0.0) {
			return singleCells.stream().filter(cell -> cell.getScoreForRanking() >= threshold)
					.sorted(new Comparator<SingleCell>() {
						@Override
						public int compare(SingleCell o1, SingleCell o2) {
							return Double.compare(o2.getScoreForRanking(), o1.getScoreForRanking());
						}
					}).collect(Collectors.toList());
		} else {
			return singleCells.stream().filter(cell -> cell.getScoreForRanking() < threshold)
					.sorted(new Comparator<SingleCell>() {
						@Override
						public int compare(SingleCell o1, SingleCell o2) {
							return Double.compare(o1.getScoreForRanking(), o2.getScoreForRanking());
						}
					}).collect(Collectors.toList());
		}
	}

	public long getCountSingleCellsPassingThreshold(Collection<SingleCell> singleCellList) {
		if (threshold >= 0.0) {
			final Stream<SingleCell> filter = singleCellList.stream()
					.filter(sc -> sc.getScoreForRanking() >= threshold);
			final int ret = filter.collect(Collectors.toSet()).size();
//			ret= filter.count();
			return ret;
		} else {
			return singleCellList.stream().filter(sc -> sc.getScoreForRanking() < threshold).count();
		}
	}

	@Override
	public String toString() {
		String sign = ">";
		if (threshold < 0.0) {
			sign = "<";
		}
		return sign + " " + threshold;
	}

	public boolean passThreshold(SingleCell singleCell) {
		if (threshold >= 0.0) {
			final boolean ret = singleCell.getScoreForRanking() >= threshold;
			return ret;
		} else {
			final boolean ret2 = singleCell.getScoreForRanking() < threshold;
			return ret2;
		}
	}

	public double getThresholdValue() {
		return threshold;
	}

	/**
	 * Sorts the list taking into account the correlation threshold, that is, if the
	 * threshold is negative, the top single cells will be the ones with lower (more
	 * negative correlation).
	 * 
	 * @param singleCellList
	 */

	public void sortSingleCellsByScore(List<SingleCell> singleCellList) {

		Collections.sort(singleCellList, new Comparator<SingleCell>() {

			@Override
			public int compare(SingleCell o1, SingleCell o2) {
				// sort by correlation from higher to lower

				if (threshold >= 0.0) {
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
