package org.architecture.connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.params.HttpParams;
import org.architecture.description.TransportInDescription;

import java.util.*;

public class ServerConnFactoryBuilder {

    private final Log log = LogFactory.getLog(ServerConnFactoryBuilder.class);

    private final TransportInDescription transportIn;
    private final HttpHost host;
    private final String name;

    public ServerConnFactoryBuilder(final TransportInDescription transportIn, final HttpHost host) {
        this.transportIn = transportIn;
        this.host = host;
        this.name = transportIn.getName().toUpperCase(Locale.US);
    }

    public ServerConnFactory build(final HttpParams params) {
        return new ServerConnFactory(params);
    }

}
