package org.architecture.connection;

import java.util.Locale;

public final class Scheme {

    private final String name;
    private final int defaultPort;
    private final boolean ssl;
    
    public Scheme(String name, int defaultPort, boolean ssl) {
        super();
        this.name = name.toLowerCase(Locale.US);
        this.defaultPort = defaultPort;
        this.ssl = ssl;
    }

    public String getName() {
        return name;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public boolean isSSL() {
        return ssl;
    }

    @Override
    public String toString() {
        return name;
    }
    
}
