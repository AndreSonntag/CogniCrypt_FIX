package de.upb.cognicryptfix.tag;

import java.util.List;

import de.upb.cognicryptfix.Constants;
import soot.Unit;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class EnsuredPredicateTag implements Tag{

	List<Unit> units;
	
	public EnsuredPredicateTag(List<Unit> units) {
		this.units = units;
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
