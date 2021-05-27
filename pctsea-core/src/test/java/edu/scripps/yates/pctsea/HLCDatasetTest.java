package edu.scripps.yates.pctsea;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.Test;

import edu.scripps.yates.pctsea.db.Expression;
import edu.scripps.yates.pctsea.db.SingleCell;
import gnu.trove.map.hash.THashMap;

public class HLCDatasetTest {
	private final String singleCellName2 = "AdultMuscle_1.GAACGCACACCCTTCCGC";
	private final String geneName2 = "ACTA2";

	@Test
	public void test() {
		final File file = new File(
				"D:\\Salva\\git_projects\\pctsea-parent\\pctsea-core\\src\\test\\resources\\Adult-Muscle1_rmbatchdge.txt.gz");

		try {
			readSingleCellGZipFileAndSaveSingleCells(file);
		} catch (final IOException e) {

			e.printStackTrace();
		}
	}

	private void readSingleCellGZipFileAndSaveSingleCells(File file) throws IOException {

		// final THashMap<String, TObjectIntMap<String>> expressionsByCell = new
		// THashMap<String, TObjectIntMap<String>>();
		BufferedReader br = null;
		final List<SingleCell> singleCellList = new ArrayList<SingleCell>();
		final Map<String, SingleCell> singleCellByNames = new THashMap<String, SingleCell>();
		final List<Expression> sces = new ArrayList<Expression>();
		try {

			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
			String line = br.readLine();
//			final TObjectIntMap<String> indexByHeader = new TObjectIntHashMap<String>();
			String[] headers = null;
			int[] indexes = null;
			int numLine = 1;

			while (line != null) {
				try {
					if (line.startsWith("#")) {
						continue;
					}

//					final List<String> split = Splitter.on(',').splitToList(line); // this is slower
					final String[] split = line.split(",");
					if (headers == null) {
						headers = new String[split.length];
						indexes = new int[split.length];
						for (int index = 0; index < split.length; index++) {
							final String string = split[index].replace("\"", "");
							if (string.equals(singleCellName2)) {
								System.out.println(singleCellName2 + " in column " + index);
							}
							headers[index] = string;
							indexes[index] = index + 1;
//							indexByHeader.put(string, index + 1); // header is shifted one position to the left
						}

						continue;
					}
					final String gene = split[0].replace("\"", "").toUpperCase();
					if (!gene.equals(geneName2)) {
						continue;
					}
					System.out.println(gene + " in line " + numLine);
//					for (final String header : indexByHeader.keySet()) {

					for (int i = 0; i < headers.length; i++) {
						final String header = headers[i];

						final int index = indexes[i];
						if (!header.equals("")) {
							final String expressionValueString = split[index];
							final short expressionValue = Short.valueOf(expressionValueString);
							if (gene.equals(geneName2) && singleCellName2.equals(header)) {
								System.out.println("expression = " + expressionValue);
							}

						}
					}

				} finally {
					line = br.readLine();
					numLine++;
//					if (sces.size() >= 100) {
//						break;
//					}
				}
			}

		} finally {
			br.close();
		}

	}
}
