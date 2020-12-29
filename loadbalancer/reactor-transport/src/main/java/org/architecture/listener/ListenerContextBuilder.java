package org.architecture.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.architecture.description.TransportInDescription;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Properties;

class ListenerContextBuilder {
    
    private final Log log = LogFactory.getLog(ListenerContextBuilder.class);

    private final static String HOST_ADDRESS="HOST_ADDRESS";
    private final static String PARAM_PORT="PARAM_PORT";

    private final TransportInDescription transportIn;
    private final String name;

    private String host = "localhost";
    private int port = 8280;
    //private PriorityExecutor executor = null;
    private boolean restDispatching = true;
    private HttpGetRequestProcessor httpGetRequestProcessor = null;
    private InetAddress bindAddress;

    public ListenerContextBuilder(final TransportInDescription transportIn) {
        this.transportIn = transportIn;
        this.name = transportIn.getName().toUpperCase(Locale.US);
    }

    public ListenerContextBuilder parse() throws Exception {
        String param = transportIn.getParameter(HOST_ADDRESS);
        if (param != null) {
            host = param.trim();
        } else {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                log.error("Unable to lookup local host name, using 'localhost'");
            }
        }

        param = transportIn.getParameter(PARAM_PORT);
        if (param != null) {
            port = Integer.parseInt(param);
        }


        param = transportIn.getParameter(NhttpConstants.BIND_ADDRESS);
        if (param != null) {
            String s = (param).trim();
            try {
                bindAddress = InetAddress.getByName(s);
            } catch (UnknownHostException ex) {
                log.error("Unable to lookup bind address, using 'localhost'");
            }
        }

        // create the priority based executor and parser
        param = transportIn.getParameter(NhttpConstants.PRIORITY_CONFIG_FILE_NAME);
        if (param != null && param != null) {
            /*String fileName = param.getValue().toString();
            OMElement definitions = null;
            try {
                FileInputStream fis = new FileInputStream(fileName);
                definitions = new StAXOMBuilder(fis).getDocumentElement();
                definitions.build();
            } catch (FileNotFoundException e) {
                handleException("Priority configuration file cannot be found : " + fileName, e);
            } catch (XMLStreamException e) {
                handleException("Error parsing priority configuration xml file " + fileName, e);
            }

            executor = createPriorityExecutor(definitions);
            parser = createParser(definitions);

            if (log.isInfoEnabled()) {
                log.info(name + " Created a priority based executor from the configuration: " +
                    fileName);
            }*/
        }

        param = transportIn.getParameter(NhttpConstants.DISABLE_REST_SERVICE_DISPATCHING);
        /*if (param != null && param.getValue() != null) {
            if (param.getValue().equals("true")) {
                restDispatching = false;
            }
        }*/

        // create http Get processor
        param = transportIn.getParameter(NhttpConstants.HTTP_GET_PROCESSOR);
        /*if (param != null && param.getValue() != null) {
            httpGetRequestProcessor = createHttpGetProcessor(param.getValue().toString());
            if (httpGetRequestProcessor == null) {
                handleException("Cannot create HttpGetRequestProcessor");
            }
        } else {*/
            httpGetRequestProcessor = new DefaultHttpGetProcessor();
        //}
        return this;
    }

    /*private PriorityExecutor createPriorityExecutor(final OMElement definitions) throws Exception {
        assert definitions != null;
        OMElement executorElem = definitions.getFirstChildWithName(
                new QName(ExecutorConstants.PRIORITY_EXECUTOR));

        if (executorElem == null) {
            handleException(ExecutorConstants.PRIORITY_EXECUTOR +
                    " configuration is mandatory for priority based routing");
        }

        PriorityExecutor executor = PriorityExecutorFactory.createExecutor(
                null, executorElem, false, new Properties());
        executor.init();
        return executor;
    }*/

    private HttpGetRequestProcessor createHttpGetProcessor(String str) throws Exception {
        Object obj = null;
        try {
            obj = Class.forName(str).newInstance();
        } catch (ClassNotFoundException e) {
            handleException("Error creating WSDL processor", e);
        } catch (InstantiationException e) {
            handleException("Error creating WSDL processor", e);
        } catch (IllegalAccessException e) {
            handleException("Error creating WSDL processor", e);
        }

        if (obj instanceof HttpGetRequestProcessor) {
            return (HttpGetRequestProcessor) obj;
        } else {
            handleException("Error creating WSDL processor. The HttpProcessor should be of type " +
                    "org.apache.synapse.transport.nhttp.HttpGetRequestProcessor");
        }

        return null;
    }

    private void handleException(String msg, Exception e) throws Exception {
        log.error(name + " " + msg, e);
        throw new Exception(msg, e);
    }

    private void handleException(String msg) throws Exception {
        log.error(name + " " + msg);
        throw new Exception(msg);
    }

    public ListenerContext build() {
        return new ListenerContext(
            transportIn, /*executor, parser,*/ restDispatching,
            httpGetRequestProcessor,  host, port, bindAddress);
    }

}