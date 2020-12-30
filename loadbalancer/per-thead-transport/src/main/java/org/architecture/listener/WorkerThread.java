package org.architecture.listener;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;
import org.apache.log4j.Logger;

import java.io.IOException;

public class WorkerThread extends Thread {
    final static Logger logger = Logger.getLogger(WorkerThread.class);
    private static final String HTTP_IN_CONN = "http.proxy.in-conn";
    private static final String HTTP_OUT_CONN = "http.proxy.out-conn";
    private static final String HTTP_CONN_KEEPALIVE = "http.proxy.conn-keepalive";

    private final HttpService httpservice;
    private final DefaultBHttpServerConnection inconn;
    private final DefaultBHttpClientConnection outconn;

    public WorkerThread(
            final HttpService httpservice,
            final DefaultBHttpServerConnection inconn,
            final DefaultBHttpClientConnection outconn) {
        super();
        this.httpservice = httpservice;
        this.inconn = inconn;
        this.outconn = outconn;
    }

    @Override
    public void run() {
        logger.debug("New connection thread");
        final HttpContext context = new BasicHttpContext(null);

        // Bind connection objects to the execution context
        context.setAttribute(HTTP_IN_CONN, this.inconn);
        context.setAttribute(HTTP_OUT_CONN, this.outconn);

        try {
            while (!Thread.interrupted()) {
                if (!this.inconn.isOpen()) {
                    this.outconn.close();
                    break;
                }

                this.httpservice.handleRequest(this.inconn, context);

                final Boolean keepalive = (Boolean) context.getAttribute(HTTP_CONN_KEEPALIVE);
                if (!Boolean.TRUE.equals(keepalive)) {
                    this.outconn.close();
                    this.inconn.close();
                    break;
                }
            }
        } catch (final ConnectionClosedException ex) {
            System.err.println("Client closed connection");
        } catch (final IOException ex) {
            System.err.println("I/O error: " + ex.getMessage());
        } catch (final HttpException ex) {
            System.err.println("Unrecoverable HTTP protocol violation: " + ex.getMessage());
        } finally {
            try {
                this.inconn.shutdown();
            } catch (final IOException ignore) {}
            try {
                this.outconn.shutdown();
            } catch (final IOException ignore) {}
        }
    }

}