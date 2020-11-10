package edu.scripps.yates.pctsea.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.log4j.Logger;

import edu.scripps.yates.pctsea.InteractorsExpressionsRetriever;
import edu.scripps.yates.utilities.maths.Maths;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.hash.THashSet;

public class SingleCell {
	private final static Logger log = Logger.getLogger(SingleCell.class);
	private static Map<String, String> map = new THashMap<String, String>();
	private final static THashMap<String, Set<String>> cellNamesByOriginalCellType = new THashMap<String, Set<String>>();
	private final static THashMap<String, Set<String>> cellNamesByMappedCellType = new THashMap<String, Set<String>>();
	private final String name;
	private double correlation;// correlation with our experimental data
	private String cluster;
	private double x;
	private double y;
	private String originalCellType;
	private String age;
	private String developmentStage;
	private String gender;

	private String biomaterial;
	private String correlationDescription;
	private ArrayList<String> genesForCorrelation;
	private final int id;
	private String expressionsUsedForCorrelation;
	private CellTypeBranched branchedCellType;
	private static final PearsonsCorrelation correlationCalculator = new PearsonsCorrelation();
	private TIntFloatMap expressionsByGene;
	private double geneExpressionVariance;

	public void addGeneExpressionValue(int geneID, float expressionValue) {
		if (expressionsByGene == null) {
			expressionsByGene = new TIntFloatHashMap();
		}
		expressionsByGene.put(geneID, expressionValue);
	}

	public float getGeneExpressionValue(int geneID) {
		if (expressionsByGene == null) {
			return 0.0f;
		}
		return expressionsByGene.get(geneID);
	}

	public String getCellType(CellTypeBranch branch) {
		if (branch == null) {
			return originalCellType;
		}
		if (branchedCellType == null) {
			branchedCellType = CellTypes.getCellTypeByOriginalType(originalCellType);
		}
		if (branchedCellType != null) {
			return branchedCellType.getCellTypeBranch(branch);
		}
		return null;
	}

