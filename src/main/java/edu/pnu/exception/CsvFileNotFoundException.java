package edu.pnu.exception;

import java.io.Serial;

public class CsvFileNotFoundException extends RuntimeException {
	@Serial
    private static final long serialVersionUID = 1L;
    public CsvFileNotFoundException(String message) { super(message); }
}
