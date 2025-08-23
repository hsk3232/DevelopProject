package edu.pnu.exception;

public class FileAccessDeniedException extends RuntimeException {
	private static final long serialVersionUID = 1L;
    public FileAccessDeniedException(String msg) { 
    	super(msg); 
    	}
}

