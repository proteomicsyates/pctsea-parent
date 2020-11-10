package edu.scripps.yates.pctsea.utils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyPrintStream extends PrintStream {
	private final SimpleDateFormat format = new SimpleDateFormat("yyy-MM-dd HH:mm:ss,SSS");

	public MyPrintStream(OutputStream outputStream) {
		super(outputStream);
	}

	@Override
	public void println(String message) {

		super.println(getFormattedTime() + ": " + message);
	}

	private String getFormattedTime() {
		return format.format(new Date());

	}
}
