package org.architecture.algorithm;

import org.architecture.BackendCluster;

import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class RoundRobinStrategy implements LoadBalanceStrategy{

    private Map<String, Integer> indexMap;
    private BackendCluster backendCluster;
    private static RoundRobinStrategy instance;

    private RoundRobinStrategy(){
        backendCluster = BackendCluster.getInstance();
        indexMap = new ConcurrentHashMap<>();
    }

    public static RoundRobinStrategy getInstance(){
        if(instance == null){
            instance = new RoundRobinStrategy();
        }

        return instance;
    }

    public Endpoint getServer(String cluster){
        Vector<Endpoint> endpoints = backendCluster.getServers(cluster);
        int index = -1;
        if(indexMap.containsKey(cluster)){
            index = indexMap.computeIfPresent(cluster, (key, value) -> value + 1);
            index = index%endpoints.size();
        } else {
            indexMap.put(cluster, 0);
            index = 0;
        }

        return endpoints.elementAt(index);
    }
}
