package de.upb.cognicryptfix.exception.jimple;

public class NotExpectedUnitException extends JimpleException {
	
	private static final long serialVersionUID = 1L;

	public NotExpectedUnitException(String message) {
        super(message);
    }
	
	public NotExpectedUnitException(String message, Throwable cause) {
		super(message, cause);
	}
}
