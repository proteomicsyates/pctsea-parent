package edu.scripps.yates.pctsea.utils;

import edu.scripps.yates.pctsea.correlation.CorrelationThreshold;
import edu.scripps.yates.pctsea.model.CellTypeClassification;

public enum CellTypesOutputTableColumns {
	CELLTYPE("cell_type"), //
	NUM_CELLS_OF_TYPE("num_cells_of_type"), //
	NUM_TOTAL_CELLS("num_total_cells"), //
	NUM_CELLS_OF_TYPE_CORR("num_cells_of_type_corr"), //
	NUM_CELLS_CORR("num_cells_corr"), //
	HYPERG_PVALUE("hyperG_p-value"), //
	LOG2_RATIO("log2_ratio"), //
//+ "\teus"),
//+ "\teus p-value"),
	EWS("ews"), //
	NORM_EWS("norm-ews"), //
	SUPX("supX"), //
	NORM_SUPX("norm-supX"), //
	EMPIRICAL_PVALUE("empirical_p-value"), //
	FDR("FDR"), //
	SECOND_EWS("2nd_ews"), //
	SECOND_SUPX("2nd_supX"), //
	SIZE_A_TYPE("size_a_type"), //
	SIZE_B_OTHERS("size_b_others"), //
	DAB("Dab"), //
	KS_PVALUE("KS_p-value"), //
	KS_PVALUE_BH_CORRECTED("KS_p-value_BH_corrected"), //
	KS_SIGNIFICANT_LEVEL("KS_significance_level"), //
	UMAP_X("Umap_x"), //
	UMAP_Y("Umap_y"), //
	GENES("genes");

	private final String columnName;

	CellTypesOutputTableColumns(String columnName) {
		this.columnName = columnName;
	}

	public String getColumnName() {
		return columnName;
	}

	public static String getHeaderString(String separator) {
		final StringBuilder sb = new StringBuilder();
		for (final CellTypesOutputTableColumns column : values()) {
			if (!"".equals(sb.toString())) {
				sb.append(separator);
			}
			sb.append(column.getColumnName());
		}
		return sb.toString();
	}

	public String getValue(CellTypeClassification cellType, int numSingleCells,
			long numSingleCellsWithPositiveCorrelation, CorrelationThreshold correlationThreshold) {
		switch (this) {
		case CELLTYPE:
			return cellType.getName();
		case NUM_CELLS_OF_TYPE:
			return String.valueOf(cellType.getNumCellsOfType());
		case NUM_TOTAL_CELLS:
			return String.valueOf(numSingleCells);
		case NUM_CELLS_OF_TYPE_CORR:
			return String.valueOf(cellType.getNumCellsOfTypePassingCorrelationThreshold());
		case NUM_CELLS_CORR:
			return String.valueOf(numSingleCellsWithPositiveCorrelation);
		case HYPERG_PVALUE:
			return String.valueOf(cellType.getHypergeometricPValue());
		case LOG2_RATIO:
			return String.valueOf(cellType.getCasimirsEnrichmentScore());
		case EWS:
			return String.valueOf(cellType.getEnrichmentScore());
		case NORM_EWS:
			return String.valueOf(cellType.getNormalizedEnrichmentScore());
		case SUPX:
			return String.valueOf(cellType.getSupremumX());
		case NORM_SUPX:
			return String.valueOf(cellType.getNormalizedSupremumX());
		case EMPIRICAL_PVALUE:
			return String.valueOf(cellType.getEnrichmentSignificance());
		case FDR:
			return String.valueOf(cellType.getEnrichmentFDR());
		case SECOND_EWS:
			return parseNullableNumber(cellType.getSecondaryEnrichmentScore());
		case SECOND_SUPX:
			return parseNullableNumber(cellType.getSecondarySupremumX());
		case SIZE_A_TYPE:
			return String.valueOf(cellType.getSizeA());
		case SIZE_B_OTHERS:
			return String.valueOf(cellType.getSizeB());
		case DAB:
			return String.valueOf(cellType.getKSTestDStatistic());
		case KS_PVALUE:
			return String.valueOf(cellType.getKSTestPvalue());
		case KS_PVALUE_BH_CORRECTED:
			return String.valueOf(cellType.getKSTestCorrectedPvalue());
		case KS_SIGNIFICANT_LEVEL:
			return String.valueOf(cellType.getSignificancyString());
		case UMAP_X:
			return parseNullableNumber(cellType.getUmapClusteringX());
		case UMAP_Y:
			return parseNullableNumber(cellType.getUmapClusteringY());
		case GENES:
			return cellType.getStringOfRankingOfGenesThatContributedToTheCorrelation(correlationThreshold);
		default:
			throw new IllegalArgumentException("Value for column " + this + " is not supported yet!");
		}
	}

