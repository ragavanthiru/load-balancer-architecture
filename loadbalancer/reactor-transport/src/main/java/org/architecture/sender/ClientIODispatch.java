package org.architecture.sender;

import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.impl.nio.reactor.AbstractIODispatch;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.reactor.IOSession;
import org.architecture.connection.ClientConnFactory;

import java.io.IOException;

class ClientIODispatch extends AbstractIODispatch<DefaultNHttpClientConnection> {

    private final NHttpClientEventHandler handler;
    private ClientConnFactory connFactory;

    public ClientIODispatch(
            final NHttpClientEventHandler handler,
            final ClientConnFactory connFactory) {
        super();
        this.handler = LoggingUtils.decorate(handler);
        this.connFactory = connFactory;
    }

    @Override
    protected DefaultNHttpClientConnection createConnection(final IOSession session) {
        Axis2HttpRequest axis2Req = (Axis2HttpRequest) session.getAttribute(IOSession.ATTACHMENT_KEY);
        HttpRoute route = axis2Req.getRoute();
        return this.connFactory.createConnection(session, route);
    }

    @Override
    protected void onConnected(final DefaultNHttpClientConnection conn) {
        Axis2HttpRequest axis2Req = (Axis2HttpRequest) conn.getContext().getAttribute(IOSession.ATTACHMENT_KEY);
        try {
            this.handler.connected(conn, axis2Req);
        } catch (final Exception ex) {
            this.handler.exception(conn, ex);
        }
    }

    @Override
    protected void onClosed(final DefaultNHttpClientConnection conn) {
        this.handler.closed(conn);
    }

    @Override
    protected void onException(final DefaultNHttpClientConnection conn, final IOException ex) {
        this.handler.exception(conn, ex);
    }

    @Override
    protected void onInputReady(final DefaultNHttpClientConnection conn) {
        conn.consumeInput(this.handler);
    }

    @Override
    protected void onOutputReady(final DefaultNHttpClientConnection conn) {
        conn.produceOutput(this.handler);
    }

    @Override
    protected void onTimeout(final DefaultNHttpClientConnection conn) {
        try {
            this.handler.timeout(conn);
        } catch (final Exception ex) {
            this.handler.exception(conn, ex);
        }
    }

    public void setConnFactory(ClientConnFactory connFactory) {
        this.connFactory = connFactory;
    }
}
