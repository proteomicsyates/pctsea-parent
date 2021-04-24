package edu.scripps.yates.pctsea.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.log4j.Logger;

import edu.scripps.yates.pctsea.PCTSEA;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class CellTypes {
	private final static String hierarchicalCellTypesFileName = "spike_cell_types_mapping_CB.txt";

	private final static Logger log = Logger.getLogger(CellTypes.class);
	private static Set<String> cellTypes;
	static {
		cellTypes = org.proteored.miapeapi.cv.CellTypes.getInstance(null).getPossibleValues().stream()
				.map(cvTerm -> cvTerm.getPreferredName().toLowerCase()).collect(Collectors.toSet());
//		PCTSEA.logStatus(cellTypes.size() + " cell types in the ontology");

	}

	public static void mapToOntologyCellType(String cellTypeString, Set<String> cellTypesMapped) {
		if (cellTypesMapped == null) {
			throw new IllegalArgumentException("Set is null");
		}
		cellTypeString = cellTypeString.toLowerCase();
		if (cellTypes.contains(cellTypeString)) {
			cellTypesMapped.add(cellTypeString);
		} else {
			final StringTokenizer tokenizer = new StringTokenizer(cellTypeString);
			final List<String> list = new ArrayList<String>();
			while (tokenizer.hasMoreTokens()) {
				list.add(tokenizer.nextToken());
			}
			if (list.size() == 1) {
				return;
			} else {
				for (int numElements = list.size() - 1; numElements >= 1; numElements--) {
					// we get all the combinations of elements with numelements elements
					final Iterator<int[]> combinationsIterator = CombinatoricsUtils.combinationsIterator(list.size(),
							numElements);
					while (combinationsIterator.hasNext()) {
						final int[] indexes = combinationsIterator.next();
						final StringBuilder sb = new StringBuilder();
						for (final int index : indexes) {
							if (!"".equals(sb.toString())) {
								sb.append(" ");
							}
							sb.append(list.get(index));
						}
						final String newCellTypeString = sb.toString();
						mapToOntologyCellType(newCellTypeString, cellTypesMapped);

					}
				}
				return;
			}
		}
	}

	public static void main(String[] args) {

		Set<String> set = new THashSet<String>();
		mapToOntologyCellType("Mucosal aquamous  Epithelial cell", set);
		for (final String string : set) {
			System.out.println(string);
		}
		set = new THashSet<String>();
		mapToOntologyCellType("germ line stem cell", set);
		for (final String string : set) {
			System.out.println(string);
		}
		set = new THashSet<String>();
		mapToOntologyCellType("asdfasdfasdfa", set);
		for (final String string : set) {
			System.out.println(string);
		}
	}

	private static final Map<String, CellTypeBranched> cellTypeByOriginalCellType = new THashMap<String, CellTypeBranched>();
	private static final Map<String, Set<CellTypeBranched>> cellTypesByType = new THashMap<String, Set<CellTypeBranched>>();
	private static final Map<String, Set<CellTypeBranched>> cellTypesBySubType = new THashMap<String, Set<CellTypeBranched>>();
	private static final Map<String, Set<CellTypeBranched>> cellTypesByCharacteristics = new THashMap<String, Set<CellTypeBranched>>();
	private static final Set<String> subTypes = new THashSet<String>();
	private static final Set<String> types = new THashSet<String>();
	private static final Set<String> characteristics = new THashSet<String>();
	private static final Set<String> originalCellTypes = new THashSet<String>();

	private static void loadHierarchicalCellType() {
		BufferedReader reader = null;
		try {
			final InputStream is = CellTypes.class.getClassLoader()
					.getResourceAsStream(CellTypes.hierarchicalCellTypesFileName);
			reader = new BufferedReader(new InputStreamReader(is));
			String line = null;
			int numLine = 0;
			while ((line = reader.readLine()) != null) {
				numLine++;
				if (numLine == 1) {
					continue;
				}
				if ("".equals(line)) {
					continue;
				}
				final String[] split = line.split("\t");
				final String originalCellType = split[0].trim();
				originalCellTypes.add(originalCellType);

				String type = null;
				String subtype = null;
				String characteristic = null;

				if (split.length > 3) {
					type = split[3].trim();
					// I replace spaces by _
					if (type.contains(" ")) {
						type = type.replace(" ", "_");
					}
					if (!"".equals(type)) {
						types.add(type);
					}
				}
				if (split.length > 4) {
					subtype = split[4].trim();
					if (!"".equals(subtype)) {
						subTypes.add(subtype);
					}
				}
				if (split.length > 5) {
					characteristic = split[5].trim();
					if (!"".equals(characteristic)) {
						characteristics.add(characteristic);
					}
				}

				final CellTypeBranched cellTypeBranched = new CellTypeBranched(originalCellType, type, subtype,
						characteristic);
				if (!cellTypeByOriginalCellType.containsKey(originalCellType)) {
					cellTypeByOriginalCellType.put(originalCellType, cellTypeBranched);
				}
				if (characteristic != null && !"".equals(characteristic)) {
					if (!cellTypesByCharacteristics.containsKey(characteristic)) {
						cellTypesByCharacteristics.put(characteristic, new THashSet<CellTypeBranched>());
					}
					cellTypesByCharacteristics.get(characteristic).add(cellTypeBranched);
				}
				if (subtype != null && !"".equals(subtype)) {
					if (!cellTypesBySubType.containsKey(subtype)) {
						cellTypesBySubType.put(subtype, new THashSet<CellTypeBranched>());
					}
					cellTypesBySubType.get(subtype).add(cellTypeBranched);
				}
				if (type != null && !"".equals(type)) {
					if (!cellTypesByType.containsKey(type)) {
						cellTypesByType.put(type, new THashSet<CellTypeBranched>());
					}
					cellTypesByType.get(type).add(cellTypeBranched);
				}

			}
			reader.close();
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	static Set<String> notFound = new THashSet<String>();

	public static CellTypeBranched getCellTypeByOriginalType(String originalType) {
		if (originalCellTypes.isEmpty()) {
			loadHierarchicalCellType();
		}
		if (originalType == null) {
			log.info(originalType);
		} else {
			originalType = originalType.trim();
		}
		final CellTypeBranched cellTypeBranched = cellTypeByOriginalCellType.get(originalType);
		if (cellTypeBranched == null) {
			try {
				if (!notFound.contains(originalType)) {
					final FileWriter fw = new FileWriter(new File(
							"C:\\Users\\salvador\\eclipse-workspace\\pctsea-parent\\pctsea-core\\src\\main\\resources\\cell_types_not_found_in_table.txt"),
							true);
					fw.write(originalType + "\n");
					fw.close();
					notFound.add(originalType);
					log.debug(originalType);
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}

		}
		return cellTypeBranched;
	}

	public static Set<CellTypeBranched> getCellTypesByCharacteristics(String characteristics) {
		if (originalCellTypes.isEmpty()) {
			loadHierarchicalCellType();
		}
		return cellTypesByCharacteristics.get(characteristics);
	}

	public static Set<CellTypeBranched> getCellTypesBySubType(String subType) {
		if (originalCellTypes.isEmpty()) {
			loadHierarchicalCellType();
		}
		return cellTypesBySubType.get(subType);
	}

	public static Set<CellTypeBranched> getCellTypesByType(String type) {
		if (originalCellTypes.isEmpty()) {
			loadHierarchicalCellType();
		}
		return cellTypesByType.get(type);
	}

	public static Set<String> getSubTypes() {
		if (originalCellTypes.isEmpty()) {
			loadHierarchicalCellType();
		}
		return subTypes;
	}

	public static Set<String> getTypes() {
		if (originalCellTypes.isEmpty()) {
			loadHierarchicalCellType();
		}
		return types;
	}

	public static Set<String> getCharacteristics() {
		if (originalCellTypes.isEmpty()) {
			loadHierarchicalCellType();
		}
		return characteristics;
	}

	public static Set<String> getOriginalCellTypes() {
		if (originalCellTypes.isEmpty()) {
			loadHierarchicalCellType();
		}
		return originalCellTypes;
	}
}
