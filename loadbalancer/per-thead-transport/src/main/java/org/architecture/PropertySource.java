package org.architecture;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertySource {
    private static Properties prop = null;

    static{
        try (InputStream input = new FileInputStream("E:\\msc\\Project\\code\\load-balancer-architecture" +
                "\\loadbalancer\\config\\http-blocking.properties")) {
            prop = new Properties();

            prop.load(input);
            System.out.println(prop.getProperty("backend.web.servers"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getProperty(String key){
        return prop.getProperty(key);
    }

    public static void setProperty(String key, String value){
        prop.put(key, value);
    }
}
