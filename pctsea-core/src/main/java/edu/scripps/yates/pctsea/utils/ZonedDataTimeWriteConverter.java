package edu.scripps.yates.pctsea.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;

public class ZonedDataTimeWriteConverter implements Converter<Date, ZonedDateTime> {

	@Override
	public ZonedDateTime convert(Date date) {
		return date.toInstant().atZone(ZoneOffset.systemDefault());
	}

}
