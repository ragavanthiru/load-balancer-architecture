package org.architecture.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.protocol.HTTP;
import org.architecture.context.MessageContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

/**
 * Default http Get processor implementation for Synapse.
 */
public class DefaultHttpGetProcessor implements HttpGetRequestProcessor {
    private static final Log log = LogFactory.getLog(DefaultHttpGetProcessor.class);

    private static final String LOCATION = "Location";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String TEXT_HTML = "text/html";
    private static final String TEXT_XML = "text/xml";

    /*protected ConfigurationContext cfgCtx;*/

    protected ServerHandler serverHandler;

    public void init(/*ConfigurationContext cfgCtx,*/ ServerHandler serverHandler) throws Exception {
        //this.cfgCtx = cfgCtx;
        this.serverHandler = serverHandler;
    }

    /**
     * Process the HTTP GET request.
     *
     * @param request    The HttpRequest
     * @param response   The HttpResponse
     * @param msgContext The MessageContext
     * @param conn       The NHttpServerConnection
     * @param os         The OutputStream
     */
    public void process(HttpRequest request,
                        HttpResponse response,
                        MessageContext msgContext,
                        NHttpServerConnection conn,
                        OutputStream os,
                        boolean isRestDispatching) {

        String uri = request.getRequestLine().getUri();
        System.out.println("process = "+uri);

        /*String servicePath = cfgCtx.getServiceContextPath();
        if (!servicePath.startsWith("/")) {
            servicePath = "/" + servicePath;
        }

        String serviceName = getServiceName(request);

        Map<String, String> parameters = new HashMap<String, String>();
        int pos = uri.indexOf("?");
        if (pos != -1) {
            msgContext.setTo(new EndpointReference(uri.substring(0, pos)));
            StringTokenizer st = new StringTokenizer(uri.substring(pos + 1), "&");
            while (st.hasMoreTokens()) {
                String param = st.nextToken();
                pos = param.indexOf("=");
                if (pos != -1) {
                    parameters.put(param.substring(0, pos), param.substring(pos + 1));
                } else {
                    parameters.put(param, null);
                }
            }
        } else {
            msgContext.setTo(new EndpointReference(uri));
        }

        if (isServiceListBlocked(uri)) {
            response.setStatusCode(HttpStatus.SC_FORBIDDEN);
            serverHandler.commitResponseHideExceptions(conn,  response);
        } else if (uri.equals("/favicon.ico")) {
            response.setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY);
            response.addHeader(LOCATION, "http://wso2.org/favicon.ico");
            serverHandler.commitResponseHideExceptions(conn, response);

//        } else if (!uri.startsWith(servicePath)) {
//            response.setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY);
//            response.addHeader(LOCATION, servicePath + "/");
//            serverHandler.commitResponseHideExceptions(conn, response);

        } else if (serviceName != null && parameters.containsKey("wsdl")) {
            generateWsdl(request, response, msgContext,
                    conn, os, serviceName, parameters, isRestDispatching);
            return;
        } else if (serviceName != null && parameters.containsKey("wsdl2")) {
            generateWsdl2(request, response, msgContext,
                    conn, os, serviceName, isRestDispatching);
            return;
        } else if (serviceName != null && parameters.containsKey("xsd")) {
            generateXsd(request, response, msgContext, conn, os, serviceName, parameters, isRestDispatching);
            return;
        } else if (serviceName != null && parameters.containsKey("info")) {
            generateServiceDetailsPage(response, conn, os, serviceName);
        } else if (uri.startsWith(servicePath) &&
                (serviceName == null || serviceName.length() == 0)) {
            generateServicesList(response, conn, os, servicePath);
        } else {*/
            processGetAndDelete(request, response, msgContext,
                    conn, os, "GET", isRestDispatching);
            return;
        //}

        // make sure that the output stream is flushed and closed properly
        //closeOutputStream(os);
    }

    private void closeOutputStream(OutputStream os) {
        try {
            os.flush();
            os.close();
        } catch (IOException ignore) {
        }
    }

