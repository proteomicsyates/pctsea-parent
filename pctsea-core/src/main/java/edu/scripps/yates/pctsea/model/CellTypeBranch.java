package edu.scripps.yates.pctsea.model;

public enum CellTypeBranch {
	TYPE, TYPE_SUBTYPE, TYPE_SUBTYPE_CHARACTERISTIC, CHARACTERISTIC;

	public static String getStringSeparated(String separator) {
		return "'" + TYPE + "'" + separator + "'" + TYPE_SUBTYPE + "'" + separator + "'" + TYPE_SUBTYPE_CHARACTERISTIC
				+ "'" + separator + "'" + CHARACTERISTIC + "'";
	}

//	public static List<CellTypeBranch> parseCellTypeBranchesString(String optionValue) {
//		final List<CellTypeBranch> ret = new ArrayList<CellTypeBranch>();
//		final Set<String> elements = new THashSet<String>();
//		if (optionValue.contains(",")) {
//			final String[] split = optionValue.split(",");
//			for (final String string : split) {
//				elements.add(string.trim());
//			}
//		} else {
//			elements.add(optionValue.trim());
//		}
//		for (final String element : elements) {
//			final CellTypeBranch valueOf = CellTypeBranch.valueOf(element);
//			if (!ret.contains(valueOf)) {
//				ret.add(valueOf);
//			}
//		}
//		return ret;
//	}
//
//	public static String getStringSeparated(List<CellTypeBranch> cellTypeBranches, String separator) {
//		final StringBuilder sb = new StringBuilder();
//		for (final CellTypeBranch cellTypeBranch : cellTypeBranches) {
//			if (!"".equals(sb.toString())) {
//				sb.append(separator);
//			}
//			sb.append(cellTypeBranch.name());
//		}
//		return sb.toString();
//	}
}
