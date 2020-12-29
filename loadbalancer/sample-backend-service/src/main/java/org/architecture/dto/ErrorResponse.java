package org.architecture.dto;

public class ErrorResponse {

    int code;
    String message;

    public ErrorResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ErrorResponseBuilder builder(){
        return new ErrorResponseBuilder();
    }

    public static class ErrorResponseBuilder{
        int code;
        String message;

        public ErrorResponseBuilder code(int code) {
            this.code = code;
            return this;
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponse build(){
            return new ErrorResponse(code, message);
        }

    }
}