    /**
     * Is the incoming URI is requesting service list and http.block_service_list=true in
     * nhttp.properties
     * @param incomingURI incoming URI
     * @return whether to proceed with incomingURI

     */
    protected boolean isServiceListBlocked(String incomingURI) {
        String isBlocked = NHttpConfiguration.getInstance().isServiceListBlocked();

        return (("/services").equals(incomingURI) || ("/services" + "/").equals(incomingURI)) &&
               Boolean.parseBoolean(isBlocked);
    }

    /**
     * Returns the service name.
     *
     * @param request HttpRequest
     * @return service name as a String
     */
    protected String getServiceName(HttpRequest request) {
        String uri = request.getRequestLine().getUri();
        System.out.println("getServiceName = "+uri);

        /*String servicePath = cfgCtx.getServiceContextPath();
        if (!servicePath.startsWith("/")) {
            servicePath = "/" + servicePath;
        }

        String serviceName = null;
        if (uri.startsWith(servicePath)) {
            serviceName = uri.substring(servicePath.length());
            if (serviceName.startsWith("/")) {
                serviceName = serviceName.substring(1);
            }
            if (serviceName.indexOf("?") != -1) {
                serviceName = serviceName.substring(0, serviceName.indexOf("?"));
            }
        } else {
            // this may be a custom URI
            String incomingURI = request.getRequestLine().getUri();

            Map serviceURIMap = (Map) cfgCtx.getProperty(NhttpConstants.EPR_TO_SERVICE_NAME_MAP);
            if (serviceURIMap != null) {
                Set keySet = serviceURIMap.keySet();
                for (Object key : keySet) {
                    if (incomingURI.toLowerCase().contains(((String) key).toLowerCase())) {
                        return (String) serviceURIMap.get(key);
                    }
                }
            }
        }

        if (serviceName != null) {
            int opnStart = serviceName.indexOf("/");
            if (opnStart != -1) {
                serviceName = serviceName.substring(0, opnStart);
            }
        }
        return serviceName;*/
        return null;
    }

    /**
     * Generates the services list.
     *
     * @param response    HttpResponse
     * @param conn        NHttpServerConnection
     * @param os          OutputStream
     * @param servicePath service path of the service
     */
    protected void generateServicesList(HttpResponse response,
                                        NHttpServerConnection conn,
                                        OutputStream os, String servicePath) {
        /*try {
            byte[] bytes = getServicesHTML(
                    servicePath.endsWith("/") ? "" : servicePath + "/").getBytes();
            response.addHeader(CONTENT_TYPE, TEXT_HTML);
            serverHandler.commitResponseHideExceptions(conn, response);
            os.write(bytes);

        } catch (IOException e) {
            handleBrowserException(response, conn, os,
                    "Error generating services list", e);
        }*/
    }

    /**
     * Generates service details page.
     *
     * @param response    HttpResponse
     * @param conn        NHttpServerConnection
     * @param os          OutputStream
     * @param serviceName service name
     */
    protected void generateServiceDetailsPage(HttpResponse response,
                                              NHttpServerConnection conn,
                                              OutputStream os, String serviceName) {
        /*AxisService service = cfgCtx.getAxisConfiguration().
                getServices().get(serviceName);
        if (service != null) {
            String parameterValue = (String) service.getParameterValue("serviceType");
            if ("proxy".equals(parameterValue) && !isWSDLProvidedForProxyService(service)) {
                handleBrowserException(response, conn, os,
                        "No WSDL was provided for the Service " + serviceName +
                                ". A WSDL cannot be generated.", null);
            }
            try {
                byte[] bytes =
                        HTTPTransportReceiver.printServiceHTML(serviceName, cfgCtx).getBytes();
                response.addHeader(CONTENT_TYPE, TEXT_HTML);
                serverHandler.commitResponseHideExceptions(conn, response);
                os.write(bytes);

            } catch (IOException e) {
                handleBrowserException(response, conn, os,
                        "Error generating service details page for : " + serviceName, e);
            }
        } else {
            handleBrowserException(response, conn, os,
                    "Invalid service : " + serviceName, null);
        }*/
    }



