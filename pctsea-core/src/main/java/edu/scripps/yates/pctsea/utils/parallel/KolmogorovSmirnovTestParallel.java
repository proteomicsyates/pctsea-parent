package edu.scripps.yates.pctsea.utils.parallel;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;

import edu.scripps.yates.pctsea.model.CellTypeClassification;
import edu.scripps.yates.pctsea.utils.PCTSEAUtils;
import edu.scripps.yates.utilities.pi.ParIterator;

public class KolmogorovSmirnovTestParallel extends Thread {
	private final ParIterator<CellTypeClassification> iterator;
	private final int numThread;
	private final KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();

	public KolmogorovSmirnovTestParallel(ParIterator<CellTypeClassification> iterator, int numCore) {
		this.iterator = iterator;
		numThread = numCore;
	}

	@Override
	public void run() {
		while (iterator.hasNext()) {
			final CellTypeClassification cellType = iterator.next();
			final float[] x = cellType.getCellTypeCorrelationDistribution().toArray();
			final float[] y = cellType.getOtherCellTypesCorrelationDistribution().toArray();
			double pvalue = 1.0;
			if (x.length > 1 && y.length > 1) {
				pvalue = ksTest.kolmogorovSmirnovTest(PCTSEAUtils.toDoubleArray(x), PCTSEAUtils.toDoubleArray(y),
						false);
			}
//			cellType.setKolmogorovSmirnovTestPValue(pvalue);
		}
	}
}
