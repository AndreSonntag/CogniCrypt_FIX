package de.upb.cognicryptfix.exception.generation;

import de.upb.cognicryptfix.exception.patch.RepairException;

public class GenerationException extends RepairException{

	private static final long serialVersionUID = 1L;

	public GenerationException(String message) {
        super(message);
    }
	
	public GenerationException(String message, Throwable cause) {
		super(message, cause);
	}
}