	public static String parseCellTypeTypos(String cellType) {
		final String originalCellType = cellType;
		if (map.containsKey(cellType)) {
			return map.get(cellType);
		}
		final Set<String> cellTypesMapped = new THashSet<String>();
		CellTypes.mapToOntologyCellType(cellType, cellTypesMapped);
		if (cellTypesMapped.size() > 1) {
//			log.info(cellType + " is mapped to multiple terms in the cell type ontology!");
		}
		if (!cellTypesMapped.isEmpty()) {
			final String next = cellTypesMapped.iterator().next();
			map.put(originalCellType, next);
			return next;
		}
//		log.info("'" + cellType + "' is not mapped to any cell type ontology");
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

	public void setCellType(String cellType) {
		if (cellType == null) {
			return;
		}
		if (!cellNamesByOriginalCellType.containsKey(cellType)) {
			cellNamesByOriginalCellType.put(cellType, new THashSet<String>());
		}
		cellNamesByOriginalCellType.get(cellType).add(getName());
		this.originalCellType = parseCellTypeTypos(cellType);
		this.originalCellType = cellType;
		if (!cellNamesByMappedCellType.containsKey(this.originalCellType)) {
			cellNamesByMappedCellType.put(this.originalCellType, new THashSet<String>());
		}
		cellNamesByMappedCellType.get(this.originalCellType).add(getName());
		branchedCellType = CellTypes.getCellTypeByOriginalType(cellType);

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

	public SingleCell(int singleCellID, String name, double correlation) {
		this.id = singleCellID;
		this.name = name;
		this.correlation = correlation;
	}

	public String getName() {
		return name;
	}

	public double getCorrelation() {
		return correlation;
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

	public void setCorrelation(double correlation2) {
		this.correlation = correlation2;

	}

	public String getCorrelationDescription() {
		return correlationDescription;
	}

	/**
	 * 
	 * @param interactorExpressions
	 * @param takeZerosInCorrelation
	 * @param minNumInteractorsForCorrelation
	 * @param getExpressionsUsedForCorrelation
	 * 
	 * @return
	 */
	public double calculateCorrelation(InteractorsExpressionsRetriever interactorExpressions,
			boolean takeZerosInCorrelation, double minNumInteractorsForCorrelation,
			boolean getExpressionsUsedForCorrelation) {
		// in case of having a percentage
		if (minNumInteractorsForCorrelation <= 1.0) {
			final int numInteractors = interactorExpressions.getInteractorsGeneIDs().size();
			minNumInteractorsForCorrelation = numInteractors * minNumInteractorsForCorrelation;
		}

		final TIntList geneIDs = interactorExpressions.getInteractorsGeneIDs();
		if (minNumInteractorsForCorrelation > geneIDs.size()) {
			throw new IllegalArgumentException(
					"Minimum number of genes to be expressed in the cell lines (" + minNumInteractorsForCorrelation
							+ ") is higher than the actual number of genes in the experimental input data ("
							+ geneIDs.size() + ")");
		}
		final TDoubleArrayList nonZeroInteractorsExpressionsToCorrelate = new TDoubleArrayList();
		final TDoubleArrayList nonZeroGenesExpressionsToCorrelate = new TDoubleArrayList();
		final TDoubleList interactorsExpressionsToCorrelate = new TDoubleArrayList();
		final TDoubleList genesExpressionsToCorrelate = new TDoubleArrayList();
		int singleCellNonZero = 0;
		final StringBuilder description = new StringBuilder();
		this.genesForCorrelation = new ArrayList<String>();
		final TIntIterator geneIDIterator = geneIDs.iterator();
		while (geneIDIterator.hasNext()) {
			final int geneID = geneIDIterator.next();
			final float geneExpressionInSingleCell = getGeneExpressionValue(geneID);
//			final float geneExpressionInSingleCell = getGeneExpressionValue(geneID)interactorExpressions.getExpressionsOfGene(geneID)
//					.getSingleCellExpression(this.getId());
			final float interactorExpression = interactorExpressions.getInteractionExpressionInOurExperiment(geneID);
			// get only pairs of values when both values are > 0.0
			if (takeZerosInCorrelation || (geneExpressionInSingleCell > 0.0f && interactorExpression > 0.0f)) {

				interactorsExpressionsToCorrelate.add(interactorExpression);
				genesExpressionsToCorrelate.add(geneExpressionInSingleCell);
			}
			if (geneExpressionInSingleCell > 0.0f) {
				genesForCorrelation.add(InteractorsExpressionsRetriever.getInstance().getGeneName(geneID));
				nonZeroGenesExpressionsToCorrelate.add(geneExpressionInSingleCell);
				nonZeroInteractorsExpressionsToCorrelate.add(interactorExpression);
			}
			if (geneExpressionInSingleCell != 0.0f) {// this is also true when value is NaN
				singleCellNonZero++;
			}
		}
		for (final String gene : genesForCorrelation) {
			if (!"".equals(description.toString())) {
				description.append(",");
			}
			description.append(gene);
		}

		if (getExpressionsUsedForCorrelation) {
			this.expressionsUsedForCorrelation = getExpressionsUsedForCorrelation(nonZeroGenesExpressionsToCorrelate,
					nonZeroInteractorsExpressionsToCorrelate);
		} else {
			this.expressionsUsedForCorrelation = "";
		}

		correlationDescription = description.toString();

		// check variance of gene expressions to correlate
		geneExpressionVariance = Maths.var(nonZeroGenesExpressionsToCorrelate.toArray());
		if (!takeZerosInCorrelation && singleCellNonZero < Math.max(2, minNumInteractorsForCorrelation)) {
			setCorrelation(Double.NaN);
		} else {
			final double correlation2 = correlationCalculator.correlation(interactorsExpressionsToCorrelate.toArray(),
					genesExpressionsToCorrelate.toArray());
			if (!Double.isNaN(correlation)) {
//				System.out.println("Correlations in cell " + getId() + " before was " + correlation + " and now is "
//						+ correlation2);
			}
			setCorrelation(correlation2);
		}
		return this.correlation;
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

	public List<String> getGenesForCorrelation() {
		return genesForCorrelation;
	}

	private String getExpressionsUsedForCorrelation(TDoubleArrayList nonZeroInteractorsExpressionsToCorrelate,
			TDoubleArrayList nonZeroGenesExpressionsToCorrelate) {
		final StringBuilder sb = new StringBuilder("[");

		for (final double expression : nonZeroGenesExpressionsToCorrelate.toArray()) {
			if (!"[".equals(sb.toString())) {
				sb.append(",");
			}
			sb.append(expression);
		}
		final StringBuilder sb2 = new StringBuilder();
		for (final double expression : nonZeroInteractorsExpressionsToCorrelate.toArray()) {
			if (!"".equals(sb2.toString())) {
				sb2.append(",");
			}
			sb2.append(expression);
		}
		return sb.append("] [").append(sb2.toString()).append("]").toString();
	}

	public String getExpressionsUsedForCorrelation() {
		return this.expressionsUsedForCorrelation;
	}

	public String getGenesForCorrelationString() {
		final StringBuilder sb = new StringBuilder();
		for (final String gene : genesForCorrelation) {
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

//	public static void printCellTypeMapping(File outputFile) throws IOException {
//		FileWriter fw = null;
//		try {
//			fw = new FileWriter(outputFile);
//			fw.write("# original\tOriginal\t# mapped\tMapped\n");
//			for (final String originalCellType : map.keySet()) {
//				final int numOriginal = cellIDsByOriginalCellType.get(originalCellType).size();
//				final String mappedCellType = map.get(originalCellType);
//				int numMapped = 0;
//				if (cellIDsByMappedCellType.containsKey(mappedCellType)) {
//					numMapped = cellIDsByMappedCellType.get(mappedCellType).size();
//				}
//				fw.write(numOriginal + "\t" + originalCellType + "\t" + numMapped + "\t" + mappedCellType + "\n");
//			}
//		} finally {
//			fw.close();
//			System.out.println("Cell types mapping stored at: " + outputFile.getAbsolutePath());
//		}
//	}

	public String getOriginalCellType() {
		return originalCellType;
	}

}
