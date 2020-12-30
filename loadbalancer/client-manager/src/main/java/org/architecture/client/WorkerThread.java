package org.architecture.client;


import org.apache.http.HttpClientConnection;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.log4j.Logger;
import org.architecture.PropertySource;

import java.util.concurrent.TimeUnit;

public class WorkerThread extends Thread {
    final static Logger logger = Logger.getLogger(WorkerThread.class);

    private final HttpClientContext  context;
    private HttpRoute route;
    private HttpGet httpget;
    private HttpRequestExecutor exeRequest;;
    private int noOfRequestsPerConnection;
    private HttpClientConnection conn;

    public WorkerThread(HttpHost host, HttpRoute route, HttpGet httpget) {
        BasicHttpClientConnectionManager basicConnManager = new BasicHttpClientConnectionManager();

        this.context = HttpClientContext.create();
        this.route = route;
        ConnectionRequest connRequest = basicConnManager.requestConnection(route, null);

        try {
            this.conn = connRequest.get(1000, TimeUnit.SECONDS);
            basicConnManager.connect(this.conn,  route, 100000, this.context);
            basicConnManager.routeComplete(this.conn, route, this.context);
            this.exeRequest = new HttpRequestExecutor();
            this.context.setTargetHost(host);
            this.httpget = httpget;
            this.noOfRequestsPerConnection = Integer.parseInt(PropertySource.getProperty(ClientConstant.REQUESTS_PER_CONNECTION));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < noOfRequestsPerConnection; i++) {
            try {
                HttpResponse httpResponse = this.exeRequest.execute(httpget, this.conn, this.context);
                int httpStatus = httpResponse.getStatusLine().getStatusCode();

                Thread.sleep(1000);
            } catch (Exception e) {
                logger.debug("Invocation failed \n" + e);
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

}