package org.architecture;

import org.architecture.description.TransportInDescription;
import org.architecture.listener.HttpCoreNIOListener;
import org.architecture.listener.NhttpConstants;

public class ListenerTest
{
    public static void main( String[] args ) throws Exception {
        TransportInDescription transportInDescription = new TransportInDescription("Test");
        transportInDescription.setName("HTTP NIO Listener");
        transportInDescription.setParameter("HOST_ADDRESS", "127.0.0.1");
        transportInDescription.setParameter("PARAM_PORT", "8080");
        transportInDescription.setParameter(NhttpConstants.BIND_ADDRESS, "localhost");
        HttpCoreNIOListener httpCoreNIOListener = new HttpCoreNIOListener();
        httpCoreNIOListener.init(transportInDescription);
        httpCoreNIOListener.start();

        System.out.println( "Starting HTTP server..." );
    }
}
