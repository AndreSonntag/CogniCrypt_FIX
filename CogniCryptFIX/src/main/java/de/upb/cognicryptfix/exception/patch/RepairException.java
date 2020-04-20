package de.upb.cognicryptfix.exception.patch;

import de.upb.cognicryptfix.exception.CogniCryptFixException;

public class RepairException extends CogniCryptFixException{

	private static final long serialVersionUID = 1L;

	public RepairException(String message) {
        super(message);
    }
	
	public RepairException(String message, Throwable cause) {
		super(message, cause);
	}
}
