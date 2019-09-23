package de.upb.cognicryptfix.extractor.constraints;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ValueConstraint implements IConstraint{

	private static final Logger logger = LogManager.getLogger(ValueConstraint.class.getSimpleName());
	
	private String usedValue;
	private List<String> expectedValueList;
	
	public ValueConstraint(String usedValue, List<String> expectedValueList) {
		super();
		this.usedValue = usedValue;
		this.expectedValueList = expectedValueList;
	}

	@Override
	public String getUsedValue() throws Exception {
		return usedValue;
	}

	@Override
	public String getExpectedConstraintValue() throws Exception {
		return expectedValueList.get(0);
	}

	public List<String> getExpectedValueList() {
		return expectedValueList;
	}
	
	


	
	

}
