package org.architecture.debug;

import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.nio.NHttpClientConnection;


/**
 * A connection debug object would be accumulated during processing, but only made use of if the connection
 * encounters issues during processing.
 */
public class ClientConnectionDebug extends AbstractConnectionDebug {

    private long connectionCreationTime;
    private long lastRequestStartTime;
    private String lastRequestEPR;
    private String lastRequestProtocol;
    private String lastRequestHTTPMethod;
    private StringBuffer previousRequestAttempts;

    private long requestCompletionTime;
    private long responseStartTime;
    private long responseCompletionTime = -1;
    private String responseLine;

    private ServerConnectionDebug serverConnectionDebug;

    public ClientConnectionDebug(ServerConnectionDebug serverConnectionDebug) {
        super();
        this.serverConnectionDebug = serverConnectionDebug;
    }

    public void recordRequestStartTime(NHttpClientConnection conn/*, Axis2HttpRequest axis2Req*/) {

        if (conn != null) {
            /*this.connectionCreationTime = (Long) conn.getContext().getAttribute(
                    ClientHandler.CONNECTION_CREATION_TIME);*/
            try {
                HttpRequest request = null;//axis2Req.getRequest();
                RequestLine requestLine = request.getRequestLine();
                this.lastRequestProtocol = requestLine.getProtocolVersion().toString();
                this.lastRequestHTTPMethod = requestLine.getMethod();
                this.headers = request.getAllHeaders();
            } catch (Exception ignore) {
            }
        }

        if (this.lastRequestStartTime != 0) {
            if (previousRequestAttempts == null) {
                previousRequestAttempts = new StringBuffer();
            } else {
                previousRequestAttempts.append(fieldSeparator);
            }
            previousRequestAttempts.append("Attempt-Info").append(keyValueSeparator).append("{");
            previousRequestAttempts.append("Req-Start-Time").append(keyValueSeparator)
                    .append(format(this.lastRequestStartTime));
            previousRequestAttempts.append(fieldSeparator);
            previousRequestAttempts.append("Req-URL").append(keyValueSeparator)
                    .append(this.lastRequestEPR).append("}");
        }
        this.lastRequestStartTime = System.currentTimeMillis();
        //this.lastRequestEPR = axis2Req.getEpr().toString();
    }

    public void recordResponseCompletionTime() {
        this.responseCompletionTime = System.currentTimeMillis();
    }

    public void recordRequestCompletionTime() {
        this.requestCompletionTime = System.currentTimeMillis();
    }

    public void recordResponseStartTime(String responseLine) {
        this.responseStartTime = System.currentTimeMillis();
        this.responseLine = responseLine;
    }

    public long getLastRequestStartTime() {
        return lastRequestStartTime;
    }

    public long getResponseCompletionTime() {
        return responseCompletionTime;
    }

    public long getResponseStartTime() {
        return responseStartTime;
    }

    public String dump() {
        StringBuffer sb = new StringBuffer(25);

        sb.append("E2S-Req-Start").append(keyValueSeparator).append(format(lastRequestStartTime));
        sb.append(fieldSeparator);
        sb.append("E2S-Req-End").append(keyValueSeparator).append(format(requestCompletionTime));
        sb.append(fieldSeparator);
        sb.append("E2S-Req-ConnCreateTime").append(keyValueSeparator)
                .append(format(connectionCreationTime));
        sb.append(statementSeparator);

        sb.append("E2S-Req-URL").append(keyValueSeparator).append(lastRequestEPR);
        sb.append(fieldSeparator);
        sb.append("E2S-Req-Protocol").append(keyValueSeparator).append(lastRequestProtocol);
        sb.append(fieldSeparator);
        sb.append("E2S-Req-Method").append(keyValueSeparator).append(lastRequestHTTPMethod);
        sb.append(statementSeparator);

        if (previousRequestAttempts != null) {
            sb.append("E2S-Previous-Attempts").append(keyValueSeparator)
                    .append(previousRequestAttempts);
            sb.append(statementSeparator);
        }

        sb.append("S2E-Resp-Start").append(keyValueSeparator).append(format(responseStartTime));
        sb.append(fieldSeparator);
        sb.append("S2E-Resp-End").append(keyValueSeparator).append(responseCompletionTime != -1 ?
                format(responseCompletionTime) : "NOT-COMPLETED");
        sb.append(statementSeparator);

        sb.append("S2E-Resp-Status").append(keyValueSeparator).append(responseLine);
        if (!printNoHeaders) {
            sb.append(fieldSeparator);
            sb.append("S2E-Resp-Info").append(keyValueSeparator).append("{")
                    .append(headersToString()).append("}");
        }
        sb.append(statementSeparator);

        return sb.toString();
    }

    public ServerConnectionDebug getServerConnectionDebug() {
        return serverConnectionDebug;
    }
}