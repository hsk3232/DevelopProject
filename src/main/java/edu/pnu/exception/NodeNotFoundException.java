package edu.pnu.exception;

public class NodeNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;
    public NodeNotFoundException(String message) {
        super(message);
    }
}