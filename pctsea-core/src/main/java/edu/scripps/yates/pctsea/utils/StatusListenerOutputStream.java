package edu.scripps.yates.pctsea.utils;

import java.io.IOException;
import java.io.OutputStream;

public class StatusListenerOutputStream extends OutputStream {

	private final StatusListener statusListener;

	private final StringBuilder sb = new StringBuilder();

	public StatusListenerOutputStream(final StatusListener statusListener) {
		this.statusListener = statusListener;
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() {
	}

	@Override
	public void write(int b) throws IOException {

		if (b == '\r') {
			return;
		}

		if (b == '\n') {
			final String text = sb.toString();
			statusListener.onStatusUpdate(text);
			sb.setLength(0);
		} else {
			sb.append((char) b);
		}
	}

}
