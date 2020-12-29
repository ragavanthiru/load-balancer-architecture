package org.architecture.listener;

import org.apache.axiom.util.UIDGenerator;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.reactor.ssl.SSLIOSession;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.architecture.context.MessageContext;
import org.architecture.util.Constants;
import org.architecture.util.NhttpUtil;


import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.*;

/**
 * Processes an incoming request through Axis2. An instance of this class would be created to
 * process each unique request
 */
public class ServerWorker implements Runnable {

    private static final Log log = LogFactory.getLog(ServerWorker.class);

    /** Protocol scheme name */
    private final String schemeName;
    /** the message handler to be used */
    private final ServerHandler serverHandler;
    /** the underlying http connection */
    private final NHttpServerConnection conn;
    /** the http request */
    private final HttpRequest request;
    /** the http response message (which the this would be creating) */
    private final HttpResponse response;
    /** the input stream to read the incoming message body */
    private final InputStream is;
    /** the output stream to write the response message body */
    private final OutputStream os;
    /** Weather we should do rest dispatching or not */
    private boolean isRestDispatching;
    /** WSDL processor for Get requests */
    private HttpGetRequestProcessor httpGetRequestProcessor;
    /** the incoming message to be processed */
    private final MessageContext msgContext;
    
    private static final String SOAPACTION   = "SOAPAction";
    private static final String LOCATION     = "Location";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String TEXT_HTML    = "text/html";
    private static final String TEXT_XML     = "text/xml";
    /**
     * Save requesting user IP address for logging - even during response processing when
     * the connection may be closed
     */
    private String remoteAddress = null;

    /**
     * Create a new server side worker to process an incoming message and optionally begin creating
     * its output. This however does not force the processor to write a response back as the
     * traditional servlet service() method, but creates the background required to write the
     * response, if one would be created.
     *
     *
     * @param conn the underlying http connection
     * @param serverHandler the handler of the server side messages
     * @param request the http request received (might still be in the process of being streamed)
     * @param is the stream input stream to read the request body
     * @param response the response to be populated if applicable
     * @param os the output stream to write the response body if one is applicable
     */
    public ServerWorker(
            final String schemeName,
            final NHttpServerConnection conn,
            final ServerHandler serverHandler,
            final HttpRequest request, 
            final InputStream is,
            final HttpResponse response, 
            final OutputStream os,
            final boolean isRestDispatching,
            final HttpGetRequestProcessor httpGetRequestProcessor) {

        this.schemeName = schemeName;
        this.conn = conn;
        this.serverHandler = serverHandler;
        this.request = request;
        this.response = response;
        this.is = is;
        this.os = os;
        this.isRestDispatching = isRestDispatching;
        this.httpGetRequestProcessor = httpGetRequestProcessor;
        this.msgContext = createMessageContext(request);
        conn.getContext().setAttribute(NhttpConstants.SERVER_WORKER_INIT_TIME,
            System.currentTimeMillis());
    }

