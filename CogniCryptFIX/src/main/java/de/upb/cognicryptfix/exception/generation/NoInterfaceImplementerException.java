package de.upb.cognicryptfix.exception.generation;

public class NoInterfaceImplementerException extends GenerationException {
	
	private static final long serialVersionUID = 1L;

	public NoInterfaceImplementerException(String message) {
        super(message);
    }
	
	public NoInterfaceImplementerException(String message, Throwable cause) {
		super(message, cause);
	}
}
