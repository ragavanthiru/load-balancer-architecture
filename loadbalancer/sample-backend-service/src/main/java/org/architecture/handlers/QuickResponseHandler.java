package org.architecture.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import org.apache.log4j.Logger;
import org.architecture.dto.ResponseEntity;
import org.architecture.dto.SampleRequest;
import org.architecture.dto.StatusCode;
import org.architecture.errors.ApplicationExceptions;
import org.architecture.errors.GlobalExceptionHandler;
import org.architecture.util.HTTPConstants;

import java.io.*;

public class QuickResponseHandler extends Handler {

    private static final Logger logger = Logger.getLogger(QuickResponseHandler.class);

    public QuickResponseHandler(ObjectMapper objectMapper, GlobalExceptionHandler exceptionHandler) {
        super(objectMapper, exceptionHandler);
    }

    @Override
    protected void execute(HttpExchange exchange) throws IOException {
        byte[] response;
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if ("POST".equals(exchange.getRequestMethod())) {
            SampleRequest sampleRequest = readRequest(exchange.getRequestBody(), SampleRequest.class);
            ResponseEntity e = doPost(exchange, sampleRequest.getDelayTime());
            exchange.getResponseHeaders().putAll(e.getHeaders());
            exchange.sendResponseHeaders(e.getStatusCode().getCode(), 0);
            response = super.writeResponse(e.getBody());

        } else {
            throw ApplicationExceptions.methodNotAllowed(
                "Method " + exchange.getRequestMethod() + " is not allowed for " + exchange.getRequestURI()).get();
        }


        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }

    private ResponseEntity<String> doPost(HttpExchange exchange, int delay) {
        try {
            Thread.sleep(delay);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return new ResponseEntity<>("Hello after delay of "+delay,
                getHeaders(HTTPConstants.CONTENT_TYPE, HTTPConstants.APPLICATION_JSON), StatusCode.OK);
    }

}
