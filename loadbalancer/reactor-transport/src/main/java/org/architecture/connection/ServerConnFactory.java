package org.architecture.connection;

import org.apache.http.HttpRequestFactory;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ssl.SSLIOSession;
import org.apache.http.nio.reactor.ssl.SSLMode;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerConnFactory {

    private final HttpRequestFactory requestFactory;
    private final ByteBufferAllocator allocator;
    private final HttpParams params;
    
    public ServerConnFactory(
            final HttpRequestFactory requestFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        super();
        this.requestFactory = requestFactory != null ? requestFactory : new AdvancedHTTPRequestFactory();
        this.allocator = allocator != null ? allocator : new HeapByteBufferAllocator();
        this.params = params != null ? params : new BasicHttpParams();
    }
    
    public ServerConnFactory(
            final HttpParams params) {
        this(null, null, params);
    }

    public DefaultNHttpServerConnection createConnection(final IOSession iosession) {

        IOSession customSession = iosession;

        DefaultNHttpServerConnection conn =  new DefaultNHttpServerConnection(
                customSession, requestFactory, allocator, params);
        int timeout = HttpConnectionParams.getSoTimeout(params);
        conn.setSocketTimeout(timeout);
        return conn;
    }
    
    public Set<InetSocketAddress> getBindAddresses() {
        return Collections.<InetSocketAddress>emptySet();
    }

}
