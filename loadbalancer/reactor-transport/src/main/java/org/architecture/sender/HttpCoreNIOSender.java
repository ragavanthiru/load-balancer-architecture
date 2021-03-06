package org.architecture.sender;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.architecture.connection.ClientConnFactory;
import org.architecture.connection.ClientConnFactoryBuilder;
import org.architecture.context.MessageContext;
import org.architecture.description.TransportOutDescription;
import org.architecture.listener.BaseConstants;
import org.architecture.listener.NHttpConfiguration;
import org.architecture.listener.NhttpConstants;
import org.architecture.listener.ServerWorker;
import org.architecture.threads.NativeThreadFactory;
import org.architecture.util.Constants;
import org.architecture.util.HTTPConstants;
import org.architecture.util.NhttpUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class HttpCoreNIOSender extends AbstractHandler implements TransportSender, ManagementSupport {

    private static final Log log = LogFactory.getLog(HttpCoreNIOSender.class);

    /** The IOReactor */
    private volatile DefaultConnectingIOReactor ioReactor;
    /** The I/O dispatch */
    private volatile ClientIODispatch iodispatch;
    /** Connection factory used by this sender **/
    private volatile ClientConnFactory connFactory;
    /** The component name (to be used in logs)*/
    private volatile String name;
    /** The connection pool */
    private volatile ConnectionPool connpool;
    /** The client handler */
    private volatile ClientHandler handler;
    /** The session request callback that calls back to the message receiver with errors */
    private final SessionRequestCallback sessionRequestCallback = getSessionRequestCallback();

    /** state of the listener */
    private volatile int state = BaseConstants.STOPPED;
    /** NHttp transporter base configurations */
    private NHttpConfiguration cfg;
    /** Proxy config */
    private volatile ProxyConfig proxyConfig;
    /** Http Params **/
    private volatile HttpParams params;

    /** Socket timeout duration for HTTP connections */
    private int socketTimeout = 0;

    protected ClientConnFactoryBuilder initConnFactoryBuilder(
            final TransportOutDescription transportOut) {
        return new ClientConnFactoryBuilder(transportOut);
    }

    /**
     * Initialize the transport sender, and execute reactor in new separate thread
     * @param cfgCtx the Axis2 configuration context
     * @param transportOut the description of the http/s transport from Axis2 configuration
     * @throws AxisFault thrown on an error
     */
    public void init(TransportOutDescription transportOut)  {

        cfg = NHttpConfiguration.getInstance();
        params = new BasicHttpParams();
        params
                .setIntParameter(CoreConnectionPNames.SO_TIMEOUT,
                        cfg.getProperty(NhttpConstants.SO_TIMEOUT_SENDER, 60000))
                .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
                        cfg.getProperty(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000))
                .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE,
                        cfg.getProperty(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024))
                .setParameter(CoreProtocolPNames.USER_AGENT, "Synapse-HttpComponents-NIO");
