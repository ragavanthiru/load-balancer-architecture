package org.architecture;

import org.architecture.disruptor.DisruptorListener;
import org.architecture.disruptor.context.ConfigurationContext;
import org.architecture.disruptor.description.TransportInDescription;

import java.io.IOException;
import java.net.InetAddress;

public class DisruptorTransportInitializer
{
    public static void main( String[] args )
    {

        TransportInDescription transportInDescription = new TransportInDescription("Disruptor");
        try {
            transportInDescription.setAddr(InetAddress.getByName("127.0.0.1"));

        transportInDescription.setPort(4333);
        DisruptorListener server = new DisruptorListener();
        server.init(transportInDescription, new ConfigurationContext());
        server.start();
        } catch(IOException e){
            e.printStackTrace();
        }
        System.out.println( "Hello World!" );
    }
}
