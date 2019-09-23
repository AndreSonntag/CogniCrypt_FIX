package de.upb.cognicryptfix.extractor.constraints;

import javax.script.ScriptException;

public interface IConstraint {

	 String getUsedValue() throws Exception;
	 String getExpectedConstraintValue() throws Exception;
}
