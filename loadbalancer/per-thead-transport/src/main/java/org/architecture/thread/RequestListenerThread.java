package org.architecture.thread;

import org.apache.http.HttpHost;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.protocol.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestListenerThread extends Thread {

    private final HttpHost target;
    private final ServerSocket serversocket;
    private final HttpService httpService;
    protected ExecutorService threadPool =
            Executors.newFixedThreadPool(10);

    public RequestListenerThread(final int port, final HttpHost target) throws IOException {
        this.target = target;
        this.serversocket = new ServerSocket(port);

        // Set up HTTP protocol processor for incoming connections
        final HttpProcessor inhttpproc = new ImmutableHttpProcessor(
                new ResponseDate(),
                new ResponseServer("Test/1.1"),
                new ResponseContent(),
                new ResponseConnControl());

        // Set up HTTP protocol processor for outgoing connections
        final HttpProcessor outhttpproc = new ImmutableHttpProcessor(
                new RequestContent(),
                new RequestTargetHost(),
                new RequestConnControl(),
                new RequestUserAgent("Test/1.1"),
                new RequestExpectContinue(true));

        // Set up outgoing request executor
        final HttpRequestExecutor httpexecutor = new HttpRequestExecutor();

        // Set up incoming request handler
        final UriHttpRequestHandlerMapper reqistry = new UriHttpRequestHandlerMapper();
        reqistry.register("*", new ProxyHandler(
                this.target,
                outhttpproc,
                httpexecutor));

        // Set up the HTTP service
        this.httpService = new HttpService(inhttpproc, reqistry);
    }

    @Override
    public void run() {
        System.out.println("Listening on port " + this.serversocket.getLocalPort());
        while (!Thread.interrupted()) {
            try {
                final int bufsize = 8 * 1024;
                // Set up incoming HTTP connection
                final Socket insocket = this.serversocket.accept();


                final DefaultBHttpServerConnection inconn = new DefaultBHttpServerConnection(bufsize);
                System.out.println("Incoming connection from " + insocket.getInetAddress());
                inconn.bind(insocket);

                // Set up outgoing HTTP connection
                final DefaultBHttpClientConnection outconn = new DefaultBHttpClientConnection(bufsize);

                this.threadPool.execute(
                        new WorkerThread(this.httpService, inconn, outconn));

            } catch (final InterruptedIOException ex) {
                break;
            } catch (final IOException e) {
                System.err.println("I/O error initialising connection thread: "
                        + e.getMessage());
                break;
            }
        }
    }
}
