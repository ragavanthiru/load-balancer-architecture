package org.architecture.algorithm;

public interface LoadBalanceStrategy {

    public Endpoint getServer(String cluster);
}
