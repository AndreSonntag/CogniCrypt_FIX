package de.upb.cognicryptfix.exception;

public class NotSupportedConstraintException extends Exception {

	private static final long serialVersionUID = 1L;

	public NotSupportedConstraintException(String errorMessage) {
        super(errorMessage);
    }
	
	public NotSupportedConstraintException() {
        super();
    }
}
