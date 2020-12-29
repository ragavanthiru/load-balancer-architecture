package org.architecture.connection;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpParams;
import org.architecture.description.TransportOutDescription;

import javax.net.ssl.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.*;

public class ClientConnFactoryBuilder {

    private static final Log log = LogFactory.getLog(ClientConnFactoryBuilder.class);

    private final TransportOutDescription transportOut;
    private final String name;

    public ClientConnFactoryBuilder(final TransportOutDescription transportOut) {
        super();
        this.transportOut = transportOut;
        this.name = transportOut.getName().toUpperCase(Locale.US);
    }

    public ClientConnFactory createConnFactory(final HttpParams params) {

            return new ClientConnFactory(params);

    }

}
