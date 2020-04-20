package de.upb.cognicryptfix.exception.generation.crysl;

public class NoPredicateEnsurerException extends CrySLGenerationException {
	
	private static final long serialVersionUID = 1L;

	public NoPredicateEnsurerException(String message) {
        super(message);
    }
	
	public NoPredicateEnsurerException(String message, Throwable cause) {
		super(message, cause);
	}

}
