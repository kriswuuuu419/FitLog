package com.cjlu.fitlog.exception;

public class FitLogException extends RuntimeException {
    public FitLogException(String message) {
        super(message);
    }

    public FitLogException(String message, Throwable cause) {
        super(message, cause);
    }
}
