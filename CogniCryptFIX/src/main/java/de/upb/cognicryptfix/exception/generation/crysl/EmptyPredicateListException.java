package de.upb.cognicryptfix.exception.generation.crysl;

public class EmptyPredicateListException extends CrySLGenerationException{

	private static final long serialVersionUID = 1L;

	public EmptyPredicateListException(String message) {
        super(message);
    }
	
	public EmptyPredicateListException(String message, Throwable cause) {
		super(message, cause);
	}
}
