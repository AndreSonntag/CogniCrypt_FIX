package de.upb.cognicryptfix.extractor.constraints;

public interface IConstraint {

	 String getUsedValue() throws Exception;
	 String getExpectedConstraintValue() throws Exception;
}
