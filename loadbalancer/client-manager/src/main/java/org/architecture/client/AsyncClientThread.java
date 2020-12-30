package org.architecture.client;

import org.apache.log4j.Logger;
import org.asynchttpclient.*;

import java.util.concurrent.Executor;

public class AsyncClientThread implements Runnable {
    final static Logger logger = Logger.getLogger(AsyncClientThread.class);
    private int noOfRequests;
    private int timeBetweenRequests;
    private AsyncHttpClient client;
    private Request unboundedRequest;
    private Executor executor;

    public AsyncClientThread(AsyncHttpClient client, Request unboundedRequest, int noOfRequests, int timeBetweenRequests, Executor executor){
        this.noOfRequests = noOfRequests;
        logger.debug("noOfRequests = "+noOfRequests);
        this.timeBetweenRequests = timeBetweenRequests;
        this.client = client;
        this.unboundedRequest = unboundedRequest;
        this.executor = executor;
    }

    @Override
    public void run() {
        //logger.debug("STARTING  ");
        int i=0;
        while(i<noOfRequests){
            //logger.debug("I = "+i);

            ListenableFuture<Response> listenableFuture = client
                    .executeRequest(unboundedRequest);
            listenableFuture.addListener(() -> {
                try {
                    Response response = listenableFuture.get();
                    //logger.debug(response.getStatusCode());
                } catch (Exception e){
                    e.printStackTrace();
                }
            }, executor);
            /*try {
                Thread.sleep(1000);
            }catch (Exception e){
                e.printStackTrace();
            }*/
            i++;
        }

        ClientStats d= client.getClientStats();
        System.out.println("getTotalActiveConnectionCount = "+d.getTotalActiveConnectionCount());
        System.out.println("getTotalConnectionCount = "+d.getTotalConnectionCount());
        System.out.println("getTotalIdleConnectionCount = "+d.getTotalIdleConnectionCount());
    }
}
