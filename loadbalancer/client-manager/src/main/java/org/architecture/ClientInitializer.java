package org.architecture;


import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.routing.HttpRoute;
import org.architecture.client.ClientConstant;
import org.architecture.client.WorkerThread;

public class ClientInitializer
{
    public static void main( String[] args )
    {
        String loadbalancerHost = PropertySource.getProperty(ClientConstant.LOAD_BALANCER_HOST);
        int loadbalancerPort = Integer.parseInt(PropertySource.getProperty(ClientConstant.LOAD_BALANCER_PORT));
        int noOfParallelConnections = Integer.parseInt(PropertySource.getProperty(ClientConstant.PARALLEL_CONNECTIONS));

        HttpHost host = new HttpHost(loadbalancerHost, loadbalancerPort);
        HttpRoute route = new HttpRoute(host);


        // URI to invoke
        String uri  = "http://"+loadbalancerHost+":"+loadbalancerPort+"/api/health";
        HttpGet httpget = new HttpGet(uri);

        for(int i = 0;i < noOfParallelConnections; i++){
            WorkerThread thread =  new WorkerThread(host,route,httpget);
            thread.start();
        }
    }
}
