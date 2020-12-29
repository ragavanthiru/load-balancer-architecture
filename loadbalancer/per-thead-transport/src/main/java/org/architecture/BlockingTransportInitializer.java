package org.architecture;

import org.apache.http.HttpHost;
import org.architecture.thread.RequestListenerThread;

public class BlockingTransportInitializer
{
    public static void main(final String[] args) throws Exception {
        HttpHost targetHost = new HttpHost(
                "localhost",
                9001,
                "http");
        int port = 8080;
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }

        System.out.println("Reverse proxy to " + targetHost);

        final Thread t = new RequestListenerThread(port, targetHost);
        t.setDaemon(false);
        t.start();
    }
}
