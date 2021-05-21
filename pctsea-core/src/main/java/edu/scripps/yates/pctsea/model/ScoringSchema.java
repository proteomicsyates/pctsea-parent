package edu.scripps.yates.pctsea.model;

import edu.scripps.yates.pctsea.scoring.ScoreThreshold;

/**
 * This class encapsulates a list of pairs of {@link ScoringMethod} and the
 * {@link ScoreThreshold}
 * 
 * @author salvador
 *
 */
public class ScoringSchema {
	private final ScoringMethod scoringMethod;
	private final ScoreThreshold scoringThreshold;
	private final int minNumberExpressedGenesInCell;

	public ScoringSchema(ScoringMethod scoringMethod, ScoreThreshold threshold, int minNumberExpressedGenesInCell) {
		this.scoringMethod = scoringMethod;
		this.scoringThreshold = threshold;
		this.minNumberExpressedGenesInCell = minNumberExpressedGenesInCell;
	}

	public ScoringMethod getScoringMethod() {
		return scoringMethod;
	}

	public ScoreThreshold getScoringThreshold() {
		return scoringThreshold;
	}

	public int getMinNumberExpressedGenesInCell() {
		return minNumberExpressedGenesInCell;
	}

}
