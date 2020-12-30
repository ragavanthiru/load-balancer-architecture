package org.architecture.bio.thread;

import org.apache.http.*;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.protocol.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

public class ProxyHandler implements HttpRequestHandler {
    final static Logger logger = Logger.getLogger(ProxyHandler.class);
    private static final String HTTP_IN_CONN = "http.proxy.in-conn";
    private static final String HTTP_OUT_CONN = "http.proxy.out-conn";
    private static final String HTTP_CONN_KEEPALIVE = "http.proxy.conn-keepalive";

    private final HttpHost target;
    private final HttpProcessor httpproc;
    private final HttpRequestExecutor httpexecutor;
    private final ConnectionReuseStrategy connStrategy;

    public ProxyHandler(
            final HttpHost target,
            final HttpProcessor httpproc,
            final HttpRequestExecutor httpexecutor) {
        super();
        this.target = target;
        this.httpproc = httpproc;
        this.httpexecutor = httpexecutor;
        this.connStrategy = DefaultConnectionReuseStrategy.INSTANCE;
    }

    public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context) throws HttpException, IOException {

        final DefaultBHttpClientConnection conn = (DefaultBHttpClientConnection) context.getAttribute(
                HTTP_OUT_CONN);

        if (!conn.isOpen() || conn.isStale()) {
            final Socket outsocket = new Socket(this.target.getHostName(), this.target.getPort() >= 0 ? this.target.getPort() : 80);
            conn.bind(outsocket);
            logger.debug("Outgoing connection to " + outsocket.getInetAddress());
        }

        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, conn);
        context.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, this.target);

        logger.debug(">> Request URI: " + request.getRequestLine().getUri());

        // Remove hop-by-hop headers
        request.removeHeaders(HTTP.TARGET_HOST);
        request.removeHeaders(HTTP.CONTENT_LEN);
        request.removeHeaders(HTTP.TRANSFER_ENCODING);
        request.removeHeaders(HTTP.CONN_DIRECTIVE);
        request.removeHeaders("Keep-Alive");
        request.removeHeaders("Proxy-Authenticate");
        request.removeHeaders("TE");
        request.removeHeaders("Trailers");
        request.removeHeaders("Upgrade");

        this.httpexecutor.preProcess(request, this.httpproc, context);
        final HttpResponse targetResponse = this.httpexecutor.execute(request, conn, context);
        this.httpexecutor.postProcess(response, this.httpproc, context);

        // Remove hop-by-hop headers
        targetResponse.removeHeaders(HTTP.CONTENT_LEN);
        targetResponse.removeHeaders(HTTP.TRANSFER_ENCODING);
        targetResponse.removeHeaders(HTTP.CONN_DIRECTIVE);
        targetResponse.removeHeaders("Keep-Alive");
        targetResponse.removeHeaders("TE");
        targetResponse.removeHeaders("Trailers");
        targetResponse.removeHeaders("Upgrade");

        response.setStatusLine(targetResponse.getStatusLine());
        response.setHeaders(targetResponse.getAllHeaders());
        response.setEntity(targetResponse.getEntity());
        HttpEntity entity = targetResponse.getEntity();
        //InputStream is = entity.getContent();
        //String result = EntityUtils.toString(entity);
        logger.debug("<< Response: " + response.getStatusLine());
        //logger.debug("<< Response Body: " + result);
        final boolean keepalive = this.connStrategy.keepAlive(response, context);
        context.setAttribute(HTTP_CONN_KEEPALIVE, new Boolean(keepalive));
    }

}