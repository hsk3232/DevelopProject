package edu.pnu.exception;

public class NotFoundExceptions extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
    public NotFoundExceptions(String msg) { super(msg); }
}
