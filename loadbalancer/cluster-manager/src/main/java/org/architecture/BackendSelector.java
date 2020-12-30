package org.architecture;

import org.architecture.algorithm.Endpoint;
import org.architecture.algorithm.LoadBalanceStrategy;

public class BackendSelector {

    LoadBalanceStrategy loadBalanceStrategy;
    public BackendSelector(LoadBalanceStrategy loadBalanceStrategy){
        this.loadBalanceStrategy = loadBalanceStrategy;
    }

    public Endpoint getServer(String cluster){
        return loadBalanceStrategy.getServer(cluster);
    }
}
