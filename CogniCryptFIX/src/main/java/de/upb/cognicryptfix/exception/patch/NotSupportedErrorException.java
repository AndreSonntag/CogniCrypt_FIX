package de.upb.cognicryptfix.exception.patch;

public class NotSupportedErrorException extends RepairException {

	private static final long serialVersionUID = 1L;

	public NotSupportedErrorException(String message) {
        super(message);
    }
	
	public NotSupportedErrorException(String message, Throwable cause) {
		super(message, cause);
	}
}
