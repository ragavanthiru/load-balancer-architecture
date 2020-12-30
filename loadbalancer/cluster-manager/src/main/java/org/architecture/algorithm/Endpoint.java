package org.architecture.algorithm;

import java.net.InetAddress;

public class Endpoint {

    private InetAddress address;
    private int port;

    public Endpoint(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "address=" + address +
                ", port=" + port +
                '}';
    }
}
