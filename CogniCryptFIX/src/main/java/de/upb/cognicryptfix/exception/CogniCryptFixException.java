package de.upb.cognicryptfix.exception;

public class CogniCryptFixException extends Exception{
	
	
	private static final long serialVersionUID = 1L;

	public CogniCryptFixException(String errorMessage) {
        super(errorMessage);
    }
	
	public CogniCryptFixException(String message, Throwable cause) {
		super(message, cause);
	}
}
