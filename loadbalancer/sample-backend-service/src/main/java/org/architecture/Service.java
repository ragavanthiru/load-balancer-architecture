package org.architecture;

import com.sun.net.httpserver.HttpServer;
import org.apache.log4j.Logger;
import org.architecture.handlers.HealthHandler;
import org.architecture.handlers.QuickResponseHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.architecture.config.Configuration.getErrorHandler;
import static org.architecture.config.Configuration.getObjectMapper;

public class Service
{
    private static final Logger logger = Logger.getLogger(Service.class);

    public static void main( String[] args )
    {
        if (args.length < 1) {
            logger.error("Usage format is java -jar sample-backend-service.jar port");
            System.exit(0);
        }
        System.out.println( "Starting sample service..." );
        try {
            startAPIServer(Integer.parseInt(args[0]));
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void startAPIServer(int serverPort) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);

        HealthHandler healthHandler = new HealthHandler(getObjectMapper(), getErrorHandler());
        QuickResponseHandler quickResponseHandler = new QuickResponseHandler(getObjectMapper(), getErrorHandler());

        server.createContext("/api/health", healthHandler::handle);
        server.createContext("/api/sample", quickResponseHandler::handle);

        server.setExecutor(null);
        server.start();
    }
}
