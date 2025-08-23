package edu.pnu.exception;

public class CsvFilePathNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;
    public CsvFilePathNotFoundException(String message) { super(message); }
}