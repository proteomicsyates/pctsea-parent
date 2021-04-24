package edu.scripps.yates.pctsea.model;

public class CellTypeBranched {
	private String originalCellType = null;
	private String type = null;
	private String subtype = null;
	private String characteristic = null;

	public CellTypeBranched(String originalCellType, String type, String subtype, String characteristic) {
		setOriginalCellType(originalCellType);
		setType(type);
		setSubtype(subtype);
		setCharacteristic(characteristic);
	}

	public String getOriginalCellType() {
		return originalCellType;
	}

	public void setOriginalCellType(String originalCellType) {
		this.originalCellType = originalCellType;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSubtype() {
		return subtype;
	}

	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}

	public String getCharacteristic() {
		return characteristic;
	}

	public void setCharacteristic(String characteristic) {
		this.characteristic = characteristic;
	}

	@Override
	public String toString() {
		return "[originalCellType=" + originalCellType + ", type=" + type + ", subtype=" + subtype + ", characteristic="
				+ characteristic + "]";
	}

	public String getCellTypeBranch(CellTypeBranch branch) {
		switch (branch) {
		case ORIGINAL:
			return this.originalCellType;
		case CHARACTERISTIC:
			return this.characteristic;
		case TYPE:
			return this.type;
		case TYPE_SUBTYPE:
			String ret = this.type;
			if (subtype != null) {
				ret += "-" + subtype;
			}
			return ret;
		case TYPE_SUBTYPE_CHARACTERISTIC:
			String ret2 = this.type;
			if (subtype != null) {
				ret2 += "-" + subtype;
			}
			if (characteristic != null) {
				ret2 += "-" + characteristic;
			}
			return ret2;

		default:
			throw new IllegalArgumentException(branch + " not supported?");
		}
	}

}
