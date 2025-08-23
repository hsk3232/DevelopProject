package edu.pnu.exception;

public class InvalidCsvFormatException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
    public InvalidCsvFormatException(String msg) { 
    	super(msg); 
    	}
}