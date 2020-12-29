package org.architecture.context;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.util.UIDGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.architecture.description.TransportInDescription;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import java.io.*;
import java.util.*;


public class MessageContext{

    /*
     * setup for logging
     */
    private static final Log log = LogFactory.getLog(MessageContext.class);

    /**
     * @serial An ID which can be used to correlate operations on a single
     * message in the log files, irrespective of thread switches, persistence,
     * etc.
     */
    private String logCorrelationID = null;

    /**
     * This string will be used to hold a form of the logCorrelationID that
     * is more suitable for output than its generic form.
     */
    private transient String logCorrelationIDString = null;

    private static final String myClassName = "MessageContext";

    /**
     * @serial The serialization version ID tracks the version of the class.
     * If a class definition changes, then the serialization/externalization
     * of the class is affected. If a change to the class is made which is
     * not compatible with the serialization/externalization of the class,
     * then the serialization version ID should be updated.
     * Refer to the "serialVer" utility to compute a serialization
     * version ID.
     */
    private static final long serialVersionUID = -7753637088257391858L;

    /**
     * @serial Tracks the revision level of a class to identify changes to the
     * class definition that are compatible to serialization/externalization.
     * If a class definition changes, then the serialization/externalization
     * of the class is affected.
     * Refer to the writeExternal() and readExternal() methods.
     */
    // supported revision levels, add a new level to manage compatible changes
    private static final int REVISION_2 = 2;
    // current revision level of this object
    private static final int revisionID = REVISION_2;

    private static final boolean DEBUG_ENABLED = log.isDebugEnabled() || log.isTraceEnabled();

    /**
     * A place to store the current MessageContext
     */
    public static ThreadLocal<MessageContext> currentMessageContext = new ThreadLocal<MessageContext>();

    public static MessageContext getCurrentMessageContext() {
        return (MessageContext) currentMessageContext.get();
    }

    public static void destroyCurrentMessageContext() {
        currentMessageContext.remove();
    }

    public static void setCurrentMessageContext(MessageContext ctx) {
        currentMessageContext.set(ctx);
    }


    public final static int IN_FLOW = 1;
    public final static int IN_FAULT_FLOW = 3;

    public final static int OUT_FLOW = 2;
    public final static int OUT_FAULT_FLOW = 4;

    public static final String REMOTE_ADDR = "REMOTE_ADDR";
    public static final String TRANSPORT_ADDR = "TRANSPORT_ADDR";
    public static final String TRANSPORT_HEADERS = "TRANSPORT_HEADERS";

    /**
     * Constant used as the key for the property which stores the In MessageContext in the
     * Out MessageContext/FaultMessageContext. This is needed in cases where an OperationContext
     * is not created, for example, since the request never gets dispatched to the service
     * operation, either due to a security failure or a request coming in for a non-existing
     * endpoint
     */
    public static final String IN_MESSAGE_CONTEXT = "axis2.inMsgContext";

    /**
     * message attachments
     * NOTE: Serialization of message attachments is handled as part of the
     * overall message serialization.  If this needs to change, then
     * investigate having the Attachment class implement the
     * java.io.Externalizable interface.
     */
    public transient Attachments attachments;

    /**
     * Field TRANSPORT_OUT
     */
    public static final String TRANSPORT_OUT = "TRANSPORT_OUT";

    /**
     * Field TRANSPORT_IN
     */
    public static final String TRANSPORT_IN = "TRANSPORT_IN";

    /**
     * Field CHARACTER_SET_ENCODING
     */
    public static final String CHARACTER_SET_ENCODING = "CHARACTER_SET_ENCODING";

    /**
     * Field UTF_8. This is the 'utf-8' value for CHARACTER_SET_ENCODING
     * property.
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * Field UTF_16. This is the 'utf-16' value for CHARACTER_SET_ENCODING
     * property.
     */
    public static final String UTF_16 = "utf-16";

    /**
     * Field TRANSPORT_SUCCEED
     */
    public static final String TRANSPORT_SUCCEED = "TRANSPORT_SUCCEED";

    /**
     * Field DEFAULT_CHAR_SET_ENCODING. This is the default value for
     * CHARACTER_SET_ENCODING property.
     */
    public static final String DEFAULT_CHAR_SET_ENCODING = UTF_8;

