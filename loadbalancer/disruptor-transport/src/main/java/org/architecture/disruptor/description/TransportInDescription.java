package org.architecture.disruptor.description;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class TransportInDescription {

    protected String name;
    private InetAddress addr;
    private int port;
    private static Map<String, String> parameters = new HashMap<>();

    public TransportInDescription(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InetAddress getAddr() {
        return addr;
    }

    public void setAddr(InetAddress addr) {
        this.addr = addr;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getParameter(String key){
        return parameters.get(key);
    }

    public String setParameter(String key, String value){
        return parameters.put(key, value);
    }
}
