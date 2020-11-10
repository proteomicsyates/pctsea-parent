package edu.scripps.yates.pctsea.correlation;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.scripps.yates.pctsea.model.SingleCell;

public class CorrelationThreshold {

	private final double correlationThresholdValue;

	public CorrelationThreshold(double correlationThresholdValue) {
		this.correlationThresholdValue = correlationThresholdValue;
	}

	public List<SingleCell> getSingleCellsPassingThreshold(Collection<SingleCell> singleCells) {
		if (correlationThresholdValue > 0.0) {
			return singleCells.stream().filter(cell -> cell.getCorrelation() > correlationThresholdValue)
					.collect(Collectors.toList());
		} else {
			return singleCells.stream().filter(cell -> cell.getCorrelation() < correlationThresholdValue)
					.collect(Collectors.toList());
		}
	}

	public List<SingleCell> getSingleCellsPassingThresholdSortedByCorrelation(Collection<SingleCell> singleCells) {
		if (correlationThresholdValue > 0.0) {
			return singleCells.stream().filter(cell -> cell.getCorrelation() > correlationThresholdValue)
					.sorted(new Comparator<SingleCell>() {
						@Override
						public int compare(SingleCell o1, SingleCell o2) {
							return Double.compare(o2.getCorrelation(), o1.getCorrelation());
						}
					}).collect(Collectors.toList());
		} else {
			return singleCells.stream().filter(cell -> cell.getCorrelation() < correlationThresholdValue)
					.sorted(new Comparator<SingleCell>() {
						@Override
						public int compare(SingleCell o1, SingleCell o2) {
							return Double.compare(o1.getCorrelation(), o2.getCorrelation());
						}
					}).collect(Collectors.toList());
		}
	}

	public long getCountSingleCellsPassingThreshold(Collection<SingleCell> singleCellList) {
		if (correlationThresholdValue > 0.0) {
			final Stream<SingleCell> filter = singleCellList.stream()
					.filter(sc -> sc.getCorrelation() > correlationThresholdValue);
			final int ret = filter.collect(Collectors.toSet()).size();
//			ret= filter.count();
			return ret;
		} else {
			return singleCellList.stream().filter(sc -> sc.getCorrelation() < correlationThresholdValue).count();
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

	public boolean passThreshold(SingleCell singleCell) {
		if (correlationThresholdValue > 0.0) {
			return singleCell.getCorrelation() > correlationThresholdValue;
		} else {
			return singleCell.getCorrelation() < correlationThresholdValue;
		}
	}

	public double getThresholdValue() {
		return this.correlationThresholdValue;
	}

	/**
	 * Sorts the list taking into account the correlation threshold, that is, if the
	 * threshold is negative, the top single cells will be the ones with lower (more
	 * negative correlation).
	 * 
	 * @param singleCellList
	 */
	public void sortSingleCellsByCorrelation(List<SingleCell> singleCellList) {

		Collections.sort(singleCellList, new Comparator<SingleCell>() {

			@Override
			public int compare(SingleCell o1, SingleCell o2) {
				// sort by correlation from higher to lower

				if (correlationThresholdValue > 0.0) {
					final double corr1 = Double.isNaN(o1.getCorrelation()) ? -Double.MAX_VALUE : o1.getCorrelation();
					final double corr2 = Double.isNaN(o2.getCorrelation()) ? -Double.MAX_VALUE : o2.getCorrelation();
					return Double.compare(corr2, corr1);
				} else {
					// sort by correlation from lower to higher
					final double corr1 = Double.isNaN(o1.getCorrelation()) ? Double.MAX_VALUE : o1.getCorrelation();
					final double corr2 = Double.isNaN(o2.getCorrelation()) ? Double.MAX_VALUE : o2.getCorrelation();
					return Double.compare(corr1, corr2);
				}
			}
		});
	}
}