    private MessageContext createMessageContext(HttpRequest request) {

        MessageContext msgContext = new MessageContext();
        //msgContext.setMessageID(UIDGenerator.generateURNString());

        // There is a discrepency in what I thought, Axis2 spawns a new threads to
        // send a message if this is TRUE - and I want it to be the other way
        msgContext.setProperty(MessageContext.CLIENT_API_NON_BLOCKING, Boolean.FALSE);
        //msgContext.setConfigurationContext(cfgCtx);
        /*if ("https".equalsIgnoreCase(schemeName)) {
            msgContext.setTransportOut(cfgCtx.getAxisConfiguration()
                .getTransportOut(Constants.TRANSPORT_HTTPS));
            msgContext.setTransportIn(cfgCtx.getAxisConfiguration()
                .getTransportIn(Constants.TRANSPORT_HTTPS));
            msgContext.setIncomingTransportName(Constants.TRANSPORT_HTTPS);

            SSLIOSession session = (SSLIOSession) (conn.getContext()).getAttribute(SSLIOSession.SESSION_KEY);
            //set SSL certificates to message context if SSLVerifyClient parameter is set
            if (session != null && msgContext.getTransportIn() != null
                && msgContext.getTransportIn().getParameter(NhttpConstants.SSL_VERIFY_CLIENT) != null) {
                try {
                    msgContext.setProperty(NhttpConstants.SSL_CLIENT_AUTH_CERT_X509,
                                           session.getSSLSession().getPeerCertificateChain());
                } catch (SSLPeerUnverifiedException e) {
                    //Peer Certificate Chain may not be available always.(in case of verify client is optional)
                    if (log.isTraceEnabled()) {
                        log.trace("Peer certificate chain is not available for MsgContext " + msgContext.getMessageID());
                    }
                }
            }
        } else {*/
            /*msgContext.setTransportOut(cfgCtx.getAxisConfiguration()
                .getTransportOut(Constants.TRANSPORT_HTTP));*/
            /*msgContext.setTransportIn(cfgCtx.getAxisConfiguration()
                .getTransportIn(Constants.TRANSPORT_HTTP));*/
            msgContext.setIncomingTransportName(Constants.TRANSPORT_HTTP);
        //}
        msgContext.setProperty(Constants.OUT_TRANSPORT_INFO, this);
        // the following statement causes the soap session services to be failing - ruwan        
        // msgContext.setServiceGroupContextId(UUIDGenerator.getUUID());
        msgContext.setServerSide(true);
        msgContext.setProperty(
            Constants.Configuration.TRANSPORT_IN_URL, request.getRequestLine().getUri());

        // http transport header names are case insensitive 
        Map<String, String> headers = new TreeMap<String, String>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        
        for (Header header : request.getAllHeaders()) {
        	
        	String headerName = header.getName();
        	
        	// if this header is already added
        	if(headers.containsKey(headerName)){
        		/* this is a multi-value header */
        		// generate the key
        		String key = NhttpConstants.EXCESS_TRANSPORT_HEADERS;
        		// get the old value
        		String oldValue = headers.get(headerName);
        		// adds additional values to a list in a property of message context
        		Map map;
        		if(msgContext.getProperty(key) != null){
        			map = (Map) msgContext.getProperty(key);
        			map.put(headerName, oldValue);
        		} else{
        			map = new MultiValueMap();
        			map.put(headerName, oldValue);
        			// set as a property in message context
        			msgContext.setProperty(key, map);
        		}
        		
        	}
            headers.put(header.getName(), header.getValue());
        }
        msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, headers);

        // find the remote party IP address and set it to the message context
        if (conn instanceof HttpInetConnection) {
            HttpContext httpContext = conn.getContext();
            HttpInetConnection inetConn = (HttpInetConnection) conn;
            InetAddress remoteAddr = inetConn.getRemoteAddress();
            if (remoteAddr != null) {
                httpContext.setAttribute(NhttpConstants.CLIENT_REMOTE_ADDR, remoteAddr);
                httpContext.setAttribute(NhttpConstants.CLIENT_REMOTE_PORT, inetConn.getRemotePort());
                msgContext.setProperty(
                        MessageContext.REMOTE_ADDR, remoteAddr.getHostAddress());
                msgContext.setProperty(
                        NhttpConstants.REMOTE_HOST, NhttpUtil.getHostName(remoteAddr));
                remoteAddress = remoteAddr.getHostAddress();
            }
        }

        /*msgContext.setProperty(RequestResponseTransport.TRANSPORT_CONTROL,
                new HttpCoreRequestResponseTransport(msgContext));*/

        /*msgContext.setProperty(ServerHandler.SERVER_CONNECTION_DEBUG,
            conn.getContext().getAttribute(ServerHandler.SERVER_CONNECTION_DEBUG));*/

