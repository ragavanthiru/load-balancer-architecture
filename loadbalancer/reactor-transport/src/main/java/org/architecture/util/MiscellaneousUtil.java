package org.architecture.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.Properties;

/**
 *
 */
public class MiscellaneousUtil {


    private static Log log = LogFactory.getLog(MiscellaneousUtil.class);

    private static final String CONF_LOCATION = "conf.location";

    private MiscellaneousUtil() {
    }

    /**
     * Helper method to get the value of the property from a given property bag
     *
     * @param properties
     *            The property collection
     * @param name
     *            The name of the property
     * @param defaultValue
     *            The default value for the property
     * @return The value of the property if it is found , otherwise , default
     *         value
     */
    public static String getProperty(Properties properties, String name, String defaultValue) {

        String result = properties.getProperty(name);
        if ((result == null || result.length() == 0) && defaultValue != null) {
            if (log.isDebugEnabled()) {
                log.debug("The name with '" + name + "' cannot be found. " + "Using default value : " + defaultValue);
            }
            result = defaultValue;
        }
        if (result != null) {
            return result.trim();
        } else {
            return defaultValue;
        }
    }

    /**
     * Helper method to get the value of the property from a given property bag
     * This method will return a value with the type equal to the type
     * given by the Class type parameter. Therefore, The user of this method
     * can ensure that he is get what he request
     *
     * @param properties
     *            Properties bag
     * @param name
     *            Name of the property
     * @param defaultValue
     *            Default value
     * @param type
     *            Expected Type using Class
     * @return Value corresponding to the given property name
     */
    @SuppressWarnings({ "TypeParameterExplicitlyExtendsObject", "unchecked" })
    public static <T extends Object> T getProperty(Properties properties, String name, T defaultValue, Class<? extends T> type) throws Exception{

        String result = properties.getProperty(name);
        if (result == null && defaultValue != null) {
            if (log.isDebugEnabled()) {
                log.debug("The name with '" + name + "' cannot be found. " + "Using default value : " + defaultValue);
            }
            return defaultValue;
        }

        if (result == null || type == null) {
            return null;
        }

        if (String.class.equals(type)) {
            return (T) result;
        } else if (Boolean.class.equals(type)) {
            return (T) Boolean.valueOf(Boolean.parseBoolean(result));
        } else if (Integer.class.equals(type)) {
            return (T) Integer.valueOf(Integer.parseInt(result));
        } else if (Long.class.equals(type)) {
            return (T) Long.valueOf(Long.parseLong(result));
        } else {
            handleException("Unsupported type : " + type);
        }

        return null;
    }

    /**
     * Loads the properties from a given property file path
     *
     * @param filePath
     *            Path of the property file
     * @return Properties loaded from given file
     */
    public static Properties loadProperties(String filePath) throws Exception{

        Properties properties = new Properties();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        if (log.isDebugEnabled()) {
            log.debug("Loading a file '" + filePath + "' from classpath");
        }

        InputStream in  = null;

        //if we reach to this assume that the we may have to looking to the customer provided external location for the
        //given properties
        if (System.getProperty(CONF_LOCATION) != null) {
            try {
                in = new FileInputStream(System.getProperty(CONF_LOCATION) + File.separator + filePath);
            } catch (FileNotFoundException e) {
                String msg = "Error loading properties from a file at from the System defined location: " + filePath;
                log.warn(msg);
            }
        }

        if(in ==null){
            //if can not find with system path definition looking to the class path for the given property file
            in = cl.getResourceAsStream(filePath);
        }

        if (in == null) {

            if (log.isDebugEnabled()) {
                log.debug("Unable to load file  '" + filePath + "'");
            }

            filePath = "conf" +
                    File.separatorChar + filePath;
            if (log.isDebugEnabled()) {
                log.debug("Loading a file '" + filePath + "' from classpath");
            }

            in = cl.getResourceAsStream(filePath);
            if (in == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to load file  '" + filePath + "'");
                }
            }



        }
        if (in != null) {
            try {
                properties.load(in);
            } catch (IOException e) {
                handleException("Error loading properties from a file at : " + filePath, e);
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    log.warn("Error while closing the input stream from the file: " + filePath, e);
                }
            }
        }
        return properties;
    }




    /**
     * Helper method to serialize object into a byte array
     *
     * @param data
     *            The object to be serialized
     * @return The byte array representation of the provided object
     */
    public static byte[] serialize(Object data) throws Exception{

        ObjectOutputStream outputStream = null;
        ByteArrayOutputStream binOut = null;
        byte[] result = null;
        try {
            binOut = new ByteArrayOutputStream();
            outputStream = new ObjectOutputStream(binOut);
            outputStream.writeObject(data);
            result = binOut.toByteArray();
        } catch (IOException e) {
            handleException("Error serializing object : " + data);
        } finally {
            if (binOut != null) {
                try {
                    binOut.close();
                } catch (IOException ignored) {
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
        return result;
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    public static byte[] asBytes(InputStream in) throws Exception{

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = in.read(buffer)) >= 0)
                out.write(buffer, 0, len);
        } catch (IOException e) {
            throw new Exception("Error during converting a input stream " + "into a byte array");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }
        return out.toByteArray();
    }

    /**
     * Helper methods for handle errors.
     *
     * @param msg
     *            The error message
     */
    private static void handleException(String msg) throws Exception{
        log.error(msg);
        throw new Exception(msg);
    }

    /**
     * Helper method for handle errors.
     *
     * @param msg The error message
     * @param ex  Error exception
     */
    private static void handleException(String msg, Exception ex) throws Exception{
        log.error(msg);
        throw new Exception(msg, ex);
    }
}