    /**
     * Calls the RESTUtil to process GET and DELETE Request
     *
     * @param request           HttpRequest
     * @param response          HttpResponse
     * @param msgContext        MessageContext
     * @param conn              NHttpServerConnection
     * @param os                OutputStream
     * @param method            HTTP method, either GET or DELETE
     * @param isRestDispatching weather transport should do rest dispatching
     */
    protected void processGetAndDelete(HttpRequest request, HttpResponse response,
                                       MessageContext msgContext,
                                       NHttpServerConnection conn, OutputStream os,
                                       String method, boolean isRestDispatching) {
        /*try {
            RESTUtil.processGetAndDeleteRequest(
                    msgContext, os, request.getRequestLine().getUri(),
                    request.getFirstHeader(HTTP.CONTENT_TYPE), method, isRestDispatching);
            // do not let the output stream close (as by default below) since
            // we are serving this GET/DELETE request through the Synapse engine
        } catch (AxisFault axisFault) {
            handleException(response, msgContext, conn, os,
                    "Error processing " + method + " request for: " +
                            request.getRequestLine().getUri(), axisFault);
        }*/

    }

    /**
     * Handles exception.
     *
     * @param response   HttpResponse
     * @param msgContext MessageContext
     * @param conn       NHttpServerConnection
     * @param os         OutputStream
     * @param msg        message
     * @param e          Exception
     */
    protected void handleException(HttpResponse response, MessageContext msgContext,
                                   NHttpServerConnection conn,
                                   OutputStream os, String msg, Exception e) {
        if (e == null) {
            log.error(msg);
        } else {
            log.error(msg, e);
        }

        if (e == null) {
            e = new Exception(msg);
        }

        try {
            /*MessageContext faultContext = MessageContextBuilder.createFaultMessageContext(
                    msgContext, e);*/
            //AxisEngine.sendFault(faultContext);

        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.addHeader(CONTENT_TYPE, TEXT_XML);
            serverHandler.commitResponseHideExceptions(conn, response);

            try {
                os.write(msg.getBytes());
                if (ex != null) {
                    os.write(ex.getMessage().getBytes());
                }
            } catch (IOException ignore) {
            }

            if (conn != null) {
                try {
                    conn.shutdown();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /**
     * Handles browser exception.
     *
     * @param response HttpResponse
     * @param conn     NHttpServerConnection
     * @param os       OutputStream
     * @param msg      message
     * @param e        Exception
     */
    protected void handleBrowserException(HttpResponse response,
                                          NHttpServerConnection conn, OutputStream os,
                                          String msg, Exception e) {
        if (e == null) {
            log.error(msg);
        } else {
            log.error(msg, e);
        }

        if (!response.containsHeader(HTTP.TRANSFER_ENCODING)) {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.setReasonPhrase(msg);
            response.addHeader(CONTENT_TYPE, TEXT_HTML);
            serverHandler.commitResponseHideExceptions(conn, response);
            try {
                os.write(msg.getBytes());
                os.close();
            } catch (IOException ignore) {
            }
        }

        if (conn != null) {
            try {
                conn.shutdown();
            } catch (IOException ignore) {
            }
        }
    }


    /**
     * Whatever this method returns as the IP is ignored by the actual http/s listener when
     * its getServiceEPR is invoked. This was originally copied from axis2
     *
     * @return Returns String.
     * @throws SocketException if the socket can not be accessed
     */
    protected static String getIpAddress() throws SocketException {
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        String address = "127.0.0.1";

        while (e.hasMoreElements()) {
            NetworkInterface netface = (NetworkInterface) e.nextElement();
            Enumeration addresses = netface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress ip = (InetAddress) addresses.nextElement();
                if (!ip.isLoopbackAddress() && isIP(ip.getHostAddress())) {
                    return ip.getHostAddress();
                }
            }
        }
        return address;
    }

    protected static boolean isIP(String hostAddress) {
        return hostAddress.split("[.]").length == 4;
    }

}
