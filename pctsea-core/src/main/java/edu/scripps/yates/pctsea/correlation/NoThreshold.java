package edu.scripps.yates.pctsea.correlation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import edu.scripps.yates.pctsea.model.SingleCell;

public class NoThreshold implements ScoreThreshold {

	public NoThreshold() {

	}

	@Override
	public List<SingleCell> getSingleCellsPassingThreshold(Collection<SingleCell> singleCells) {
		if (singleCells instanceof List) {
			return (List<SingleCell>) singleCells;
		}
		return singleCells.stream().collect(Collectors.toList());
	}

	@Override
	public List<SingleCell> getSingleCellsPassingThresholdSortedByScore(Collection<SingleCell> singleCells) {
		if (singleCells instanceof List) {
			return (List<SingleCell>) singleCells;
		}
		return singleCells.stream().collect(Collectors.toList());
	}

	@Override
	public long getCountSingleCellsPassingThreshold(Collection<SingleCell> singleCellList) {
		return singleCellList.size();
	}

	@Override
	public String toString() {
		return "No threshold";
	}

	@Override
	public boolean passThreshold(SingleCell singleCell) {
		return true;
	}

	@Override
	public void sortSingleCellsByScore(List<SingleCell> singleCellList) {
		return;
	}

	@Override
	public double getThresholdValue() {
		return Double.NaN;
	}

}
