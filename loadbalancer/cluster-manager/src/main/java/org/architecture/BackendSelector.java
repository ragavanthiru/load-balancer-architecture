package org.architecture;

import java.util.List;
import java.util.Map;

public class BackendSelector {

    BackendInitiator backendInitiator = null;
    Map<String, List<String>> clusters = null;
    Map<String, Integer> index = null;
    public BackendSelector(){
        backendInitiator = BackendInitiator.getInstance();
        clusters = backendInitiator.getClusters();
    }



    public String getServer(String cluster){
        return null;
    }
}
