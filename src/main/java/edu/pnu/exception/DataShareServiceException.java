package edu.pnu.exception;

public class DataShareServiceException extends RuntimeException {
	private static final long serialVersionUID = 1L;
    public DataShareServiceException(String message) {
        super(message);
    }
    public DataShareServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
