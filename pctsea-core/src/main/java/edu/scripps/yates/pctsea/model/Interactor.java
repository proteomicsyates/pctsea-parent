package edu.scripps.yates.pctsea.model;

import java.util.Collections;
import java.util.Set;

import edu.scripps.yates.pctsea.utils.PCTSEAUtils;
import edu.scripps.yates.utilities.annotations.uniprot.UniprotEntryUtil;
import edu.scripps.yates.utilities.annotations.uniprot.xml.Entry;
import edu.scripps.yates.utilities.fasta.FastaParser;

/**
 * Gene that represent an interactor in an IP experiment. It can contain
 * multiple gene names because of protein grouping
 * 
 * @author salvador
 *
 */
public class Interactor {
	private final String gene;
	private final float normSPC;
	private final String acc;
	private Set<String> ensgIDs = null;

	public Interactor(String acc, String gene, float normSPC, Entry entry) {
		this.acc = acc;
		this.gene = PCTSEAUtils.parseGeneName(gene);
		this.normSPC = normSPC;
		if (entry != null) {
			ensgIDs = UniprotEntryUtil.getENSGIDs(entry).get(acc);
			if (ensgIDs == null || ensgIDs.isEmpty()) {
				// try to get the HPA reference
				final Set<String> ids = UniprotEntryUtil.getDBReferenceIDsByType(entry, "HPA");
				if (ids != null && !ids.isEmpty()) {
					ensgIDs = ids;
				}
			}
		}
		if (ensgIDs == null) {

			ensgIDs = Collections.emptySet();
		}
	}

	public String getGene() {
		return gene;
	}

	public float getNormSPC() {
		return normSPC;
	}

	public String getAcc() {
		return acc;
	}

	public Set<String> getENSGID() {
		return this.ensgIDs;
	}

	public String getNonIsoformAcc() {
		return FastaParser.getNoIsoformAccession(getAcc());
	}

}
