package de.upb.cognicryptfix.exception.path;

public class NoCallFoundException extends PathException{
	
	private static final long serialVersionUID = 1L;

	public NoCallFoundException(String message) {
        super(message);
    }
	
	public NoCallFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
