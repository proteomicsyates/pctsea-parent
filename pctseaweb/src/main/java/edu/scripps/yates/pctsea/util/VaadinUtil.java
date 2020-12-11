package edu.scripps.yates.pctsea.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.server.StreamResource;

public class VaadinUtil {
	public static Component createComponent(String mimeType, String fileName, InputStream stream) {
		if (mimeType.startsWith("text")) {
			return createTextComponent(stream);
		} else if (mimeType.startsWith("image")) {
			final Image image = new Image();
			try {

				final byte[] bytes = IOUtils.toByteArray(stream);
				image.getElement().setAttribute("src",
						new StreamResource(fileName, () -> new ByteArrayInputStream(bytes)));
				try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
					final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
					if (readers.hasNext()) {
						final ImageReader reader = readers.next();
						try {
							reader.setInput(in);
							image.setWidth(reader.getWidth(0) + "px");
							image.setHeight(reader.getHeight(0) + "px");
						} finally {
							reader.dispose();
						}
					}
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}

			return image;
		}
		final Div content = new Div();
		final String text = String.format("Mime type: '%s'\nSHA-256 hash: '%s'", mimeType,
				MessageDigestUtil.sha256(stream.toString()));
		content.setText(text);
		return content;

	}

	public static Component createTextComponent(InputStream stream) {
		String text;
		try {
			text = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
		} catch (final IOException e) {
			text = "exception reading stream";
		}
		return new Text(text);
	}

	public static void showErrorDialog(Exception e) {
		showErrorDialog(e.getMessage());
	}

	public static void showErrorDialog(String text) {
		showDialog(text, VaadinIcon.WARNING.create(), "OK");
	}

	public static void showInformationDialog(String text) {
		showDialog(text, VaadinIcon.INFO.create(), "OK");
	}

	public static void showDialog(String text, Icon icon, String buttonText) {
		final Dialog dialog = new Dialog();
		final Div message = new Div(new Span(text));
		dialog.setCloseOnEsc(true);
		dialog.setCloseOnOutsideClick(true);
		final Button confirmButton = new Button(buttonText, event -> {

			dialog.close();
		});
		final HorizontalLayout horizontalPanel = new HorizontalLayout(icon, message);
		final VerticalLayout verticalPanel = new VerticalLayout(horizontalPanel, confirmButton);
		verticalPanel.setHorizontalComponentAlignment(Alignment.CENTER, horizontalPanel, confirmButton);
		dialog.add(verticalPanel);
		dialog.open();
	}
}
