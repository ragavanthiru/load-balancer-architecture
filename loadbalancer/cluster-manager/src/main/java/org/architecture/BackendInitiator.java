package org.architecture;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackendInitiator {
    private Map<String, List<String>> clusterMap;
    private static BackendInitiator instance;

    private BackendInitiator(){
        init();
    }
    private void init(){
        clusterMap = new HashMap<String, List<String>>();

        String clusterNames = PropertySource.getProperty("clusters");
        if(clusterNames != null ){
            String[] clusterNamesArr = clusterNames.split(",");
            Arrays.stream(clusterNamesArr).forEach(cluster ->{

                String servers = PropertySource.getProperty("backend."+cluster.trim()+".servers");
                if(servers != null){
                    String[] serversArr = servers.split(",");
                    clusterMap.putIfAbsent(cluster, Arrays.asList(serversArr));
                }
            });
        }
    }

    public Map<String, List<String>> getClusters(){
        return clusterMap;
    }

    public List<String> getServers( String cluster){
        return clusterMap.get(cluster);
    }

    public static BackendInitiator getInstance(){
        if(instance != null){
            instance = new BackendInitiator();
        }
        return instance;
    }
}
