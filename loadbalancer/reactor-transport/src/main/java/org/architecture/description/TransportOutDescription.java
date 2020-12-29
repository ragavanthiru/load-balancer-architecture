package org.architecture.description;

import java.util.HashMap;
import java.util.Map;

public class TransportOutDescription {
    protected String name;
    private static Map<String, String> parameters = new HashMap<>();

    public TransportOutDescription(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParameter(String key){
        return parameters.get(key);
    }

    public String setParameter(String key, String value){
        return parameters.put(key, value);
    }
}
