package de.upb.cognicryptfix.exception.path;

import de.upb.cognicryptfix.exception.patch.RepairException;

public class PathException extends RepairException{
	
	private static final long serialVersionUID = 1L;

	public PathException(String message) {
        super(message);
    }
	
	public PathException(String message, Throwable cause) {
		super(message, cause);
	}
}
