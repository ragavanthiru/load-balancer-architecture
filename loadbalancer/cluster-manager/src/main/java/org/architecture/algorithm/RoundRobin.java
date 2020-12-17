package org.architecture.algorithm;

import org.architecture.BackendInitiator;

import java.util.HashMap;
import java.util.Map;

public class RoundRobin {

    private Map<String, Integer> indexMap;
    private BackendInitiator backendInitiator;
    private static RoundRobin instance;
    private RoundRobin(){
        backendInitiator = BackendInitiator.getInstance();
        indexMap = new HashMap<String, Integer>();
    }
}