    /**
     * @serial The direction flow in use to figure out which path the message is in
     * (send or receive)
     */
    public int FLOW = IN_FLOW;

    /**
      * To invoke fireAndforget method we have to hand over transport sending logic to a thread
      * other wise user has to wait till it get transport response (in the case of HTTP its HTTP
      * 202)
      * 202). This was eariler named TRANSPORT_NON_BLOCKING, but that name is wrong as transport non blocking is NIO,
      * which has nothing to do with this property. See https://issues.apache.org/jira/browse/AXIS2-4196. 
      * Renaming this to CLIENT_API_NON_BLOCKING instead.  
      * 
     */
    public static final String CLIENT_API_NON_BLOCKING = "ClientApiNonBlocking";

    /**
     * This property allows someone (e.g. RM) to disable an async callback from
     * being invoked if a fault occurs during message transmission.  If this is
     * not set, it can be assumed that the fault will be delivered via
     * Callback.onError(...).
     */
    public static final String DISABLE_ASYNC_CALLBACK_ON_TRANSPORT_ERROR =
            "disableTransmissionErrorCallback";

    /**
     * @serial processingFault
     */
    private boolean processingFault;

    /**
     * @serial paused
     */
    private boolean paused;

    /**
     * @serial outputWritten
     */
    public boolean outputWritten;

    /**
     * @serial newThreadRequired
     */
    private boolean newThreadRequired;

    /**
     * @serial isSOAP11
     */
    private boolean isSOAP11 = true;

    /**
     * @serial Flag to indicate if we are doing REST
     */
    private boolean doingREST;

    /**
     * @serial Flag to indicate if we are doing MTOM
     */
    private boolean doingMTOM;

    /**
     * @serial Flag to indicate if we are doing SWA
     */
    private boolean doingSwA;

    /**
     * @serial Index into the executuion chain of the currently executing handler
     */
    private int currentHandlerIndex;

    /**
     * @serial Index into the current Phase of the currently executing handler (if any)
     */
    private int currentPhaseIndex;

    /**
     * If we're processing this MC due to flowComplete() being called in the case
     * of an Exception, this will hold the Exception which caused the problem.
     */
    private Exception failureReason;

    /**
     * @serial responseWritten
     */
    private boolean responseWritten;

    /**
     * @serial serverSide
     */
    private boolean serverSide;

    /**
     * @serial service context ID
     */
    private String serviceContextID;

    /**
     * @serial Holds a key to retrieve the correct ServiceGroupContext.
     */
    private String serviceGroupContextId;


    /**
     * transport in description
     */
    private transient TransportInDescription transportIn;


    /**
     * @serial incoming transport name
     */
    //The value will be set by the transport receiver and there will be validation for the transport
    //at the dispatch phase (its post condition)
    private String incomingTransportName;


    /*
     * SelfManagedData will hold message-specific data set by handlers
     * Note that this list is not explicitly saved by the MessageContext, but
     * rather through the SelfManagedDataManager interface implemented by handlers
     */
    private transient LinkedHashMap<String, Object> selfManagedDataMap = null;

    //-------------------------------------------------------------------------
    // MetaData for data to be restored in activate() after readExternal()
    //-------------------------------------------------------------------------

    /**
     * Indicates whether the message context has been reconstituted
     * and needs to have its object references reconciled
     */
    private transient boolean needsToBeReconciled = false;

    /**
     * selfManagedDataHandlerCount is a count of the number of handlers
     * that actually saved data during serialization
     */
    private transient int selfManagedDataHandlerCount = 0;


    private Map<String, Object> items = new HashMap<>();

    public void setProperty(String key, Object value){
        items.put(key, value);
    }

    public Object getProperty(String key){
        return items.get(key);
    }


    public void setServerSide(boolean b) {
        serverSide = b;
    }

    public String getIncomingTransportName() {
        return incomingTransportName;
    }

    public void setIncomingTransportName(String incomingTransportName) {
        this.incomingTransportName = incomingTransportName;
    }

    public void setTransportIn(TransportInDescription in) {
        this.transportIn = in;
    }

    public void setMessageID(String messageId) {
        //options.setMessageId(messageId);
    }
}
