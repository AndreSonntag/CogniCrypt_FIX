package de.upb.cognicryptfix.exception;

public class NoEnsuredPredicateException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public NoEnsuredPredicateException(String errorMessage) {
        super(errorMessage);
    }
	
	public NoEnsuredPredicateException() {
        super();
    }

}
