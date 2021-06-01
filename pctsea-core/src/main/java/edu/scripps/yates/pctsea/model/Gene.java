package edu.scripps.yates.pctsea.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import edu.scripps.yates.pctsea.utils.SingleCellsMetaInformationReader;
import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * Normalized count expression values of a gene in different single cells
 * 
 * @author salvador
 *
 */
public class Gene {
	private final static Logger log = Logger.getLogger(Gene.class);
	private final String geneName;
	private final TIntFloatMap expressionsBySingleCellID = new TIntFloatHashMap();
	private final TObjectIntMap<String> numCellsByCellType = new TObjectIntHashMap<String>();
	private List<Integer> singleCellsIDs;
	private TFloatList expressions;
	private List<Integer> indexes;
	private final short geneID;

//	public Gene(String geneName) {
//		this(++staticGeneID, geneName);
//	}

	public Gene(short geneID, String geneName) {
		this.geneID = geneID;
		this.geneName = geneName;

	}

	public void addExpressionValueInSingleCell(int singleCellID, float expressionValue, String cellTypeName) {

		if (expressionsBySingleCellID.containsKey(singleCellID)) {
			final float expressionPrevious = expressionsBySingleCellID.get(singleCellID);
			if (Float.compare(expressionPrevious, expressionValue) != 0) {
				// keep the highest
				if (expressionPrevious < expressionValue) {
//					PCTSEA.logStatus("Gene " + geneName + " already contained an expression value for single cell "
//							+ singleCellID + " ("
//							+ SingleCellsMetaInformationReader.getSingleCellByCellID(singleCellID).getName()
//							+ ") which is " + expressionPrevious
//							+ " now it will be updated to the highest number");
				} else {
					return;
				}
			}
		}
		expressionsBySingleCellID.put(singleCellID, expressionValue);
		if (!numCellsByCellType.containsKey(cellTypeName)) {
			numCellsByCellType.put(cellTypeName, 0);
		}
		numCellsByCellType.put(cellTypeName, numCellsByCellType.get(cellTypeName) + 1);
	}

	public String getGeneName() {
		return geneName;
	}

	public int getNumSingleCellsInWhichIsExpressed(String cellTypeName) {
		if (numCellsByCellType.containsKey(cellTypeName)) {
			return numCellsByCellType.get(cellTypeName);
		}
		return 0;
	}

	public boolean permuteGeneExpressionInCells(List<SingleCell> singleCells) {
		try {
			if (singleCellsIDs == null) {
				// keep expressions of this gene in the single cells
				// so that we can shuffle them and go back to the original values calling to
				// resetToOriginalNonPermutatedExpressions
				singleCellsIDs = new ArrayList<Integer>();
				expressions = new TFloatArrayList();
				indexes = new ArrayList<Integer>();
				int index = 0;
				for (final SingleCell singleCell : singleCells) {
					final int singleCellID = singleCell.getId();
					singleCellsIDs.add(singleCellID);
					if (expressionsBySingleCellID.containsKey(singleCellID)) {
						final float expression = expressionsBySingleCellID.get(singleCellID);
						expressions.add(expression);
					} else {
						expressions.add(Float.NaN);
					}
					indexes.add(index++);
				}
			}

			Collections.shuffle(indexes);

			for (int i = 0; i < singleCellsIDs.size(); i++) {
				final int index = indexes.get(i);
				final Integer cellID = singleCellsIDs.get(i);
				final float expression = expressions.get(index);
				expressionsBySingleCellID.put(cellID, expression);
				final SingleCell singleCell = SingleCellsMetaInformationReader.getSingleCellByCellID(cellID);
				singleCell.addGeneExpressionValue(geneID, expression);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean permuteGeneExpressionInCells() {
		try {
			if (singleCellsIDs == null) {
				// keep expressions of this gene in the single cells
				// so that we can shuffle them and go back to the original values calling to
				// resetToOriginalNonPermutatedExpressions
				singleCellsIDs = new ArrayList<Integer>();
				expressions = new TFloatArrayList();
				indexes = new ArrayList<Integer>();
				int index = 0;
				for (final int singleCellID : expressionsBySingleCellID.keys()) {
					singleCellsIDs.add(singleCellID);
					final float expression = expressionsBySingleCellID.get(singleCellID);
					if (Float.isNaN(expression)) {
						continue;
					}
					expressions.add(expression);
					indexes.add(index++);
				}
			}

			Collections.shuffle(indexes);

			for (int i = 0; i < singleCellsIDs.size(); i++) {
				final int index = indexes.get(i);
				final Integer cellID = singleCellsIDs.get(i);
				final float expression = expressions.get(index);
				expressionsBySingleCellID.put(cellID, expression);
				final SingleCell singleCell = SingleCellsMetaInformationReader.getSingleCellByCellID(cellID);
				singleCell.addGeneExpressionValue(geneID, expression);
			}
			return true;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public void resetToOriginalNonPermutatedExpressions() {
		if (singleCellsIDs != null) {
			for (int i = 0; i < singleCellsIDs.size(); i++) {
				final Integer cellID = singleCellsIDs.get(i);
				final float expression = expressions.get(i);
				expressionsBySingleCellID.put(cellID, expression);
				final SingleCell singleCell = SingleCellsMetaInformationReader.getSingleCellByCellID(cellID);
				singleCell.addGeneExpressionValue(geneID, expression);
			}
		}
	}

	public int getGeneID() {
		return geneID;
	}

	/**
	 * @return the singleCellsIDs
	 */
	public List<Integer> getSingleCellsIDs() {
		return singleCellsIDs;
	}

	/**
	 * @return the expressions
	 */
	public TFloatList getExpressions() {
		return expressions;
	}

}
