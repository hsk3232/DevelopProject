package edu.pnu.exception;

public class CsvFileNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;
    public CsvFileNotFoundException(String message) { super(message); }
}
