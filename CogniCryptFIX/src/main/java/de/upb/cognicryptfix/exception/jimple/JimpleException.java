package de.upb.cognicryptfix.exception.jimple;

import de.upb.cognicryptfix.exception.CogniCryptFixException;
import de.upb.cognicryptfix.exception.patch.RepairException;

public class JimpleException extends RepairException{

	private static final long serialVersionUID = 1L;

	public JimpleException(String message) {
        super(message);
    }
	
	public JimpleException(String message, Throwable cause) {
		super(message, cause);
	}
}
