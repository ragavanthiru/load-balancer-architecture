package org.architecture;


import org.apache.http.client.methods.HttpGet;
import org.architecture.client.AsyncClientThread;
import org.architecture.client.ClientThread;
import org.architecture.client.GetThread;
import org.asynchttpclient.*;
import org.asynchttpclient.util.HttpConstants;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ClientInitializer
{
    public static void main( String[] args )
    {
        if(args.length < 2){
            System.out.println( "Initialize the client cluster with client-manager.jar <no_on_connections> <no_of_requests_per_client>" );
            System.exit(0);
        }

        int noOfConnections = 100;//Integer.parseInt(args[0]);
        int noOfRequestsPerConnection = 200;//Integer.parseInt(args[1]);

        System.out.println("noOfConnections = "+noOfConnections);
        System.out.println("noOfRequestsPerConnection = "+noOfRequestsPerConnection);


        // URIs to perform GETs on
        String uri  = "http://127.0.0.1:8080/api/health";
        HttpGet httpget = new HttpGet(uri);
        // create a thread for each URI
        GetThread thread =  new GetThread(httpget);
        thread.start();


        /*DefaultAsyncHttpClientConfig.Builder clientBuilder = Dsl.config()
                .setConnectTimeout(500)
                .setThreadPoolName("Async Client")
                .setIoThreadsCount(30)
                .setConnectTimeout(10000)
                .setPooledConnectionIdleTimeout(1000)
                *//*.setProxyServer(new ProxyServer(...))*//*;


        AsyncHttpClient client = Dsl.asyncHttpClient(clientBuilder);
        System.out.println(">>>>>>>>>>>>>>"+client.getConfig().getMaxConnectionsPerHost());
        Request unboundedRequest = new RequestBuilder(HttpConstants.Methods.GET)
                .setUrl("http://localhost:8080/api/health")
                .build();
        Executor executor = Executors.newFixedThreadPool(1);

        Thread t = new Thread(new AsyncClientThread(client, unboundedRequest, noOfRequestsPerConnection, 12, executor));
        t.start();*/

            /*ExecutorService executor = Executors.newFixedThreadPool(noOfConnections);
        //ExecutorService es = Executors.newFixedThreadPool(2);
        List<Callable<Object>> todo = new ArrayList<Callable<Object>>(noOfConnections);

        for (int i=0; i<noOfConnections; i++) {
            todo.add(Executors.callable(new ClientThread(noOfRequestsPerConnection,12)));
        }

        try {
            List<Future<Object>> answers = executor.invokeAll(todo);
            for(Future<Object> ans : answers){
                System.out.println(ans);
            }
        } catch (Exception e){
            e.printStackTrace();
        }*/
    }
}