        msgContext.setProperty(NhttpConstants.NHTTP_INPUT_STREAM, is);
        msgContext.setProperty(NhttpConstants.NHTTP_OUTPUT_STREAM, os);
        return msgContext;
    }

    /**
     * Process the incoming request
     */
    @SuppressWarnings({"unchecked"})
    public void run() {

        //CustomLogSetter.getInstance().clearThreadLocalContent();
        conn.getContext().setAttribute(NhttpConstants.SERVER_WORKER_START_TIME, System.currentTimeMillis());
        conn.getContext().setAttribute(NhttpConstants.SERVER_WORKER_THREAD_ID, Thread.currentThread().getId());
        String method = request.getRequestLine().getMethod().toUpperCase();
        msgContext.setProperty(Constants.Configuration.HTTP_METHOD,
            request.getRequestLine().getMethod());
        try {
            if (NHttpConfiguration.getInstance().isHttpMethodDisabled(method)) {

                    handleException("Unsupported method : " + method, null);

            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        String oriUri = request.getRequestLine().getUri();
        String restUrlPostfix = NhttpUtil.getRestUrlPostfix(oriUri, "");

        msgContext.setProperty(NhttpConstants.REST_URL_POSTFIX, restUrlPostfix);
        String servicePrefix = oriUri.substring(0, oriUri.indexOf(restUrlPostfix));
        System.out.println( "servicePrefix = " +servicePrefix);
        if (servicePrefix.indexOf("://") == -1) {
            HttpInetConnection inetConn = (HttpInetConnection) conn;
            InetAddress localAddr = inetConn.getLocalAddress();
            if (localAddr != null) {
                servicePrefix = schemeName + "://" + localAddr.getHostName() + ":" + inetConn.getLocalPort() + servicePrefix;
            }
        }
        msgContext.setProperty(NhttpConstants.SERVICE_PREFIX, servicePrefix);
        if ("GET".equals(method)) {           
            httpGetRequestProcessor.process(request, response,
                    msgContext, conn, os, isRestDispatching);
        } else if ("POST".equals(method)) {
            try {
                byte[] body = is.readAllBytes();
                System.out.println(">>>>>>> "+ new String(body));
            }catch (IOException e){
                e.printStackTrace();
            }

            processEntityEnclosingMethod();
        } else if ("PUT".equals(method)) {
            processEntityEnclosingMethod();
        } else if ("HEAD".equals(method)) {
            processNonEntityEnclosingMethod();
        } else if ("OPTIONS".equals(method)) {
            processNonEntityEnclosingMethod();
        } else if ("DELETE".equals(method)) {
            processGetAndDelete("DELETE");
        } else if ("TRACE".equals(method)) {
            processNonEntityEnclosingMethod();
        } else if ("PATCH".equals(method)) {
            processEntityEnclosingMethod();            
        } else {
            handleException("Unsupported method : " + method, null);
        }

        // here the RequestResponseTransport plays an important role when it comes to
        // dual channel invocation. This is becasue we need to ACK to the request once the request
        // is received to synapse. Otherwise we will not be able to support the single channel
        // invocation within the actual service and synapse for a dual channel request from the
        // client.
        if (isAckRequired()) {
            String respWritten = "";
            /*if (msgContext.getOperationContext() != null) {
                respWritten = (String) msgContext.getOperationContext().getProperty(
                        Constants.RESPONSE_WRITTEN);
            }*/
            boolean respWillFollow = !Constants.VALUE_TRUE.equals(respWritten)
                    && !"SKIP".equals(respWritten);
            /*boolean acked = (((RequestResponseTransport) msgContext.getProperty(
                    RequestResponseTransport.TRANSPORT_CONTROL)).getStatus()
                    == RequestResponseTransport.RequestResponseTransportStatus.ACKED);
            boolean forced = msgContext.isPropertyTrue(NhttpConstants.FORCE_SC_ACCEPTED);
            boolean nioAck = msgContext.isPropertyTrue("NIO-ACK-Requested", false);*/
     
            //if (respWillFollow || acked || forced || nioAck) {

                /*if (!nioAck) {
                    if (log.isDebugEnabled()) {
                        log.debug("Sending 202 Accepted response for MessageID : " +
                                msgContext.getMessageID() +
                                " response written : " + respWritten +
                                " response will follow : " + respWillFollow +
                                " acked : " + acked + " forced ack : " + forced);
                    }
                    response.setStatusCode(HttpStatus.SC_ACCEPTED);
                } else {*/
                    //if (log.isDebugEnabled()) {
                        System.out.println("Sending ACK response with status "
                               // + msgContext.getProperty(NhttpConstants.HTTP_SC)
                                + ", for MessageID : " );
                    //}
                    response.setStatusCode(Integer.parseInt("200"
                            /*msgContext.getProperty(NhttpConstants.HTTP_SC).toString()*/));

            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContentType("text/plain");
            entity.setChunked(true);

            try {
                //os.write("test payload".getBytes());

                final byte[] bodyData = "test payload123".getBytes();
                entity.setContent(new ByteArrayInputStream(bodyData));
                entity.setContentLength(bodyData.length);

            }catch(Exception r){
                r.printStackTrace();
            }

                    response.setEntity(entity);

                    Map<String, String> responseHeaders = (Map<String, String>)
                            msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
					if (responseHeaders != null) {
						for (String headerName : responseHeaders.keySet()) {
							response.addHeader(headerName,
									responseHeaders.get(headerName));

							String excessProp = NhttpConstants.EXCESS_TRANSPORT_HEADERS;

							Map map = (Map) msgContext.getProperty(excessProp);
							if (map != null) {
								System.out.println("Number of excess values for "
										+ headerName
										+ " header is : "
										+ ((Collection) (map.get(headerName)))
												.size());

								for (Iterator iterator = map.keySet()
										.iterator(); iterator.hasNext();) {
									String key = (String) iterator.next();

									for (String excessVal : (Collection<String>) map
											.get(key)) {
										response.addHeader(headerName,
												(String) excessVal);
									}

								}
							}
						}

					}
                //}

               /* if (metrics != null) {
                    metrics.incrementMessagesSent();
                }*/

                try {
                	
                	/*
                     * Remove Content-Length and Transfer-Encoding headers, if already present.
                     * */
                    response.removeHeaders(HTTP.TRANSFER_ENCODING);
                    response.removeHeaders(HTTP.CONTENT_LEN);
                	
                    serverHandler.commitResponse(conn, response);

                } catch (HttpException e) {
                    /*if (metrics != null) {
                        metrics.incrementFaultsSending();
                    }*/
                    handleException("Unexpected HTTP protocol error : " + e.getMessage(), e);
                } catch (ConnectionClosedException e) {
                    /*if (metrics != null) {
                        metrics.incrementFaultsSending();
                    }*/
                    log.warn("Connection closed by client (Connection closed)");
                } catch (IllegalStateException e) {
                    /*if (metrics != null) {
                        metrics.incrementFaultsSending();
                    }*/
                    log.warn("Connection closed by client (Buffer closed)");
                } catch (IOException e) {
                    /*if (metrics != null) {
                        metrics.incrementFaultsSending();
                    }*/
                    handleException("IO Error sending response message", e);
                } catch (Exception e) {
                    /*if (metrics != null) {
                        metrics.incrementFaultsSending();
                    }*/
                    handleException("General Error sending response message", e);
                }

                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ignore) {}
                }

                // make sure that the output stream is flushed and closed properly
                try {
                    os.flush();
                    os.close();
                } catch (IOException ignore) {}
           // }
        }

        System.out.println("*******************");
    }

    private boolean isAckRequired() {

        // This condition is a bit complex but cannot simplify any further.
        /*if (msgContext != null) {
            if (msgContext.getOperationContext() != null &&
                    (!msgContext.getOperationContext().getAxisOperation().isControlOperation() ||
                            msgContext.isPropertyTrue(NhttpConstants.FORCE_SC_ACCEPTED))) {

                return true;
            } else if (msgContext.isPropertyTrue("NIO-ACK-Requested", false)) {
                return true;
            }
        }*/

        return true;
    }

    private void processEntityEnclosingMethod() {

        try {
            Header contentType = request.getFirstHeader(HTTP.CONTENT_TYPE);
            String contentTypeStr = contentType != null ?
                    contentType.getValue() : inferContentType();
            /*String charSetEncoding = BuilderUtil.getCharSetEncoding(contentTypeStr);
            msgContext.setProperty(
                    Constants.Configuration.CHARACTER_SET_ENCODING, charSetEncoding);*/

               /* RESTUtil.processPOSTRequest(msgContext, is, os,
                        request.getRequestLine().getUri(), contentTypeStr, isRestDispatching);*/


        } catch (Exception e) {
            handleException("Error processing POST request ", e);
        }
    }

    private boolean isRest(String contentType) {
        return true;
        /*return contentType != null &&
                contentType.indexOf(SOAP11Constants.SOAP_11_CONTENT_TYPE) == -1 &&
                contentType.indexOf(SOAP12Constants.SOAP_12_CONTENT_TYPE) == -1;*/
    }

    private String inferContentType() {
        System.out.println( "inferContentType = " );
       /* Parameter param = cfgCtx.getAxisConfiguration().
                getParameter(NhttpConstants.REQUEST_CONTENT_TYPE);
        if (param != null) {
            return param.getValue().toString();
        }*/
        return null;
    }

    /**
     * Process HEAD, DELETE, TRACE, OPTIONS
     */
    private void processNonEntityEnclosingMethod() {
        System.out.println( "processNonEntityEnclosingMethod = " );
        /*try {
            RESTUtil.processURLRequest(
                msgContext, os, null,
                request.getRequestLine().getUri());

        } catch (AxisFault e) {
            handleException("Error processing " + request.getRequestLine().getMethod() +
                " request for : " + request.getRequestLine().getUri(), e);
        }*/
    }

    private void processGetAndDelete(String method) {
        System.out.println( "processGetAndDelete = " );
        /*try {
            RESTUtil.processGetAndDeleteRequest(
                    msgContext, os, request.getRequestLine().getUri(),
                    request.getFirstHeader(HTTP.CONTENT_TYPE), method, isRestDispatching);
            // do not let the output stream close (as by default below) since
            // we are serving this GET/DELETE request through the Synapse engine
        } catch (AxisFault axisFault) {
            handleException("Error processing " + method + " request for: " +
                    request.getRequestLine().getUri(), axisFault);
        }*/

    }

    private void handleException(String msg, Exception e) {
        System.out.println(msg + "  "+ e);
        /*if (e == null) {
            log.error(msg);
        } else {
            log.error(msg, e);
        }
        Exception newException = e;
        if (e == null) {
            newException = new Exception(msg);
        }

        try {
            MessageContext faultContext = MessageContextBuilder.createFaultMessageContext(
                    msgContext, newException);
            AxisEngine.sendFault(faultContext);

        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.addHeader(CONTENT_TYPE, TEXT_XML);
            conn.getContext().setAttribute(NhttpConstants.FORCE_CONNECTION_CLOSE, true);
            serverHandler.commitResponseHideExceptions(conn, response);

            try {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ignore) {}
                }

                String body = "<html><body><h1>" + "Failed to process the request" +
                         "</h1><p>"+ msg + "</p>";
                if (e != null) {
                    body = body + "<p>"+ e.getMessage() + "</p></body></html>";
                }
                if (ex != null) {
                    body = body + "<p>"+ ex.getMessage() + "</p></body></html>";
                }
                os.write(body.getBytes());
                os.flush();
                os.close();
            } catch (IOException ignore) {}
        }*/
    }

    public HttpResponse getResponse() {
        return response;
    }

    public OutputStream getOutputStream() {
        return os;
    }

    public InputStream getIs() {
        return is;
    }

    public ServerHandler getServiceHandler() {
        return serverHandler;
    }

    public NHttpServerConnection getConn() {
        return conn;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

}
