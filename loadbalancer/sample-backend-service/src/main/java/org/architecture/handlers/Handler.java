package org.architecture.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.architecture.errors.ApplicationExceptions;
import org.architecture.errors.GlobalExceptionHandler;

import java.io.InputStream;

public abstract class Handler {

    private final ObjectMapper objectMapper;
    private final GlobalExceptionHandler exceptionHandler;

    public Handler(ObjectMapper objectMapper,
        GlobalExceptionHandler exceptionHandler) {
        this.objectMapper = objectMapper;
        this.exceptionHandler = exceptionHandler;
    }

    protected static Headers getHeaders(String key, String value) {
        Headers headers = new Headers();
        headers.set(key, value);
        return headers;
    }

    public void handle(HttpExchange exchange) {
        try {
            execute(exchange);
        } catch(Exception e){
            exceptionHandler.handle(e, exchange);
        }
    }

    protected abstract void execute(HttpExchange exchange) throws Exception;

    protected <T> T readRequest(InputStream is, Class<T> type) {
        T obj = null;
        try {
            obj = objectMapper.readValue(is, type);
        } catch(Exception e){
            e.printStackTrace();
            ApplicationExceptions.invalidRequest();
        }
        return obj;
    }

    protected <T> byte[] writeResponse(T response) {
        byte[] obj = null;
        try {
            obj = objectMapper.writeValueAsBytes(response);
        } catch(Exception e){
            ApplicationExceptions.invalidRequest();
        }
        return obj;
    }
}
