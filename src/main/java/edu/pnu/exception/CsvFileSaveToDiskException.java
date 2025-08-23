package edu.pnu.exception;

public class CsvFileSaveToDiskException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
    public CsvFileSaveToDiskException(String msg) { 
    	super(msg); 
    	}
}