package org.architecture.listener;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpServerConnection;
import org.architecture.context.MessageContext;

import java.io.OutputStream;

/**
 * This interface is used for plugging in different implementations for special processing of some
 * HTTP GET requests.
 * <p/>
 * e.g. ?wsdl, ?wsdl2 etc.
 * <p/>
 * If you need to handle a special HTTP GET request, you have to write an implementation of this
 * interface.
 */
public interface HttpGetRequestProcessor {
    /**
     * Initialize the HttpGetProcessor
     */
    void init(/*ConfigurationContext cfgCtx,*/ ServerHandler serverHandler) throws Exception;

    /**
     * Process the HTTP GET request.
     *
     * @param request       The HttpRequest
     * @param response      The HttpResponse
     * @param msgContext    The MessageContext
     * @param conn          The NHttpServerConnection
     * @param os            The OutputStream
     * @param isRestDispatching Rest dispatching
     */
    void process(HttpRequest request,
                 HttpResponse response,
                 MessageContext msgContext,
                 NHttpServerConnection conn,
                 OutputStream os,
                 boolean isRestDispatching);

}