//                .setParameter(CoreProtocolPNames.HTTP_ELEMENT_CHARSET,
//                        cfg.getStringValue(CoreProtocolPNames.HTTP_ELEMENT_CHARSET,HTTP.DEFAULT_PROTOCOL_CHARSET)); //TODO:This does not works with HTTPCore 4.3

        name = transportOut.getName().toUpperCase(Locale.US) + " Sender";
        
        ClientConnFactoryBuilder contextBuilder = initConnFactoryBuilder(transportOut);
        connFactory = contextBuilder.createConnFactory(params);

        connpool = new ConnectionPool();

        proxyConfig = new ProxyConfigBuilder().build(transportOut);
        if (log.isDebugEnabled()) {
            log.debug(proxyConfig.logProxyConfig());
        }

        Parameter param = transportOut.getParameter("warnOnHTTP500");
        if (param != null) {
            String[] warnOnHttp500 = ((String) param.getValue()).split("\\|");
            cfgCtx.setNonReplicableProperty("warnOnHTTP500", warnOnHttp500);
        }

        IOReactorConfig ioReactorConfig = new IOReactorConfig();
        ioReactorConfig.setIoThreadCount(
                NHttpConfiguration.getInstance().getClientIOWorkers());
        ioReactorConfig.setSoTimeout(
                cfg.getProperty(NhttpConstants.SO_TIMEOUT_RECEIVER, 60000));
        ioReactorConfig.setConnectTimeout(
                cfg.getProperty(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000));
        ioReactorConfig.setTcpNoDelay(cfg.getProperty(CoreConnectionPNames.TCP_NODELAY, 1) == 1);
        if (cfg.getBooleanValue("http.nio.interest-ops-queueing", false)) {
            ioReactorConfig.setInterestOpQueued(true);
        }

        try {
            String prefix = name + " I/O dispatcher";
            ioReactor = new DefaultConnectingIOReactor(
                    ioReactorConfig,
                    new NativeThreadFactory(new ThreadGroup(prefix + " thread group"), prefix));
            ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {
                public boolean handle(IOException ioException) {
                    log.warn("System may be unstable: IOReactor encountered a checked exception : " +
                            ioException.getMessage(), ioException);
                    return true;
                }

                public boolean handle(RuntimeException runtimeException) {
                    log.warn("System may be unstable: IOReactor encountered a runtime exception : " +
                            runtimeException.getMessage(), runtimeException);
                    return true;
                }
            });
        } catch (IOException e) {
            System.out.println("Error starting the IOReactor", e);
        }

       // metrics = new NhttpMetricsCollector(false, transportOut.getName());
        handler = new ClientHandler(connpool, connFactory, proxyConfig, params);
        iodispatch = new ClientIODispatch(handler, connFactory);
        final IOEventDispatch ioEventDispatch = iodispatch;

        // start the Sender in a new seperate thread
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    ioReactor.execute(ioEventDispatch);
                } catch (InterruptedIOException ex) {
                    log.fatal("Reactor Interrupted");
                } catch (IOException e) {
                    log.fatal("Encountered an I/O error: " + e.getMessage(), e);
                }
                log.info(name + " Shutdown");
            }
        }, "HttpCoreNIOSender");
        t.start();
        log.info(name + " starting");

        state = BaseConstants.STARTED;
    }

    /**
     * transport sender invocation from Axis2 core
     * @param msgContext message to be sent
     * @return the invocation response (always InvocationResponse.CONTINUE)
     * @throws AxisFault on error
     */
    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {

        // remove unwanted HTTP headers (if any from the current message)
        removeUnwantedHeaders(msgContext);

        if (AddressingHelper.isReplyRedirected(msgContext)
                && !msgContext.getReplyTo().hasNoneAddress()) {

            msgContext.setProperty(NhttpConstants.IGNORE_SC_ACCEPTED, Constants.VALUE_TRUE);
        }

        EndpointReference epr = NhttpUtil.getDestinationEPR(msgContext);
        if (epr != null) {
            if (!epr.hasNoneAddress()) {
                sendAsyncRequest(epr, msgContext);
            } else {
                handleException("Cannot send message to " + AddressingConstants.Final.WSA_NONE_URI);
            }
        } else {

            if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) != null) {
                if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) instanceof ServerWorker) {
                    sendAsyncResponse(msgContext);
                } else {
                    sendUsingOutputStream(msgContext);
                }
            } else {
                handleException("No valid destination EPR or OutputStream to send message");
            }
        }

        if (msgContext.getOperationContext() != null) {
            msgContext.getOperationContext().setProperty(
                Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Remove unwanted headers from the http response of outgoing request. These are headers which
     * should be dictated by the transport and not the user. We remove these as these may get
     * copied from the request messages
     *
     * @param msgContext the Axis2 Message context from which these headers should be removed
     */
    private void removeUnwantedHeaders(MessageContext msgContext) {
        Map transportHeaders = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
        Map excessHeaders = (Map) msgContext.getProperty(NhttpConstants.EXCESS_TRANSPORT_HEADERS);

        if (transportHeaders != null && !transportHeaders.isEmpty()) {
            removeUnwantedHeadersFromHeaderMap(transportHeaders, cfg);
        }

        if (excessHeaders != null && !excessHeaders.isEmpty()) {
            removeUnwantedHeadersFromHeaderMap(excessHeaders, cfg);
        }
    }

    /**
     * Remove unwanted headers from the given header map.
     *
     * @param headers Header map
     * @param nHttpConfiguration NHttp transporter base configurations
     */
    private void removeUnwantedHeadersFromHeaderMap(Map headers, NHttpConfiguration nHttpConfiguration) {

        Iterator iter = headers.keySet().iterator();
        while (iter.hasNext()) {
            String headerName = (String) iter.next();
            if (HTTP.CONN_DIRECTIVE.equalsIgnoreCase(headerName)
                || HTTP.TRANSFER_ENCODING.equalsIgnoreCase(headerName)
                || HTTP.CONTENT_TYPE.equalsIgnoreCase(headerName)
                || HTTP.CONTENT_LEN.equalsIgnoreCase(headerName)) {
                iter.remove();
            }

            if (HTTP.SERVER_HEADER.equalsIgnoreCase(headerName)
                && !nHttpConfiguration.isPreserveHttpHeader(HTTP.SERVER_HEADER)) {
                iter.remove();
            }

            if (HTTP.USER_AGENT.equalsIgnoreCase(headerName)
                && !nHttpConfiguration.isPreserveHttpHeader(HTTP.USER_AGENT)) {
                iter.remove();
            }

            if (HTTP.DATE_HEADER.equalsIgnoreCase(headerName)
                && !nHttpConfiguration.isPreserveHttpHeader(HTTP.DATE_HEADER)) {
                iter.remove();
            }

        }
    }

    /**
     * Send the request message asynchronously to the given EPR
     * @param epr the destination EPR for the message
     * @param msgContext the message being sent
     * @throws AxisFault on error
     */
    private void sendAsyncRequest(EndpointReference epr, MessageContext msgContext) throws AxisFault {
        try {
            URL url = new URL(epr.getAddress());
            String scheme = url.getProtocol() != null ? url.getProtocol() : "http";
            String hostname = url.getHost();
            int port = url.getPort();
            if (port == -1) {
                // use default
                if ("http".equals(scheme)) {
                    port = 80;
                } else if ("https".equals(scheme)) {
                    port = 443;
                }
            }
            HttpHost target = new HttpHost(hostname, port, scheme);
            boolean secure = "https".equalsIgnoreCase(target.getSchemeName());

            HttpHost proxy = proxyConfig.selectProxy(target);
            msgContext.setProperty(NhttpConstants.PROXY_PROFILE_TARGET_HOST, target.getHostName());

            HttpRoute route;
            if (proxy != null) {
                route = new HttpRoute(target, null, proxy, secure);
            } else {
                route = new HttpRoute(target, null, secure);
            }
            Axis2HttpRequest axis2Req = new Axis2HttpRequest(epr, route, msgContext);
            Object timeout = msgContext.getProperty(NhttpConstants.SEND_TIMEOUT);
            if (timeout != null && timeout instanceof Long) {
                axis2Req.setTimeout( (int) ((Long) timeout).longValue());
            }

            NHttpClientConnection conn = connpool.getConnection(route);

            // Ensure MessageContext has a ClientConnectionDebug attached before we start streaming
            ServerConnectionDebug scd = (ServerConnectionDebug)
                    msgContext.getProperty(ServerHandler.SERVER_CONNECTION_DEBUG);

            ClientConnectionDebug ccd;
            if (scd != null) {
                ccd = scd.getClientConnectionDebug();
                if (ccd == null) {
                    ccd = new ClientConnectionDebug(scd);
                    scd.setClientConnectionDebug(ccd);
                }
                ccd.recordRequestStartTime(conn, axis2Req);
                msgContext.setProperty(ClientHandler.CLIENT_CONNECTION_DEBUG, ccd);
            }

            if (conn == null) {
                HttpHost host = route.getProxyHost() != null ? route.getProxyHost() : route.getTargetHost();
                ioReactor.connect(new InetSocketAddress(host.getHostName(), host.getPort()),
                        null, axis2Req, sessionRequestCallback);
                if (log.isDebugEnabled()) {
                    log.debug("A new connection established to : " + route);
                }
            } else {
                conn.setSocketTimeout(socketTimeout); // reinitialize timeouts for the pooled connection
                try {
                    handler.submitRequest(conn, axis2Req);
                    if (log.isDebugEnabled()) {
                        log.debug("An existing connection reused to : " + hostname + ":" + port);
                    }
                } catch (ConnectionClosedException e) {
                    ioReactor.connect(new InetSocketAddress(hostname, port),
                            null, axis2Req, sessionRequestCallback);
                    if (log.isDebugEnabled()) {
                        log.debug("A new connection established to : " + hostname + ":" + port);
                    }
                }
            }
            try {
                axis2Req.streamMessageContents();
            } catch (AxisFault af) {
                throw af;
            }

        } catch (MalformedURLException e) {
            handleException("Malformed destination EPR : " + epr.getAddress(), e);
        }
    }

    /**
     * Send the passed in response message, asynchronously
     * @param msgContext the message context to be sent
     * @throws AxisFault on error
     */
    private void sendAsyncResponse(MessageContext msgContext) throws AxisFault {

        int contentLength = extractContentLength(msgContext);

        // remove unwanted HTTP headers (if any from the current message)
        removeUnwantedHeaders(msgContext);

        //Check whether original messageType value is changed. if not set the correct value to engage correct message formatter
        String cType = (String) msgContext.getProperty(NhttpConstants.MC_CONTENT_TYPE);
        String messageType = (String) msgContext.getProperty(NhttpConstants.MESSAGE_TYPE);
        String oriMessageType = (String) msgContext.getProperty(NhttpConstants.ORIGINAL_MESSAGE_TYPE);
        if (cType != null && cType.indexOf(HTTPConstants.MEDIA_TYPE_MULTIPART_RELATED) != -1
                && messageType != null && messageType.equals(oriMessageType)) {
            msgContext.setProperty(NhttpConstants.MESSAGE_TYPE, HTTPConstants.MEDIA_TYPE_MULTIPART_RELATED);
        }

        Map transportHeaders = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);

        ServerWorker worker = (ServerWorker) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);
        NHttpServerConnection conn = worker.getConn();

        if (null == conn.getHttpRequest()) {
            // We'll get here if the connection is already closed
            // this is a special case we dropped the connection when message size exceeds the user defined threshold
            if (conn.getContext().getAttribute(NhttpConstants.CONNECTION_DROPPED) != null &&
                (Boolean) conn.getContext().getAttribute(NhttpConstants.CONNECTION_DROPPED)) {
                //already submitted response for this case, hence return
                return;
            }
        }
        HttpResponse response = worker.getResponse();

        OMOutputFormat format = NhttpUtil.getOMOutputFormat(msgContext);
        MessageFormatter messageFormatter =
                MessageFormatterDecoratorFactory.createMessageFormatterDecorator(msgContext);
        Boolean noEntityBody = (Boolean) msgContext.getProperty(NhttpConstants.NO_ENTITY_BODY);
        if (noEntityBody == null || Boolean.FALSE == noEntityBody) {
            response.setHeader(
                HTTP.CONTENT_TYPE,
                messageFormatter.getContentType(msgContext, format, msgContext.getSoapAction()));
        }else if( Boolean.TRUE == noEntityBody){
            ((BasicHttpEntity)response.getEntity()).setChunked(false);
            ((BasicHttpEntity)response.getEntity()).setContentLength(0);

            // Since HTTP HEAD request doesn't contain message body content length of the is set to be 0. To handle
            // content length 0 while serving head method, content length of the backend response is set as the content
            // as synapse cannot calculate content length without providing message body.
            if (transportHeaders.get(NhttpConstants.HTTP_REQUEST_METHOD) != null &&
                NhttpConstants.HTTP_HEAD.equals(transportHeaders.get(NhttpConstants.HTTP_REQUEST_METHOD)) &&
                transportHeaders.get(NhttpConstants.ORIGINAL_CONTENT_LEN) != null ) {

                ((BasicHttpEntity) response.getEntity()).setContentLength(
                        Long.parseLong(String.valueOf(transportHeaders.get(NhttpConstants.ORIGINAL_CONTENT_LEN))));
                transportHeaders.remove(NhttpConstants.ORIGINAL_CONTENT_LEN);
                transportHeaders.remove(NhttpConstants.HTTP_REQUEST_METHOD);
            }
        }
        response.setStatusCode(determineHttpStatusCode(msgContext, response));

        //Override the Standard Reason Phrase
        if (msgContext.getProperty(NhttpConstants.HTTP_REASON_PHRASE) != null
                && !msgContext.getProperty(NhttpConstants.HTTP_REASON_PHRASE).equals("")) {
            response.setReasonPhrase(msgContext.getProperty(NhttpConstants.HTTP_REASON_PHRASE).toString());
        }

        // set any transport headers
        if (transportHeaders != null && !transportHeaders.values().isEmpty()) {
            Iterator iter = transportHeaders.keySet().iterator();
            while (iter.hasNext()) {
                Object header = iter.next();
                Object value = transportHeaders.get(header);
                if (value != null && header instanceof String && value instanceof String) {
                    response.addHeader((String) header, (String) value);
                    
                    String excessProp = NhttpConstants.EXCESS_TRANSPORT_HEADERS;
                    
                    Map map = (Map)msgContext.getProperty(excessProp);
                    if (map != null && map.get(header) != null) {
                    	log.debug("Number of excess values for "
								+ header
								+ " header is : "
								+ ((Collection) (map.get(header)))
										.size());
						
						for (Iterator iterator = map.keySet().iterator(); iterator
								.hasNext();) {
							String key = (String) iterator.next();
							
							for (String excessVal : (Collection<String>)map.get(key)) {
							    if (header.equals(key)) {
								    response.addHeader((String)header, (String) excessVal);
								}
							}
							
						}
					}
                }
            }
        }

        boolean forceContentLength = msgContext.isPropertyTrue(
                NhttpConstants.FORCE_HTTP_CONTENT_LENGTH);
        boolean forceContentLengthCopy = msgContext.isPropertyTrue(
                NhttpConstants.COPY_CONTENT_LENGTH_FROM_INCOMING);

        BasicHttpEntity entity = (BasicHttpEntity) response.getEntity();

        MetricsCollector lstMetrics = worker.getServiceHandler().getMetrics();
        try {
            if (forceContentLength) {
                entity.setChunked(false);
                if (forceContentLengthCopy && contentLength > 0) {
                    entity.setContentLength(contentLength);
                } else {
                    setStreamAsTempData(entity, messageFormatter, msgContext, format);
                }
            }

            worker.getServiceHandler().commitResponse(worker.getConn(), response);
            lstMetrics.reportResponseCode(response.getStatusLine().getStatusCode());
            OutputStream out = worker.getOutputStream();

            /*
             * if this is a dummy message to handle http 202 case with non-blocking IO
             * write an empty byte array as body
             */
            if (msgContext.isPropertyTrue(NhttpConstants.SC_ACCEPTED)
                || Boolean.TRUE == noEntityBody) {
                out.write(new byte[0]);
            } else {
                if (forceContentLength) {
                    if (forceContentLengthCopy && contentLength > 0) {
                        messageFormatter.writeTo(msgContext, format, out, false);
                    } else {
                        writeMessageFromTempData(out, msgContext);
                    }
                } else {
                    messageFormatter.writeTo(msgContext, format, out, false);
                }
            }
            out.close();
            if (lstMetrics != null) {
                lstMetrics.incrementMessagesSent();
            }

        } catch (ProtocolException e) {
            System.out.println(e + " (Synapse may be trying to send an exact response more than once )");
        } catch (HttpException e) {
            /*if (lstMetrics != null) {
                lstMetrics.incrementFaultsSending();
            }*/
            handleException("Unexpected HTTP protocol error sending response to : " +
                worker.getRemoteAddress(), e);
        } catch (ConnectionClosedException e) {
            /*if (lstMetrics != null) {
                lstMetrics.incrementFaultsSending();
            }*/
            System.out.println("Connection closed by client : " + worker.getRemoteAddress());
        } catch (IllegalStateException e) {
            /*if (lstMetrics != null) {
                lstMetrics.incrementFaultsSending();
            }*/
            System.out.println("Connection closed by client : " + worker.getRemoteAddress());
        } catch (IOException e) {
            /*if (lstMetrics != null) {
                lstMetrics.incrementFaultsSending();
            }*/
            handleException("IO Error sending response message to : " +
                worker.getRemoteAddress(), e);
        } catch (Exception e) {
            /*if (lstMetrics != null) {
                lstMetrics.incrementFaultsSending();
            }*/
            handleException("General Error sending response message to : " +
                worker.getRemoteAddress(), e);
        }

        InputStream is = worker.getIs();
        if (is != null) {
            try {
                is.close();
            } catch (IOException ignore) {}
        }
    }

    /**
     * Extract the content length from the incoming message
     *
     * @param msgContext current MessageContext
     * @return the length of the message
     */
    private int extractContentLength(MessageContext msgContext) {
        Map headers = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);

        if (headers == null || headers.isEmpty()) {
            return -1;
        }

        for (Object o : headers.keySet()) {
            String headerName = (String) o;
            if (HTTP.CONTENT_LEN.equalsIgnoreCase(headerName)) {
                Object value = headers.get(headerName);

                if (value != null && value instanceof String) {
                    try {
                        return Integer.parseInt((String) value);
                    } catch (NumberFormatException e) {
                        return -1;
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Write the stream to a temporary storage and calculate the content length
     * @param entity HTTPEntity
     * @param messageFormatter message formatter
     * @param msgContext current message context
     * @param format message format
     * @throws IOException if an exception occurred while writing data
     */
    private void setStreamAsTempData(BasicHttpEntity entity, MessageFormatter messageFormatter,
                                     MessageContext msgContext, OMOutputFormat format)
            throws IOException {
        TemporaryData serialized = new TemporaryData(256, 4096, "http-nio_", ".dat");
        OutputStream out = serialized.getOutputStream();
        try {
            messageFormatter.writeTo(msgContext, format, out, true);
        } finally {
            out.close();
        }
        msgContext.setProperty(NhttpConstants.SERIALIZED_BYTES, serialized);
        entity.setContentLength(serialized.getLength());
    }

    /**
     * Take the data from temporary storage and write it to the output stream
     * @param out output stream output stream
     * @param msgContext messagecontext
     * @throws IOException if an exception occurred while writing data
     */
    private void writeMessageFromTempData(OutputStream out, MessageContext msgContext)
            throws IOException {
        TemporaryData serialized =
                (TemporaryData) msgContext.getProperty(NhttpConstants.SERIALIZED_BYTES);
        try {
            serialized.writeTo(out);
        } finally {
            serialized.release();
        }
    }

    /**
     * Determine the HttpStatusCodedepending on the message type processed <br>
     * (normal response versus fault response) as well as Axis2 message context properties set
     * via Synapse configuration or MessageBuilders.
     * 
     * @see org.apache.synapse.transport.nhttp.NhttpConstants#FAULTS_AS_HTTP_200
     * @see org.apache.synapse.transport.nhttp.NhttpConstants#HTTP_SC
     * 
     * @param msgContext the Axis2 message context 
     * @param response the HTTP response object
     * 
     * @return the HTTP status code to set in the HTTP response object
     */
    private int determineHttpStatusCode(MessageContext msgContext, HttpResponse response) {
        
        int httpStatus = HttpStatus.SC_OK;
        
        // retrieve original status code (if present)
        if (response.getStatusLine() != null) {
            httpStatus = response.getStatusLine().getStatusCode();
        }
        
        // if this is a dummy message to handle http 202 case with non-blocking IO
        // set the status code to 202
        if (msgContext.isPropertyTrue(NhttpConstants.SC_ACCEPTED)) {
            httpStatus = HttpStatus.SC_ACCEPTED;
        } else {            
            
            // is this a fault message
            boolean handleFault = 
                msgContext.getEnvelope().getBody().hasFault() || msgContext.isProcessingFault();
            
            // shall faults be transmitted with HTTP 200
            boolean faultsAsHttp200 =
                NhttpConstants.TRUE.equalsIgnoreCase(
                        (String) msgContext.getProperty(NhttpConstants.FAULTS_AS_HTTP_200));
            
            // Set HTTP status code to 500 if this is a fault case and we shall not use HTTP 200
            if (handleFault && !faultsAsHttp200) {
                httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            } else if (handleFault & faultsAsHttp200) {
                return HttpStatus.SC_OK;
            }

            /* 
            * Any status code previously set shall be overwritten with the value of the following
            * message context property if it is set.
            */
            Object statusCode = msgContext.getProperty(NhttpConstants.HTTP_SC);
            if (statusCode != null) {
                try {
                    httpStatus = Integer.parseInt(
                            msgContext.getProperty(NhttpConstants.HTTP_SC).toString());
                } catch (NumberFormatException e) {
                    log.warn("Unable to set the HTTP status code from the property " 
                            + NhttpConstants.HTTP_SC + " with value: " + statusCode);
                }
            }
        }
        
        return httpStatus;
    }

    private void sendUsingOutputStream(MessageContext msgContext) throws AxisFault {

        OMOutputFormat format = NhttpUtil.getOMOutputFormat(msgContext);
        MessageFormatter messageFormatter =
                MessageFormatterDecoratorFactory.createMessageFormatterDecorator(msgContext);
        OutputStream out = (OutputStream) msgContext.getProperty(MessageContext.TRANSPORT_OUT);

        if (msgContext.isServerSide()) {
            OutTransportInfo transportInfo =
                (OutTransportInfo) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);

            if (transportInfo != null) {
                transportInfo.setContentType(
                messageFormatter.getContentType(msgContext, format, msgContext.getSoapAction()));
            } else {
                throw new AxisFault(Constants.OUT_TRANSPORT_INFO + " has not been set");
            }
        }

        try {
            messageFormatter.writeTo(msgContext, format, out, false);
            out.close();
        } catch (IOException e) {
            handleException("IO Error sending response message", e);
        }
    }


    public void cleanup(MessageContext msgContext) {
        // do nothing
    }

    public void stop() {
        if (state == BaseConstants.STOPPED) return;
        try {
            ioReactor.shutdown();
            handler.stop();
            state = BaseConstants.STOPPED;
        } catch (IOException e) {
            log.warn("Error shutting down IOReactor", e);
        }
        //mbeanSupport.unregister();
        //metrics.destroy();
    }

    /**
     * Return a SessionRequestCallback which gets notified of a connection failure
     * or an error during a send operation. This method finds the corresponding
     * Axis2 message context for the outgoing request, and find the message receiver
     * and sends a fault message back to the message receiver that is marked as
     * related to the outgoing request
     * @return a Session request callback
     */
    private SessionRequestCallback getSessionRequestCallback() {
        return new SessionRequestCallback() {
            public void completed(SessionRequest request) {
                if (log.isDebugEnabled() && request.getSession() != null &&
                        request.getSession().getLocalAddress() != null) {
                    log.debug("Connected to remote address : "
                            + request.getSession().getRemoteAddress()
                            + " from local address : " + request.getSession().getLocalAddress());
                }
            }

            public void failed(SessionRequest request) {
                handleError(request, NhttpConstants.CONNECTION_FAILED, 
                    "Connection refused or failed for : " + request.getRemoteAddress() + ", " +
                    "IO Exception occured : " + request.getException().getMessage());
            }

            public void timeout(SessionRequest request) {
                handleError(request, NhttpConstants.CONNECT_TIMEOUT,
                    "Timeout connecting to : " + request.getRemoteAddress());
                request.cancel();
            }

            public void cancelled(SessionRequest request) {
                handleError(request, NhttpConstants.CONNECT_CANCEL,
                    "Connection cancelled for : " + request.getRemoteAddress());
            }

            private void handleError(SessionRequest request, int errorCode, String errorMessage) {
                if (request.getAttachment() != null &&
                    request.getAttachment() instanceof Axis2HttpRequest) {

                    Axis2HttpRequest axis2Request = (Axis2HttpRequest) request.getAttachment();
                    if (!axis2Request.isCompleted()) {
                        handler.markRequestCompletedWithError(
                            axis2Request, errorCode,  errorMessage,  null);
                    }
                }
            }
        };
    }


    private void handleException(String msg, Exception e) {
        e.printStackTrace();
        System.out.println(msg+ e);
    }

    private void handleException(String msg) {
        System.out.println(msg);
    }

    public void pause() {
        if (state != BaseConstants.STARTED) return;
        state = BaseConstants.PAUSED;
        System.out.println(name + " Paused");
    }

    public void resume() {
        if (state != BaseConstants.PAUSED) return;
        state = BaseConstants.STARTED;
        log.info(name + " Resumed");
    }

    public void maintenenceShutdown(long millis)  {
        if (state != BaseConstants.STARTED) return;
        try {
            long start = System.currentTimeMillis();
            ioReactor.shutdown(millis);
            state = BaseConstants.STOPPED;
            System.out.println("Sender shutdown in : " + (System.currentTimeMillis() - start) / 1000 + "s");
        } catch (IOException e) {
            handleException("Error shutting down the IOReactor for maintenence", e);
        }
    }

    /**
     * Returns the number of active threads processing messages
     * @return number of active threads processing messages
     */    
    public int getActiveThreadCount() {
        return handler.getActiveCount();
    }

    /**
     * Returns the number of requestes queued in the thread pool
     * @return queue size
     */
    public int getQueueSize() {
        return handler.getQueueSize();
    }

}
