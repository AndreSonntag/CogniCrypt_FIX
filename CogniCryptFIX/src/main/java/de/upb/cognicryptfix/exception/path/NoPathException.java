package de.upb.cognicryptfix.exception.path;

public class NoPathException extends PathException{

	private static final long serialVersionUID = 1L;

	public NoPathException(String message) {
        super(message);
    }
	
	public NoPathException(String message, Throwable cause) {
		super(message, cause);
	}
}
