package edu.pnu.exception;

public class FileUploadException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
    public FileUploadException(String msg) { 
    	super(msg); 
    	}
}