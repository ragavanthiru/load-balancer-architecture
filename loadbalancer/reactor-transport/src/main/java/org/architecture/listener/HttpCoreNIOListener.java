package org.architecture.listener;

import org.apache.http.HttpHost;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;
import org.architecture.connection.Scheme;
import org.architecture.connection.ServerConnFactory;
import org.architecture.connection.ServerConnFactoryBuilder;
import org.architecture.description.TransportInDescription;
import org.architecture.threads.NativeThreadFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.util.*;

public class HttpCoreNIOListener {
    final static Logger log = Logger.getLogger(HttpCoreNIOListener.class);

    /** The IOReactor */
    private volatile DefaultListeningIOReactor ioReactor;
    /** The I/O dispatch */
    private volatile ServerIODispatch iodispatch;
    /** Protocol scheme used by this listener **/
    private volatile Scheme scheme;
    /** Connection factory used by this listener **/
    private volatile ServerConnFactory connFactory;
    /** The component name (to be used in logs)*/
    private volatile String name;
    /** HTTP parameters */
    private volatile HttpParams params;
    /** The ServerHandler */
    private volatile ServerHandler handler;
    /** Listener configurations */
    private volatile ListenerContext listenerContext;
    /** state of the listener */
    private volatile int state = BaseConstants.STOPPED;
    /** Delay for ActiveConnectionMonitor */
    public static final long ACTIVE_CONNECTION_MONITOR_DELAY = 1000;

    public void init(/*ConfigurationContext ctx,*/ TransportInDescription transportIn) throws Exception{
        NHttpConfiguration cfg = NHttpConfiguration.getInstance();

        name = transportIn.getName();

        // Initialize connection factory
        params = new BasicHttpParams();
        params
                .setIntParameter(CoreConnectionPNames.SO_TIMEOUT,
                        cfg.getProperty(NhttpConstants.SO_TIMEOUT_RECEIVER, 60000))
                .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE,
                        cfg.getProperty(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024))
                .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "Synapse-HttpComponents-NIO");
