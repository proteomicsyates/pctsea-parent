package edu.scripps.yates.pctsea.model;

import java.io.Serializable;

import edu.scripps.yates.pctsea.scoring.ScoreThreshold;

/**
 * This class encapsulates a list of pairs of {@link ScoringMethod} and the
 * {@link ScoreThreshold}
 * 
 * @author salvador
 *
 */
public class ScoringSchema implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2747351373269915643L;
	private ScoringMethod scoringMethod;
	private ScoreThreshold scoringThreshold;

	public ScoringSchema() {

	}

	public ScoringSchema(ScoringMethod scoringMethod, ScoreThreshold threshold) {
		this.scoringMethod = scoringMethod;
		scoringThreshold = threshold;

	}

	public ScoringMethod getScoringMethod() {
		return scoringMethod;
	}

	public ScoreThreshold getScoringThreshold() {
		return scoringThreshold;
	}

	public void setScoringMethod(ScoringMethod scoringMethod) {
		this.scoringMethod = scoringMethod;
	}

	public void setScoringThreshold(ScoreThreshold scoringThreshold) {
		this.scoringThreshold = scoringThreshold;
	}

}
