package edu.scripps.yates.pctsea.utils;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import edu.scripps.yates.pctsea.model.CellTypeBranch;

public class CellTypeBranchConverter implements Converter<String, CellTypeBranch> {
	 

		@Override
		public CellTypeBranch convert(final String source) {
			return CellTypeBranch.fromSource(source);
		}
	 

}
