package edu.scripps.yates.pctsea.correlation;

import java.util.Collection;
import java.util.List;

import edu.scripps.yates.pctsea.model.SingleCell;

public interface ScoreThreshold {

	public void sortSingleCellsByScore(List<SingleCell> singleCellList);

	public double getThresholdValue();

	public boolean passThreshold(SingleCell singleCell);

	public long getCountSingleCellsPassingThreshold(Collection<SingleCell> singleCellList);

	public List<SingleCell> getSingleCellsPassingThresholdSortedByScore(Collection<SingleCell> singleCells);

	public List<SingleCell> getSingleCellsPassingThreshold(Collection<SingleCell> singleCells);

}
