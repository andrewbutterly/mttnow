package org.ab.imagedownloader.urlprocessor.process;

/**
 * custom exception for validation logic.
 * have overridden fillInStackTrace as the code does not use it and it is expensive
 * */
public class InvalidInputsException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public InvalidInputsException(String message){		
		super(message);
	}
	
	@Override
	public synchronized Throwable fillInStackTrace() {
		return null;		
	}
}
