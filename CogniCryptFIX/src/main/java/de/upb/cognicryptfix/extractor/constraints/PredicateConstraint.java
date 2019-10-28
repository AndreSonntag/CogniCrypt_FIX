package de.upb.cognicryptfix.extractor.constraints;

import java.util.List;

import de.upb.cognicryptfix.utils.Pair;

public class PredicateConstraint implements IConstraint{

	private String usedValue;
	private String expectedConstraintValue;
	
	public PredicateConstraint(String usedValue, String expectedConstraintValue) {
		super();
		this.usedValue = usedValue;
		this.expectedConstraintValue = expectedConstraintValue;
	}
	
	@Override
	public String getUsedValue() throws Exception {
		return usedValue;
	}

	@Override
	public String getExpectedConstraintValue() throws Exception {
		return expectedConstraintValue;
	}

}
