package de.upb.cognicryptfix.exception;

public class NotExpectedUnitException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public NotExpectedUnitException(String errorMessage) {
        super(errorMessage);
    }
	
	public NotExpectedUnitException() {
        super();
    }
}
