package edu.scripps.yates.pctsea.model;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class SingleCellSet {
	private final TIntObjectMap<SingleCell> singleCellsByCellID = new TIntObjectHashMap<SingleCell>();
	private final List<SingleCell> singleCellList = new ArrayList<SingleCell>();
	private final TObjectIntMap<String> singleCellIDsBySingleCellNameMap = new TObjectIntHashMap<String>();
	private final TIntList cellIDs = new TIntArrayList();
	private int totalNumCellsForDataset;

	public SingleCellSet() {

	}

	/**
	 * NOTE THAT CAN RETURN -1 if the cell is not found because it was ignored from
	 * the db because it didnt have a type
	 * 
	 * @param name
	 * @return
	 */
	public int getSingleCellIDBySingleCellName(String name) {
		final int id = singleCellIDsBySingleCellNameMap.get(name);
		if (id > 0) {

			return id;
		} else {
			// if it is not found it is because that single cell has not been classified,
			// doesnt have a type and was ignored from the database, therefore, we return
			// -1 here
			return -1;
//			int cellID = 1;
//			if (!cellIDs.isEmpty()) {
//				cellID = cellIDs.max() + 1;
//			}
////			statusListener.onStatusUpdate("Why cell " + name + " was not found before in the DB?",LogLevel.WARN);
//			final SingleCell cell = new SingleCell(cellID, name, Double.NaN);
//			addSingleCell(cell);
//			return cellID;
		}
	}

	public void addSingleCell(SingleCell singleCell) {
		addSingleCellIDBySingleCellName(singleCell.getName(), singleCell.getId());
		singleCellList.add(singleCell);
		totalNumCellsForDataset++;
		singleCellsByCellID.put(singleCell.getId(), singleCell);
	}

	private void addSingleCellIDBySingleCellName(String name, int id) {
		singleCellIDsBySingleCellNameMap.put(name, id);
		cellIDs.add(id);
	}

	public int getNumSingleCells() {
		return totalNumCellsForDataset;
	}

	public void clearInformation(boolean clearTotalNumber) {
		singleCellIDsBySingleCellNameMap.clear();
		singleCellList.clear();
		singleCellsByCellID.clear();
		if (clearTotalNumber) {
			totalNumCellsForDataset = 0;
		}
	}

	public TIntObjectMap<SingleCell> getSingleCellsByCellID() {
		return singleCellsByCellID;
	}

	public List<SingleCell> getSingleCellList() {
		return singleCellList;
	}

	public TObjectIntMap<String> getSingleCellIDsBySingleCellNameMap() {
		return singleCellIDsBySingleCellNameMap;
	}

	public TIntList getCellIDs() {
		return cellIDs;
	}

	public int getTotalNumCellsForDataset() {
		return totalNumCellsForDataset;
	}

	public SingleCell getSingleCellByCellID(int cellID) {
		return singleCellsByCellID.get(cellID);
	}
}
