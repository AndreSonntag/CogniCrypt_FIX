package de.upb.cognicryptfix.exception;

public class NoImplementerException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public NoImplementerException(String errorMessage) {
        super(errorMessage);
    }
	
	public NoImplementerException() {
        super();
    }
}
