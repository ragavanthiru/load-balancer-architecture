package org.architecture;

import org.architecture.algorithm.Endpoint;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class BackendCluster {
    private Map<String, Vector<Endpoint>> clusterMap;
    private static BackendCluster instance;

    private BackendCluster(){
        init();
    }
    private void init(){
        clusterMap = new HashMap<String, Vector<Endpoint>>();

        String clusterNames = PropertySource.getProperty(BackendConstant.CLUSTERS);
        if(clusterNames != null ){
            String[] clusterNamesArr = clusterNames.split(",");
            Arrays.stream(clusterNamesArr).forEach(cluster ->{

                String servers = PropertySource.getProperty("backend."+cluster.trim()+".servers");
                if(servers != null){
                    String[] serversArr = servers.split(",");
                    Vector<Endpoint> endpoints = new Vector<>();
                    for(String host : serversArr){
                        if (host != null) {
                            String[] hostDetails = host.split(":");
                            if(hostDetails.length ==2) {
                                try {
                                   /* System.out.println(hostDetails[0]);
                                    System.out.println(hostDetails[1]);*/
                                    endpoints.add(new Endpoint(InetAddress.getByName(hostDetails[0]),
                                            Integer.parseInt(hostDetails[1])));
                                } catch (UnknownHostException unkh){
                                    unkh.printStackTrace();
                                }
                            }
                        }
                    }
                    clusterMap.putIfAbsent(cluster, endpoints);
                }
            });
        }
    }

    public Map<String, Vector<Endpoint>> getClusters(){
        return clusterMap;
    }

    public Vector<Endpoint> getServers( String cluster){
        return clusterMap.get(cluster);
    }

    public static BackendCluster getInstance(){
        if(instance == null){
            instance = new BackendCluster();
        }
        return instance;
    }
}
