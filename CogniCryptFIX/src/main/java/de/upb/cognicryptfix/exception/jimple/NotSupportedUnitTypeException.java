package de.upb.cognicryptfix.exception.jimple;

public class NotSupportedUnitTypeException extends JimpleException{
	
	private static final long serialVersionUID = 1L;

	public NotSupportedUnitTypeException(String message) {
        super(message);
    }
	
	public NotSupportedUnitTypeException(String message, Throwable cause) {
		super(message, cause);
	}
}
