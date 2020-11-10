package edu.scripps.yates.pctsea.correlation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import edu.scripps.yates.pctsea.model.SingleCell;

public class NoCorrelationThreshold extends CorrelationThreshold {

	public NoCorrelationThreshold() {
		super(Double.NaN);

	}

	@Override
	public List<SingleCell> getSingleCellsPassingThreshold(Collection<SingleCell> singleCells) {
		return singleCells.stream().collect(Collectors.toList());
	}

	@Override
	public List<SingleCell> getSingleCellsPassingThresholdSortedByCorrelation(Collection<SingleCell> singleCells) {
		return singleCells.stream().collect(Collectors.toList());
	}

	@Override
	public long getCountSingleCellsPassingThreshold(Collection<SingleCell> singleCellList) {
		return singleCellList.size();
	}

	@Override
	public String toString() {
		return "No correlation threshold";
	}

	@Override
	public boolean passThreshold(SingleCell singleCell) {
		return true;
	}

	@Override
	public void sortSingleCellsByCorrelation(List<SingleCell> singleCellList) {
		return;
	}

}
