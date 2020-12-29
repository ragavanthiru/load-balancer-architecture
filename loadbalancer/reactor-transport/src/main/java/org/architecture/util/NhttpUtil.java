package org.architecture.util;


import java.net.InetAddress;

/**
 * A useful set of utility methods for the HTTP transport
 */
public class NhttpUtil {

    /**
     * This method tries to determine the hostname of the given InetAddress without
     * triggering a reverse DNS lookup.  {@link InetAddress#getHostName()}
     * triggers a reverse DNS lookup which can be very costly in cases where reverse
     * DNS fails. Tries to parse a symbolic hostname from {@link InetAddress#toString()},
     * which is documented to return a String of the form "hostname / literal IP address"
     * with 'hostname' blank if not already computed & stored in <code>address</code<.
     * <p/>
     * If the hostname cannot be determined from InetAddress.toString(),
     * the value of {@link InetAddress#getHostAddress()} is returned.
     *
     * @param address The InetAddress whose hostname has to be determined
     * @return hostsname, if it can be determined. hostaddress, if not.
     *
     * TODO: We may introduce a System property or some other method of configuration
     * TODO: which will specify whether to allow reverse DNS lookup or not
     */
    public static String getHostName(InetAddress address) {
        String result;
        String hostAddress = address.getHostAddress();
        String inetAddr = address.toString();
        int index1 = inetAddr.lastIndexOf('/');
        int index2 = inetAddr.indexOf(hostAddress);
        if (index2 == index1 + 1) {
            if (index1 == 0) {
                result = hostAddress;
            } else {
                result = inetAddr.substring(0, index1);
            }
        } else {
            result = hostAddress;
        }
        return result;
    }

    /**
     * Calculate the REST_URL_POSTFIX from the request URI
     * @param uri  - The Request URI - String
     * @param servicePath  String
     * @return  REST_URL_POSTFIX String
     */
    public static String getRestUrlPostfix(String uri, String servicePath){

        String contextServicePath = "/" + servicePath;
        if (uri.startsWith(contextServicePath)) {
            // discard upto servicePath
            uri = uri.substring(uri.indexOf(contextServicePath) +
            		contextServicePath.length());
            // discard [proxy] service name if any
            int pos = uri.indexOf("/", 1);
            if (pos > 0) {
                uri = uri.substring(pos);
            } else {
                pos = uri.indexOf("?");
                if (pos != -1) {
                    uri = uri.substring(pos);
                } else {
                    uri = "";
                }
            }
        } else {
            // remove any absolute prefix if any
            int pos = uri.indexOf("://");
            //compute index of beginning of Query Parameter
            int indexOfQueryStart = uri.indexOf("?");
            
            //Check if there exist a absolute prefix '://' and it is before query parameters
            //To allow query parameters with URLs. ex: /test?a=http://asddd
            if (pos != -1 && ((indexOfQueryStart == -1 || pos < indexOfQueryStart))) {
                uri = uri.substring(pos + 3);
            }
            pos = uri.indexOf("/");
            if (pos != -1) {
                uri = uri.substring(pos + 1);
            }
			// Remove the service prefix
			if (uri.startsWith(servicePath)) {
				// discard upto servicePath
				uri = uri.substring(uri.indexOf(contextServicePath)
						+ contextServicePath.length());
				// discard [proxy] service name if any
				pos = uri.indexOf("/", 1);
				if (pos > 0) {
					uri = uri.substring(pos);
				} else {
					pos = uri.indexOf("?");
					if (pos != -1) {
						uri = uri.substring(pos);
					} else {
						uri = "";
					}
				}
			}           
        }

        return uri;
    }
}
