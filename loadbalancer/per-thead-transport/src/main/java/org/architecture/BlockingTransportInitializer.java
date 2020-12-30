package org.architecture;

import org.apache.http.HttpHost;
import org.architecture.listener.RequestListenerThread;

public class BlockingTransportInitializer
{
    public static void main(final String[] args) throws Exception {
        HttpHost targetHost = new HttpHost(
                PropertySource.getProperty(TransportConstant.LISTEN_HOST),
                9001,
                "http");
        int port = Integer.parseInt(PropertySource.getProperty(TransportConstant.LISTEN_PORT));

        System.out.println("Reverse proxy to " + targetHost);

        final Thread t = new RequestListenerThread(port, targetHost);
        t.setDaemon(false);
        t.start();
    }

}
