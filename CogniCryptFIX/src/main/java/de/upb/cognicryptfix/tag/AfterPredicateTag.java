package de.upb.cognicryptfix.tag;

import de.upb.cognicryptfix.Constants;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class AfterPredicateTag implements Tag{

	public AfterPredicateTag() {}

	@Override
	public String getName() {
		return Constants.AFTER_PREDICATE_TAG;
	}

	@Override
	public byte[] getValue() throws AttributeValueException {
		return null;
	}
}
