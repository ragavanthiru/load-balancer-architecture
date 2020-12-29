/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.architecture.connection;

import org.apache.http.HttpResponseFactory;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ssl.SSLIOSession;
import org.apache.http.nio.reactor.ssl.SSLMode;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This custom client connection factory can keep a map of SSLContexts and use the correct
 * SSLContext when connecting to different servers. If a SSLContext cannot be found for a
 * particular server from the specified map it uses the default SSLContext.
 */
public class ClientConnFactory {

    private final HttpResponseFactory responseFactory;
    private final ByteBufferAllocator allocator;
    private final HttpParams params;

    public ClientConnFactory(
            final HttpResponseFactory responseFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        super();
        this.responseFactory = responseFactory != null ? responseFactory : new DefaultHttpResponseFactory();
        this.allocator = allocator != null ? allocator : new HeapByteBufferAllocator();
        this.params = params != null ? params : new BasicHttpParams();
    }

    public ClientConnFactory(
            final HttpParams params) {
        this(null, null, params);
    }

    public DefaultNHttpClientConnection createConnection(
               final IOSession iosession, final HttpRoute route) {
        IOSession customSession;

            customSession = iosession;

        DefaultNHttpClientConnection conn = new DefaultNHttpClientConnection(
                   customSession, responseFactory, allocator, params);
        int timeout = HttpConnectionParams.getSoTimeout(params);
        conn.setSocketTimeout(timeout);
        return conn;
    }


}
