package de.upb.cognicryptfix.exception.generation.crysl;

import de.upb.cognicryptfix.exception.generation.GenerationException;

public class CrySLGenerationException extends GenerationException{

	private static final long serialVersionUID = 1L;

	public CrySLGenerationException(String message) {
        super(message);
    }
	
	public CrySLGenerationException(String message, Throwable cause) {
		super(message, cause);
	}
}