//                .setParameter(CoreProtocolPNames.HTTP_ELEMENT_CHARSET,
//                        cfg.getStringValue(CoreProtocolPNames.HTTP_ELEMENT_CHARSET, HTTP.DEFAULT_PROTOCOL_CHARSET));

        scheme = initScheme();

        // Setup listener context
        listenerContext = new ListenerContextBuilder(transportIn).parse().build();
        System.out.println( "Hostname = " +listenerContext.getHostname());
        System.out.println( "Port = " +listenerContext.getPort());
        System.out.println( "Bind Address = " +listenerContext.getBindAddress());

        System.setProperty(transportIn.getName() + ".nio.port", String.valueOf(listenerContext.getPort()));

        // Setup connection factory
        HttpHost host = new HttpHost(
                listenerContext.getHostname(),
                listenerContext.getPort(),
                scheme.getName());
        connFactory = initConnFactoryBuilder(transportIn, host/*, ctx*/).build(params);

        // configure the IO reactor on the specified port
        try {
            String prefix = name + " I/O dispatcher";
            IOReactorConfig ioReactorConfig = new IOReactorConfig();
            ioReactorConfig.setIoThreadCount(
                    cfg.getServerIOWorkers());
            ioReactorConfig.setSoTimeout(
                    cfg.getProperty(NhttpConstants.SO_TIMEOUT_RECEIVER, 60000));
            ioReactorConfig.setTcpNoDelay(
                    cfg.getProperty(CoreConnectionPNames.TCP_NODELAY, 1) == 1);
            if (cfg.getBooleanValue("http.nio.interest-ops-queueing", false)) {
                ioReactorConfig.setInterestOpQueued(true);
            }
            ioReactorConfig.setSoReuseAddress(cfg.getBooleanValue(CoreConnectionPNames.SO_REUSEADDR, false));

            ioReactor = new DefaultListeningIOReactor(
                    ioReactorConfig,
                    new NativeThreadFactory(new ThreadGroup(prefix + " thread group"), prefix));

            ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {
                public boolean handle(IOException ioException) {
                    ioException.printStackTrace();
                    System.out.println("System may be unstable: IOReactor encountered a checked exception : "
                            + ioException.getMessage()+ ioException);
                    return true;
                }

                public boolean handle(RuntimeException runtimeException) {
                    runtimeException.printStackTrace();
                    System.out.println(" System may be unstable: IOReactor encountered a runtime exception : "
                            + runtimeException.getMessage()+ runtimeException);
                    return true;
                }
            });
        } catch (IOException e) {
            handleException("Error creating IOReactor", e);
        }

        //metrics = new NhttpMetricsCollector(true, transportIn.getName());

        handler = new ServerHandler(/*cfgCtx,*/ scheme, listenerContext/*, metrics*/);
        iodispatch = new ServerIODispatch(handler, connFactory);
    }

    public void start()  {
        //if (log.isDebugEnabled()) {
        System.out.println("Starting Listener...");
        //}

        state = BaseConstants.STARTED;

        // start the IO reactor in a new separate thread
        final IOEventDispatch ioEventDispatch = iodispatch;
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    ioReactor.execute(ioEventDispatch);
                } catch (InterruptedIOException ex) {
                    System.out.println("Reactor Interrupted" + ex);
                } catch (IOException e) {
                    System.out.println("Encountered an I/O error: " + e.getMessage() + e);
                } catch (Exception e) {
                    System.out.println("Unexpected exception in I/O reactor" + e);
                }
                log.info(name + " Shutdown");
            }
        }, "HttpCoreNIOListener");

        t.start();

        try {
            listenerContext.getHttpGetRequestProcessor().init(/*cfgCtx,*/ handler);
        }catch (Exception e){
            e.printStackTrace();
        }
        startEndpoints();
    }

    private void startEndpoints() {
        Queue<ListenerEndpoint> endpoints = new LinkedList<ListenerEndpoint>();

        Set<InetSocketAddress> addressSet = new HashSet<InetSocketAddress>();
        addressSet.addAll(connFactory.getBindAddresses());
        /*if (NHttpConfiguration.getInstance().getMaxActiveConnections() != -1) {
            addMaxConnectionCountController(NHttpConfiguration.getInstance().getMaxActiveConnections());
        }*/
        if (listenerContext.getBindAddress() != null) {
            addressSet.add(new InetSocketAddress(listenerContext.getBindAddress(), listenerContext.getPort()));
        }
        if (addressSet.isEmpty()) {
            addressSet.add(new InetSocketAddress(listenerContext.getPort()));
        }

        // Ensure simple but stable order
        List<InetSocketAddress> addressList = new ArrayList<InetSocketAddress>(addressSet);
        Collections.sort(addressList, new Comparator<InetSocketAddress>() {

            public int compare(InetSocketAddress a1, InetSocketAddress a2) {
                String s1 =  a1.toString();
                String s2 = a2.toString();
                return s1.compareTo(s2);
            }

        });
        for (InetSocketAddress address: addressList) {
            endpoints.add(ioReactor.listen(new InetSocketAddress(8080)));
        }

        // Wait for the endpoint to become ready, i.e. for the listener to start accepting requests.
        while (!endpoints.isEmpty()) {
            ListenerEndpoint endpoint = endpoints.remove();
            try {
                endpoint.waitFor();

                //if ( log.isInfoEnabled()) {
                    InetSocketAddress address = (InetSocketAddress) endpoint.getAddress();
                    if (!address.isUnresolved()) {
                        System.out.println(name + " started on " + address.getHostName() + ":" + address.getPort());
                    } else {
                        System.out.println( name + "  started on " + address);
                    }
                //}
            } catch (InterruptedException e) {
                System.out.println("Listener startup was interrupted");
                break;
            }
        }
    }

    protected ServerConnFactoryBuilder initConnFactoryBuilder(final TransportInDescription transportIn,
                                                              final HttpHost host) {
        return new ServerConnFactoryBuilder(transportIn, host);
    }

    public void maintenenceShutdown(long millis)  {
        if (state != BaseConstants.STARTED) return;
        try {
            long start = System.currentTimeMillis();
            ioReactor.pause();
            ioReactor.shutdown(millis);
            state = BaseConstants.STOPPED;
            log.info("Listener shutdown in : " + (System.currentTimeMillis() - start) / 1000 + "s");
        } catch (IOException e) {
            try {
                handleException("Error shutting down the IOReactor for maintenence", e);
            }catch (Exception er){
                er.printStackTrace();
            }
        }
    }

    protected Scheme initScheme() {
        return new Scheme("http", 80, false);
    }

    public int getActiveConnectionsSize() {
        return handler.getActiveConnectionsSize();
    }

    private void handleException(String msg, Exception e) throws Exception {
        log.error(msg, e);
        throw e;
    }
}
