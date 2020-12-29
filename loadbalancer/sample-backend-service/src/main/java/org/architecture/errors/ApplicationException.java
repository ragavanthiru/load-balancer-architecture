package org.architecture.errors;

public class ApplicationException extends RuntimeException {

    private final int code;

    ApplicationException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
