package org.architecture.client;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ClientThread implements Runnable{
    final static Logger logger = Logger.getLogger(AsyncClientThread.class);
    private int noOfRequests;
    private int timeBetweenRequests;
    private HttpURLConnection con;

    public ClientThread(int noOfRequests, int timeBetweenRequests){
        this.noOfRequests = noOfRequests;
        this.timeBetweenRequests = timeBetweenRequests;

        try {
            URL url = new URL("http://localhost:8080/api/health");
            this.con = (HttpURLConnection) url.openConnection();
            this.con.setRequestMethod("POST");
            this.con.setConnectTimeout(5000);
            this.con.setReadTimeout(5000);
            this.con.setDoOutput(true);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void run() {

        for(int i = 0; i<noOfRequests; i++) {

            //JSON String need to be constructed for the specific resource.
            //We may construct complex JSON using any third-party JSON libraries such as jackson or org.json
            String jsonInputString = "{\"name\": \"Upendra\", \"job\": \"Programmer\"}";

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                int code = con.getResponseCode();
                System.out.println(code);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println(response.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
