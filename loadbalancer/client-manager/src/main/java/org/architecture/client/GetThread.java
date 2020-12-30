package org.architecture.client;


import org.apache.http.HttpClientConnection;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GetThread extends Thread {
    final static Logger logger = Logger.getLogger(GetThread.class);
    private CloseableHttpClient httpClient;
    private final HttpClientContext  context;
    private HttpGet httpget;
    HttpRequestExecutor exeRequest;
    HttpClientConnection conn;

    public GetThread(HttpGet httpget) {


        //HttpClientContext context = HttpClientContext.create();
        //HttpClientConnectionManager connMrg = new BasicHttpClientConnectionManager();
        //HttpRoute route = new HttpRoute(new HttpHost("127.0.0.1", 8080));
// Request new connection. This can be a long process
        //ConnectionRequest connRequest = connMrg.requestConnection(route, null);
// Wait for connection up to 10 sec
        //HttpClientConnection conn = connRequest.get(10, TimeUnit.SECONDS);
        //conn.

        //BasicHttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
        /*cm.setMaxTotal(20);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);
        // Increase max connections for localhost:80 to 50
        HttpHost localhost = new HttpHost("localhost", 8080);
        cm.setMaxPerRoute(new HttpRoute(localhost), 50);*/
       /* CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();*/
        /*HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient httpClient1 = builder

                .setConnectionManager(cm)

                .build();*/

        BasicHttpClientConnectionManager basicConnManager = new BasicHttpClientConnectionManager();
        this.context = HttpClientContext.create();
// low level
        HttpRoute route = new HttpRoute(new HttpHost("localhost", 8080));
        ConnectionRequest connRequest = basicConnManager.requestConnection(route, null);
        try {
            this.conn = connRequest.get(100, TimeUnit.SECONDS);

            basicConnManager.connect(this.conn, route, 1000, this.context);
            basicConnManager.routeComplete(this.conn, route, this.context);
            this.exeRequest = new HttpRequestExecutor();
            this.context.setTargetHost((new HttpHost("localhost", 8080)));
            HttpGet get = new HttpGet("http://localhost:8080/api/health");

            //basicConnManager.releaseConnection(conn, null, -1, TimeUnit.SECONDS);
            // high level


            /*this.httpClient = HttpClients.custom()
                    .setConnectionManager(basicConnManager)

                    .build();*/
            this.httpget = get;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < 3; i++) {
            try {

                logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> I = "+i);
                //httpget = new HttpGet("http://localhost:8080/api/health");

                HttpResponse httpResponse = this.exeRequest.execute(httpget, this.conn, this.context);/*this.httpClient.execute(httpget);*/
                int httpStatus = httpResponse.getStatusLine().getStatusCode();
                logger.debug(httpStatus);
                //EntityUtils.consumeQuietly(httpResponse.getEntity());
                logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> I = "+i);
            } catch (Exception e) {
                logger.debug("Unable to download log from \n" + e);
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

}