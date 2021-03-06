package de.upb.cognicryptfix.tag;

import java.util.List;

import de.upb.cognicryptfix.Constants;
import soot.Unit;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class RequiredExceptionHandlingTag implements Tag{

	List<Unit> units;
	
	public RequiredExceptionHandlingTag(List<Unit> tryUnits) {
		this.units = tryUnits;
	}

	public List<Unit> getUnits() {
		return units;
	}

	@Override
	public String getName() {
		return Constants.REQUIRED_EXCEPTION_HANDLING_TAG;
	}

	@Override
	public byte[] getValue() throws AttributeValueException {
		return null;
	}

}
