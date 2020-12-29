package org.architecture.connection;

import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.reactor.IOSession;

public interface UpgradableNHttpConnection extends NHttpConnection {

    void bind(IOSession iosession);

    IOSession getIOSession();

}
