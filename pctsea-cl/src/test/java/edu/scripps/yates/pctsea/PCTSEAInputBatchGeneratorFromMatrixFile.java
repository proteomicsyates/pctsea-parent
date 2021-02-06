package edu.scripps.yates.pctsea;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import edu.scripps.yates.annotations.uniprot.UniprotProteinLocalRetriever;
import edu.scripps.yates.utilities.matrix.DefaultDoubleMatrix;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * Generates a set of input files for PCTSEA from a matrix file that has a
 * column per pctsea run and each row has the expression values of a protein
 * among all these experiments.
 * 
 * @author salvador
 *
 */
public class PCTSEAInputBatchGeneratorFromMatrixFile {
	private static final UniprotProteinLocalRetriever uplr = new UniprotProteinLocalRetriever(
			new File("Z:\\share\\Salva\\UniprotKB"), true);

	public static void main(String[] args) {
		try {
			final File matrixFile = new File(
					"C:\\Users\\salvador\\Desktop\\casimir\\E-PROT-2 NCI60\\nci60_protV15.txt");

			final PCTSEAInputBatchGeneratorFromMatrixFile r = new PCTSEAInputBatchGeneratorFromMatrixFile(matrixFile);
			r.run();
			System.out.println("Everything ok!");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private final File matrixFile;

	public PCTSEAInputBatchGeneratorFromMatrixFile(File matrixFile) {
		this.matrixFile = matrixFile;

	}

	public void run() throws IOException {
		final List<String> lines = Files.readAllLines(this.matrixFile.toPath());
		final String header = lines.get(0);
		final String[] split = header.split("\t");
		final List<String> cellLines = new ArrayList<String>();
		final TObjectIntMap<String> indexByCellLines = new TObjectIntHashMap<String>();
		for (int i = 1; i < split.length; i++) {
			final String cellLine = split[i];
			cellLines.add(cellLine);
			indexByCellLines.put(cellLine, i);
		}
		System.out.println(cellLines.size() + " cell lines");
		final DefaultDoubleMatrix matrix = new DefaultDoubleMatrix(Double.NaN);

		for (int numLine = 1; numLine < lines.size(); numLine++) {
			final String line = lines.get(numLine);
			final String[] split2 = line.split("\t");
			final String protein = split2[0];
			for (int j = 1; j < split2.length; j++) {
				try {
					final String cellLine = cellLines.get(j - 1);
					final double expression = Double.valueOf(split2[j]);
					matrix.add(protein, cellLine, expression);
				} catch (final NumberFormatException e) {
					e.printStackTrace();
					throw new IllegalArgumentException("There is something that is not a number at row " + numLine
							+ " column " + j + ": " + split2[j]);
				}
			}
		}
		System.out.println("Matrix read with " + matrix.getNCols() + " cols and " + matrix.getNRows() + " rows");
		final FileWriter fw2 = new FileWriter(matrixFile.getParent() + File.separator + "cell_lines_id_map.txt");
		// now create a file per column
		int i = 1;
		for (final String cellLineID : matrix.getColNames()) {
			fw2.write(cellLineID + "\t" + i + "\n");
			final File outputFile = new File(matrixFile.getParent() + File.separator + "pctsea_input_" + i + ".txt");
			final FileWriter fw = new FileWriter(outputFile);
			for (final String protein : matrix.getRowNames()) {
				final Double expression = matrix.getValue(protein, cellLineID);
				fw.write(protein + "\t" + expression + "\n");
			}
			fw.close();
			System.out.println("File written at: " + outputFile.getAbsolutePath());
			i++;
		}
		fw2.close();

	}

}
