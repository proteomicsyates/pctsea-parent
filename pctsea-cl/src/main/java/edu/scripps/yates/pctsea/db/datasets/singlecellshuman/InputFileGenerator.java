package edu.scripps.yates.pctsea.db.datasets.singlecellshuman;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.commons.io.FilenameUtils;

import edu.scripps.yates.pctsea.model.SingleCell;

public class InputFileGenerator {

	public static void main(String[] args) {
		final InputFileGenerator ifg = new InputFileGenerator();
		try {
			ifg.run();
			System.out.println("Everything ok!");
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void run() throws IOException {
		final String folderName = "C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human";

		final String[] fileNames = { "spike_fullTable.txt", "mprotein_fullTable.txt" };
		for (final String fileName : fileNames) {
			final File inputFile = new File(folderName + File.separator + fileName);
			final String baseName = FilenameUtils.getBaseName(fileName);
			// single cells
			final File singleCellsFile = new File(
					folderName + File.separator + "input files" + File.separator + baseName + "_singleCells.txt");
			final FileWriter scfw = new FileWriter(singleCellsFile);
			// single cells
			final File experimentalFile = new File(
					folderName + File.separator + "input files" + File.separator + baseName + "_experimental.txt");
			final FileWriter efw = new FileWriter(experimentalFile);

			final BufferedReader br = new BufferedReader(new FileReader(inputFile));
			String line;
			int numLine = 0;
			while ((line = br.readLine()) != null) {
				numLine++;
				final StringTokenizer tokenizer = new StringTokenizer(line, "\t");

				int colIndex = 0;

				while (tokenizer.hasMoreTokens()) {
					final String token = tokenizer.nextToken();

					// first line,
					if (numLine == 1) {

						scfw.write(token + "\t");

						if (colIndex < 1) {
							efw.write("\t" + token);
						}
					} else {
						if (colIndex != 1) {
							scfw.write(token + "\t");
						}
						if (colIndex < 2) {
							efw.write(token + "\t");
						}
					}
					colIndex++;
				}
				scfw.flush();
				efw.flush();
				scfw.write("\n");
				efw.write("\n");
			}
			scfw.close();
			efw.close();
			br.close();
		}

		// metadata file
		final File inputMetadata = new File(folderName + File.separator + "singleCellLinesTable_fullTable.txt");
		final File outputMetadata = new File(
				folderName + File.separator + "input files" + File.separator + "singleCellsMetadata.txt");
		final FileWriter fw = new FileWriter(outputMetadata);
		fw.write("cell_id\tcelltype\n");
		final BufferedReader br = new BufferedReader(new FileReader(inputMetadata));
		String line;
		int numLine = 0;
		while ((line = br.readLine()) != null) {
			numLine++;
			final String[] split = line.split("\t");
			if (numLine == 1) {
				continue;
			}
			final String cellID = split[0];
			final String cellType = SingleCell.parseCellTypeTypos(split[1]);
			fw.write(cellID + "\t" + cellType + "\n");
		}
		fw.close();
		br.close();
	}
}
