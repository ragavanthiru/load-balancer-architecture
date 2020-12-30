package org.architecture.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.architecture.dto.HealthResponse;
import org.architecture.dto.ResponseEntity;
import org.architecture.dto.StatusCode;
import org.architecture.errors.ApplicationExceptions;
import org.architecture.errors.GlobalExceptionHandler;
import org.architecture.util.HTTPConstants;

public class HealthHandler extends Handler {

    private static final Logger logger = Logger.getLogger(HealthHandler.class);

    public HealthHandler(ObjectMapper objectMapper, GlobalExceptionHandler exceptionHandler) {
        super(objectMapper, exceptionHandler);
    }

    @Override
    protected void execute(HttpExchange exchange) throws IOException {
        long queryStartTime = System.currentTimeMillis();
        byte[] response;
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if ("POST".equals(exchange.getRequestMethod())) {
            ResponseEntity e = doPost();
            exchange.getResponseHeaders().putAll(e.getHeaders());
            exchange.sendResponseHeaders(e.getStatusCode().getCode(), 0);
            response = super.writeResponse(e.getBody());
        } else if ("GET".equals(exchange.getRequestMethod())) {
            ResponseEntity e = doGet();
            exchange.getResponseHeaders().putAll(e.getHeaders());
            exchange.sendResponseHeaders(e.getStatusCode().getCode(), 0);
            response = super.writeResponse(e.getBody());
        }  else {
            throw ApplicationExceptions.methodNotAllowed(
                "Method " + exchange.getRequestMethod() + " is not allowed for " + exchange.getRequestURI()).get();
        }

        logger.debug("Health handler query execution time in milliseconds : " + (System.currentTimeMillis() - queryStartTime));

        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }

    private ResponseEntity<HealthResponse> doPost() {
        logger.debug("Received a request for health handler");
        HealthResponse response = new HealthResponse("up");

        return new ResponseEntity<>(response,
            getHeaders(HTTPConstants.CONTENT_TYPE, HTTPConstants.APPLICATION_JSON), StatusCode.OK);
    }

    private ResponseEntity<HealthResponse> doGet() {
        logger.debug("Received a request for health handler");
        HealthResponse response = new HealthResponse("up");

        return new ResponseEntity<>(response,
                getHeaders(HTTPConstants.CONTENT_TYPE, HTTPConstants.APPLICATION_JSON), StatusCode.OK);
    }
}
