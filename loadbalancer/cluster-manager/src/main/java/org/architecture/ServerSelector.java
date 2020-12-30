package org.architecture;

import org.architecture.algorithm.RoundRobinStrategy;

public class ServerSelector
{
    public static void main( String[] args )
    {
        RoundRobinStrategy roundRobinStrategy = RoundRobinStrategy.getInstance();
        BackendSelector backendSelector = new BackendSelector(roundRobinStrategy);

        for(int i=0; i< 25; i++){
            System.out.println(i+" = "+backendSelector.getServer("web"));
        }
    }
}
