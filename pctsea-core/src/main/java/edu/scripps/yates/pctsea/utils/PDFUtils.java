package edu.scripps.yates.pctsea.utils;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import edu.scripps.yates.utilities.pi.ConcurrentUtil;

public class PDFUtils {
	private final static Logger log = Logger.getLogger(PDFUtils.class);

	public static void createPDF(File pdfFile, File... imageFiles) throws IOException {
		createPDF(pdfFile, 1.0, imageFiles);
	}

	public static void createPDF(File pdfFile, double scaleFactor, File... imageFiles) throws IOException {

		final PDDocument doc = new PDDocument();

		for (final File imageFile : imageFiles) {
			ConcurrentUtil.sleep(1L);
			// we will add the image to the first page.
			final PDPage page = new PDPage();
			doc.addPage(page);
			// createFromFile is the easiest way with an image file
			// if you already have the image in a BufferedImage,
			// call LosslessFactory.createFromImage() instead
			final PDImageXObject pdImage = PDImageXObject.createFromFile(imageFile.getAbsolutePath(), doc);

			final PDPageContentStream contentStream = new PDPageContentStream(doc, page, AppendMode.APPEND, true, true);

			final float x_pos = page.getCropBox().getWidth();
			final float y_pos = page.getCropBox().getHeight();

			final float width = pdImage.getWidth() * (float) scaleFactor;
			final float height = pdImage.getHeight() * (float) scaleFactor;

			final float x_adjusted = (x_pos - width) / 2;
			final float y_adjusted = (y_pos - height) / 2;
//			final Matrix mt = new Matrix(1f, 0f, 0f, -1f, page.getCropBox().getLowerLeftX(),
//					page.getCropBox().getUpperRightY());
//			contentStream.transform(mt);
			contentStream.drawImage(pdImage, x_adjusted, y_adjusted, width, height);

			contentStream.close();
		}
		doc.save(pdfFile);
		doc.close();
	}

	public static void createPDF(File pdfFile, double scaleFactor, byte[]... bytesArrays) throws IOException {

		final PDDocument doc = new PDDocument();

		for (final byte[] byteArray : bytesArrays) {
			ConcurrentUtil.sleep(1L);
			// we will add the image to the first page.
			final PDPage page = new PDPage();
			doc.addPage(page);
			// createFromFile is the easiest way with an image file
			// if you already have the image in a BufferedImage,
			// call LosslessFactory.createFromImage() instead
			final PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, byteArray, null);

			final PDPageContentStream contentStream = new PDPageContentStream(doc, page, AppendMode.APPEND, true, true);

			final float x_pos = page.getCropBox().getWidth();
			final float y_pos = page.getCropBox().getHeight();

			final float width = pdImage.getWidth() * (float) scaleFactor;
			final float height = pdImage.getHeight() * (float) scaleFactor;

			final float x_adjusted = (x_pos - width) / 2;
			final float y_adjusted = (y_pos - height) / 2;
//			final Matrix mt = new Matrix(1f, 0f, 0f, -1f, page.getCropBox().getLowerLeftX(),
//					page.getCropBox().getUpperRightY());
//			contentStream.transform(mt);
			contentStream.drawImage(pdImage, x_adjusted, y_adjusted, width, height);

			contentStream.close();
		}
		doc.save(pdfFile);
		doc.close();
	}
}
