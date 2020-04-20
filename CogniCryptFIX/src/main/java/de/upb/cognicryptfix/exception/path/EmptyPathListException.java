package de.upb.cognicryptfix.exception.path;

public class EmptyPathListException extends PathException{

	private static final long serialVersionUID = 1L;

	public EmptyPathListException(String message) {
        super(message);
    }
	
	public EmptyPathListException(String message, Throwable cause) {
		super(message, cause);
	}
}
