package edu.scripps.yates.pctsea.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.log4j.Logger;

import edu.scripps.yates.pctsea.InteractorsExpressionsRetriever;
import edu.scripps.yates.utilities.maths.Maths;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TShortList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class SingleCell {
	private final static Logger log = Logger.getLogger(SingleCell.class);
	private static Map<String, String> map = new THashMap<String, String>();
//	private final static THashMap<String, Set<String>> cellNamesByOriginalCellType = new THashMap<String, Set<String>>();

	private String name;
	private double score;// correlation with our experimental data
	private String cluster;
	private double x;
	private double y;
	private String originalCellType;
	private String cellType;
	private String age;
	private String developmentStage;
	private String gender;

	private String biomaterial;
	private String scoreDescription;
	private ArrayList<String> genesUsedForScore;
	private final int id;
	private String expressionsUsedForScore;
//	private CellTypeBranched branchedCellType;
	private static final PearsonsCorrelation correlationCalculator = new PearsonsCorrelation();

	private double geneExpressionVariance;
	private int cellTypeID = -1;
	private final int numGenesCapacity;
	// here we will add the expressions
	private TFloatArrayList expressionsByGene;
	// we will create an array of shorts in which each position corresponds with a
	// gene. The value in each position will be the index in the expressionByGene
	// array
	private short[] expressionIndexes;

	public SingleCell(int singleCellID, String name, double correlation) {
		this(singleCellID, name, correlation, 1);
	}

	public SingleCell(int singleCellID, String name, double correlation, int numGenesCapacity) {
		id = singleCellID;
		this.name = name;
		score = correlation;
		this.numGenesCapacity = numGenesCapacity;
		expressionIndexes = new short[numGenesCapacity];
		for (short i = 0; i < numGenesCapacity; i++) {
			expressionIndexes[i] = -1; // we put -1 so that we can detect when there is nothing in that position
		}
	}

	public void addGeneExpressionValue(short geneID, float expressionValue) {
		if (expressionsByGene == null) {
			expressionsByGene = new TFloatArrayList();
		}
		final short indexInWhichIsInsertedInExpressionsFloatArray = (short) expressionsByGene.size();
		// we add the expression to the array of floats
		expressionsByGene.add(expressionValue);
		// this should do something ONLY if in the constructor the capacity was not set.
		ensureCapacityOfIndexes(geneID);
		// we keep the index in the array of indexes
		expressionIndexes[geneID] = indexInWhichIsInsertedInExpressionsFloatArray;

	}

	private void ensureCapacityOfIndexes(short capacity) {
		if (capacity > expressionIndexes.length) {
			final int newCap = Math.max(expressionIndexes.length << 1, capacity);
			final short[] tmp = new short[newCap];
			System.arraycopy(expressionIndexes, 0, tmp, 0, expressionIndexes.length);
			expressionIndexes = tmp;
		}
	}

	public float getGeneExpressionValue(short geneID) {
		if (expressionsByGene == null) {
			return 0.0f;
		}
		// we look up the array of indexes
		final short expressionIndex = expressionIndexes[geneID];
		if (expressionIndex < 0) {
			return 0f; // because this single cell doesnt have expression for that gene
		}
		return expressionsByGene.get(expressionIndex);

	}

	public static String parseCellTypeTypos(String cellType) {
		final String originalCellType = cellType;
		if (map.containsKey(cellType)) {
			return map.get(cellType);
		}
		final Set<String> cellTypesMapped = new THashSet<String>();
		CellTypes.mapToOntologyCellType(cellType, cellTypesMapped);
		if (cellTypesMapped.size() > 1) {
//			PCTSEA.logStatus(cellType + " is mapped to multiple terms in the cell type ontology!");
		}
		if (!cellTypesMapped.isEmpty()) {
			final String next = cellTypesMapped.iterator().next();
			map.put(originalCellType, next);
			return next;
		}
//		PCTSEA.logStatus("'" + cellType + "' is not mapped to any cell type ontology");
		// parse heterogeneity of cell types
		if (cellType.contains("_")) {
			cellType = cellType.split("_")[0].trim();
		}
		// fix typos
		if (cellType.equals("activative t cell") || cellType.equals("actived t cell")) {
			cellType = "activated t cell";
		} else if (cellType.equals("unknown1") || cellType.equals("unknown2")) {
			cellType = "unknown";
		}
		cellType = cellType.replace("acinar", "acniar");
		cellType = cellType.replace("  ", " ");
		cellType = cellType.replace("soomth", "smooth");
		cellType = cellType.replace("muscel", "muscle");
		// b cell(centrocyte), b cell(plasmocyte), b cell(unknown), b cell
		if (cellType.startsWith("b cell") || cellType.contains(" b cell")) {
			cellType = "b cell";
		}
		if (cellType.equals("antigen presenting cell")) {
			cellType = "antigen-presenting cell";
		}
		if (cellType.equals("astrocyte(bergmann glia)")) {
			cellType = "astrocyte";
		}
		if (cellType.equals("epithelial")) {
			cellType = "epithelial cell";
		}
		if (cellType.equals("kerationcyte")) {
			cellType = "keratinocyte";
		}
		if (cellType.equals("mast")) {
			cellType = "mast cell";
		}
		if (cellType.equals("megakaryocyte/erythroid progenitor")) {
			cellType = "megakaryocyte/erythtoid progenitor cell";
		}
		if (cellType.equals("syncytiotrophoblast")) {
			cellType = "syncytiotrophoblast cell";
		}
		if (cellType.equals("neutriophil")) {
			cellType = "neutrophil";
		}
		if (cellType.equals("kuppfer cell")) {
			cellType = "kupffer cell";
		}
		if (cellType.equals("beta cell")) {
			cellType = "b cell";
		}

//			cellType = cellType.replace(" cell", "");
		cellType = cellType.trim();
		map.put(originalCellType, cellType);
		return cellType;
	}

//	/**
//	 * 
//	 * @param cellType
//	 * @param setAsOriginalCellType if true, it is considered an originalCellType
//	 *                              coming from the database which can be very
//	 *                              redundant and will be mapped to a
//	 *                              branchedCellType <br>
//	 *                              If false, the cell type will be considered
//	 *                              already a mapped branch
//	 */
//	public void setCellType(String cellType, boolean setAsOriginalCellType) {
//		if (cellType == null) {
//			return;
//		}
//		originalCellType = parseCellTypeTypos(cellType);
//		if (!cellNamesByOriginalCellType.containsKey(originalCellType)) {
//			cellNamesByOriginalCellType.put(originalCellType, new THashSet<String>());
//		}
//		cellNamesByOriginalCellType.get(originalCellType).add(getName());
//		if (setAsOriginalCellType) {
//			branchedCellType = CellTypes.getCellTypeByOriginalType(cellType);
//		} else {
//			branchedCellType = new CellTypeBranched(cellType, cellType, null, null);
//		}
//	}

	/**
	 * sets the cell type
	 * 
	 * @param cellType       if null, the cellTypeID will be -1
	 * @param parseTypos     if true, typos will be checked
	 * @param cellTypeBranch if not null, a table will be used to map to the
	 *                       corresponding branch level. If null, the cell type will
	 *                       be set as it is
	 */
	public void setCellType(String cellType, boolean parseTypos, CellTypeBranch cellTypeBranch) {
		if (cellType == null) {
			this.cellType = null;
			cellTypeID = -1;
			return;
		}
		// only parse typos if cellTypeBranch is not original
		if (parseTypos && cellTypeBranch != CellTypeBranch.ORIGINAL) {
			cellType = parseCellTypeTypos(cellType);
		}

		CellTypeBranched branchedCellType;
		if (cellTypeBranch != null) {
			branchedCellType = CellTypes.getCellTypeByOriginalType(cellType);
			if (branchedCellType == null) {
				this.cellType = cellType;
				return;
			}
			this.cellType = branchedCellType.getCellTypeBranch(cellTypeBranch);
		} else {
			this.cellType = cellType;
		}
		cellTypeID = CellTypes.getCellTypeID(cellType);
	}

	public int getCellTypeID() {
		return cellTypeID;
	}

	public String getCellType() {
		return cellType;
	}

	public String getAge() {
		return age;
	}

	public void setAge(String age) {
		this.age = age;
	}

	public String getDevelopmentStage() {
		return developmentStage;
	}

	public void setDevelopmentStage(String developmentStage) {
		this.developmentStage = developmentStage;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getBiomaterial() {
		return biomaterial;
	}

	public void setBiomaterial(String biomaterial) {
		this.biomaterial = biomaterial;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	public String getName() {
		return name;
	}

	public double getScoreForRanking() {
		return score;
	}

	/**
	 * for the papers in which they are clustered by types of cell lines
	 * 
	 * @param cluster
	 * @param x
	 * @param y
	 */
	public void setCluster(String cluster, double x, double y) {
		this.cluster = cluster;
		this.x = x;
		this.y = y;

	}

	public String getCluster() {
		return cluster;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public void setScore(double score) {
		this.score = score;

	}

	public String getScoreDescription() {
		return scoreDescription;
	}

	/**
	 * 
	 * @param interactorExpressions
	 * @param getExpressionsUsedForCorrelation
	 * @param minCorr                          minimum correlation
	 * @return
	 */
	public double calculateCorrelation(InteractorsExpressionsRetriever interactorExpressions,
			boolean getExpressionsUsedForCorrelation, Double minCorr) {
		return calculateCorrelation(interactorExpressions, getExpressionsUsedForCorrelation, false, minCorr);
	}

	/**
	 * Returns false when the cell doesn't have the minimum number of genes with a
	 * expression > 0
	 * 
	 * @param interactorExpressions
	 * @param minNumExpressedGenes
	 * @return
	 */
	public boolean hasMinNumberOfExpressedGenes(InteractorsExpressionsRetriever interactorExpressions,
			int minNumExpressedGenes) {

		final TShortList geneIDs = interactorExpressions.getInteractorsGeneIDs();
		if (minNumExpressedGenes > geneIDs.size()) {
			throw new IllegalArgumentException(
					"Minimum number of genes to be expressed in the cell lines (" + minNumExpressedGenes
							+ ") is higher than the actual number of genes in the experimental input data ("
							+ geneIDs.size() + ")");
		}
		int singleCellNonZero = 0;
		for (final short geneID : geneIDs.toArray()) {

			final float geneExpressionInSingleCell = getGeneExpressionValue(geneID);
//			final float geneExpressionInSingleCell = getGeneExpressionValue(geneID)interactorExpressions.getExpressionsOfGene(geneID)
//					.getSingleCellExpression(this.getId());
			final float interactorExpression = interactorExpressions.getInteractionExpressionInOurExperiment(geneID);
			// get only pairs of values when both values are > 0.0
			if (geneExpressionInSingleCell > 0.0f && interactorExpression > 0.0f) {
				singleCellNonZero++;
			}
		}
		if (singleCellNonZero < minNumExpressedGenes) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param interactorExpressions
	 * 
	 * @param minNumInteractorsForCorrelation
	 * @param getExpressionsUsedForCorrelation
	 * 
	 * @return
	 */
	private double calculateCorrelation(InteractorsExpressionsRetriever interactorExpressions,
			boolean getExpressionsUsedForCorrelation, boolean makeCorrelationAsSimpleScore, Double minCorr) {
//		// in case of having a percentage
//		if (minNumInteractorsForCorrelation <= 1.0) {
//			final int numInteractors = interactorExpressions.getInteractorsGeneIDs().size();
//			minNumInteractorsForCorrelation = numInteractors * minNumInteractorsForCorrelation;
//		}

		final TShortList geneIDs = interactorExpressions.getInteractorsGeneIDs();

		final TDoubleList interactorsExpressionsToCorrelate = new TDoubleArrayList();
		final TDoubleList genesExpressionsToCorrelate = new TDoubleArrayList();
		int singleCellNonZero = 0;
		final StringBuilder description = new StringBuilder();
		genesUsedForScore = new ArrayList<String>();

		for (final short geneID : geneIDs.toArray()) {

			final float geneExpressionInSingleCell = getGeneExpressionValue(geneID);
//			final float geneExpressionInSingleCell = getGeneExpressionValue(geneID)interactorExpressions.getExpressionsOfGene(geneID)
//					.getSingleCellExpression(this.getId());
			final float interactorExpression = interactorExpressions.getInteractionExpressionInOurExperiment(geneID);
			// get only pairs of values when both values are > 0.0
			if (geneExpressionInSingleCell > 0.0f && interactorExpression > 0.0f) {

				interactorsExpressionsToCorrelate.add(interactorExpression);
				genesExpressionsToCorrelate.add(geneExpressionInSingleCell);
				genesUsedForScore.add(InteractorsExpressionsRetriever.getInstance().getGeneName(geneID));
			}
			if (geneExpressionInSingleCell != 0.0f) {// this is also true when value is NaN
				singleCellNonZero++;
			}
		}
		for (final String gene : genesUsedForScore) {
			if (!"".equals(description.toString())) {
				description.append(",");
			}
			description.append(gene);
		}

		if (getExpressionsUsedForCorrelation) {
			expressionsUsedForScore = getExpressionsUsedForScore(genesExpressionsToCorrelate,
					interactorsExpressionsToCorrelate);
		} else {
			expressionsUsedForScore = "";
		}

		scoreDescription = description.toString();

		// check variance of gene expressions to correlate
		geneExpressionVariance = Maths.var(genesExpressionsToCorrelate.toArray());
		final double interactorsExpressionVariance = Maths.var(interactorsExpressionsToCorrelate.toArray());

		if (Double.compare(geneExpressionVariance, 0.0) == 0) {

			// we need to include some variance
			for (int i = 0; i < genesExpressionsToCorrelate.size(); i++) {
				final double delta = Math.random() / 1000000;
				genesExpressionsToCorrelate.set(i, genesExpressionsToCorrelate.get(i) + delta);
			}
		}
		if (Double.compare(interactorsExpressionVariance, 0.0) == 0) {

			// we need to include some variance
			for (int i = 0; i < interactorsExpressionsToCorrelate.size(); i++) {
				final double delta = Math.random() / 1000000;
				interactorsExpressionsToCorrelate.set(i, interactorsExpressionsToCorrelate.get(i) + delta);
			}
		}
		double score = correlationCalculator.correlation(interactorsExpressionsToCorrelate.toArray(),
				genesExpressionsToCorrelate.toArray());
		if (!Double.isNaN(score)) {
//				System.out.println("Correlations in cell " + getId() + " before was " + correlation + " and now is "
//						+ correlation2);
		}
		if (minCorr != null && score < minCorr) {
			score = Double.NaN;
		} else if (makeCorrelationAsSimpleScore) {
			score += singleCellNonZero;
		}
		setScore(score);

		return score;
	}

	/**
	 * 
	 * @param interactorExpressions
	 * @param getExpressionsUsedForCorrelation
	 * @param minCorr
	 * 
	 * @return
	 */
	public double calculateSimpleScore(InteractorsExpressionsRetriever interactorExpressions,
			boolean getExpressionsUsedForCorrelation, Double minCorr) {
		return calculateCorrelation(interactorExpressions, getExpressionsUsedForCorrelation, true, minCorr);
	}

	/**
	 * 
	 * @param interactorExpressions
	 * @param takeZerosInCorrelation
	 * @param getExpressionsUsedForCorrelation
	 * 
	 * @return
	 */
	public double calculateDotProductScore(InteractorsExpressionsRetriever interactorExpressions,
			boolean takeZerosInCorrelation, boolean getExpressionsUsedForCorrelation) {
//		// in case of having a percentage
//		if (minNumInteractorsForCorrelation <= 1.0) {
//			final int numInteractors = interactorExpressions.getInteractorsGeneIDs().size();
//			minNumInteractorsForCorrelation = numInteractors * minNumInteractorsForCorrelation;
//		}

		final TShortList geneIDs = interactorExpressions.getInteractorsGeneIDs();

		final TDoubleArrayList nonZeroInputGenesExpressionsToUse = new TDoubleArrayList();
		final TDoubleArrayList nonZeroSingleCellGenesExpressionsToUse = new TDoubleArrayList();
		TDoubleList interactorsExpressionsToUse = new TDoubleArrayList();
		TDoubleList genesExpressionsToUSe = new TDoubleArrayList();
		final StringBuilder description = new StringBuilder();
		genesUsedForScore = new ArrayList<String>();

		for (final short geneID : geneIDs.toArray()) {

			final float geneExpressionInSingleCell = getGeneExpressionValue(geneID);
//			final float geneExpressionInSingleCell = getGeneExpressionValue(geneID)interactorExpressions.getExpressionsOfGene(geneID)
//					.getSingleCellExpression(this.getId());
			final float interactorExpression = interactorExpressions.getInteractionExpressionInOurExperiment(geneID);
			// get only pairs of values when both values are > 0.0
			if (takeZerosInCorrelation || (geneExpressionInSingleCell > 0.0f && interactorExpression > 0.0f)) {

				interactorsExpressionsToUse.add(interactorExpression);
				genesExpressionsToUSe.add(geneExpressionInSingleCell);
			}
			if (geneExpressionInSingleCell > 0.0f) {
				genesUsedForScore.add(InteractorsExpressionsRetriever.getInstance().getGeneName(geneID));
				nonZeroSingleCellGenesExpressionsToUse.add(geneExpressionInSingleCell);
				nonZeroInputGenesExpressionsToUse.add(interactorExpression);
			}
			if (geneExpressionInSingleCell != 0.0f) {// this is also true when value is NaN

			}
		}

		for (final String gene : genesUsedForScore) {
			if (!"".equals(description.toString())) {
				description.append(",");
			}
			description.append(gene);
		}

		if (getExpressionsUsedForCorrelation) {
			expressionsUsedForScore = getExpressionsUsedForScore(nonZeroSingleCellGenesExpressionsToUse,
					nonZeroInputGenesExpressionsToUse);
		} else {
			expressionsUsedForScore = "";
		}

		scoreDescription = description.toString();

		// check variance of gene expressions to correlate
		geneExpressionVariance = Maths.var(nonZeroSingleCellGenesExpressionsToUse.toArray());

		double dotProduct = 0.0;
		try {
			// first we normalize each vector
			interactorsExpressionsToUse = Maths.normalize(interactorsExpressionsToUse);
			genesExpressionsToUSe = Maths.normalize(genesExpressionsToUSe);
			dotProduct = Maths.dotProduct(interactorsExpressionsToUse, genesExpressionsToUSe);
		} catch (final Exception e) {
			// we dont care, in that case correlation is zero
		}
		if (!Double.isNaN(score)) {
//				System.out.println("Correlations in cell " + getId() + " before was " + correlation + " and now is "
//						+ correlation2);
		}
		final double score = dotProduct;
		setScore(score);

		return score;
	}

	/**
	 * Gets the gene expression variance calculated when calling
	 * calculateCorrelation on a set of interactors
	 * 
	 * @return
	 */
	public double getGeneExpressionVariance() {
		return geneExpressionVariance;
	}

	public List<String> getGenesUsedForScore() {
		return genesUsedForScore;
	}

	private String getExpressionsUsedForScore(TDoubleList nonZeroInteractorsExpressionsToUse,
			TDoubleList nonZeroGenesExpressionsToUse) {
		final StringBuilder sb = new StringBuilder("[");

		for (final double expression : nonZeroGenesExpressionsToUse.toArray()) {
			if (!"[".equals(sb.toString())) {
				sb.append(",");
			}
			sb.append(expression);
		}
		final StringBuilder sb2 = new StringBuilder();
		for (final double expression : nonZeroInteractorsExpressionsToUse.toArray()) {
			if (!"".equals(sb2.toString())) {
				sb2.append(",");
			}
			sb2.append(expression);
		}
		return sb.append("] [").append(sb2.toString()).append("]").toString();
	}

	public String getExpressionsUsedForScore() {
		return expressionsUsedForScore;
	}

	public String getGenesUsedForScoreString() {
		final StringBuilder sb = new StringBuilder();
		for (final String gene : genesUsedForScore) {
			if (!"".equals(sb.toString())) {
				sb.append(",");
			}
			sb.append(gene);
		}
		return sb.toString();
	}

	public int getId() {
		return id;
	}

	public String getOriginalCellType() {
		return originalCellType;
	}

	public void setName(String name) {
		this.name = name;
	}

}