	private String parseNullableNumber(Number number) {
		if (number == null) {
			return "";
		}
		return number.toString();
	}

	public static CellTypeClassification getCellTypeFromLine(String line, String separator) {
		final String[] split = line.split(separator);
		if (split.length != CellTypesOutputTableColumns.values().length) {
			throw new IllegalArgumentException("Different number of columns that it should have!");
		}
		final CellTypeClassification ret = new CellTypeClassification("tempName", Double.NaN);
		for (int i = 0; i < split.length; i++) {
			final CellTypesOutputTableColumns column = values()[i];
			final String columnValue = split[i];
			switch (column) {
			case CELLTYPE:
				ret.setName(columnValue);
				break;
			case NUM_CELLS_OF_TYPE:
				ret.setNumCellsOfType(Long.valueOf(columnValue));
				break;
			case NUM_CELLS_OF_TYPE_CORR:
				ret.setNumCellsOfTypePassingCorrelationThreshold(Long.valueOf(columnValue));
				break;
			case HYPERG_PVALUE:
				ret.setHypergeometricPValue(Double.valueOf(columnValue));
				break;
			case LOG2_RATIO:
				ret.setCasimirsEnrichmentScore(Float.valueOf(columnValue));
				break;
			case EWS:
				ret.setEnrichmentScore(Float.valueOf(columnValue));
				break;
			case NORM_EWS:
				ret.setNormalizedEnrichmentScore(Float.valueOf(columnValue));
				break;
			case EMPIRICAL_PVALUE:
				ret.setEnrichmentSignificance(Double.valueOf(columnValue));
				break;
			case FDR:
				ret.setEnrichmentFDR(Double.valueOf(columnValue));
				break;
			case SECOND_EWS:
				if (!"".equals(columnValue)) {
					ret.setSecondaryEnrichment(Float.valueOf(columnValue));
				}
				break;
			case SECOND_SUPX:
				if (!"".equals(columnValue)) {
					ret.setSecondarySupremumX(Integer.valueOf(columnValue));
				}
				break;
			case SIZE_A_TYPE:
				ret.setSizeA(Integer.valueOf(columnValue));
				break;
			case SIZE_B_OTHERS:
				ret.setSizeB(Integer.valueOf(columnValue));
				break;
			case DAB:
				ret.setKSTestDStatistic(Float.valueOf(columnValue));
				break;
			case KS_PVALUE:
				ret.setKSTestCorrectedPvalue(Double.valueOf(columnValue));
				break;
			case KS_PVALUE_BH_CORRECTED:
				ret.setKSTestCorrectedPvalue(Double.valueOf(columnValue));
				break;
			case KS_SIGNIFICANT_LEVEL:
				ret.setKSTestSignificancyString(columnValue);
				break;
			case UMAP_X:
				if (!"".equals(columnValue)) {
					ret.setUmapClusteringX(Float.valueOf(columnValue));
				}
				break;
			case UMAP_Y:
				if (!"".equals(columnValue)) {
					ret.setUmapClusteringY(Float.valueOf(columnValue));
				}
				break;
			case GENES:
				ret.setRankingOfGenesThatContributedToTheCorrelation(columnValue);
				break;
			default:
				break;
			}
		}
		return ret;
	}
}