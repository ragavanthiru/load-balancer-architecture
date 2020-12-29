package org.architecture.disruptor;

import com.lmax.disruptor.EventHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ConcurrentHashMap;

public class HttpEventHandler implements EventHandler<HttpEvent> {
    private int id;
    private ConcurrentHashMap concurrentHashMap;

    public HttpEventHandler(int id, ConcurrentHashMap concurrentHashMap) {
        this.id = id;
        this.concurrentHashMap = concurrentHashMap;

    }

    public void onEvent(HttpEvent event, long sequence, boolean endOfBatch) throws Exception {
        System.out.println("EVENT ---- "+event.getRequestId()+", ID="+id);
        if (sequence % Runtime.getRuntime().availableProcessors() == id) {

            String requestId = event.getRequestId();
            ByteBuffer buffer = event.getBuffer();
            int numRead = event.getNumRead();
            SocketChannel channel = event.getChannel();
            System.out.println("ALLOWABLE ---- "+event.getRequestId()+", ID="+id+",  state="+channel.isConnected());

            ByteBuffer responseBuffer = handleRequest(buffer, numRead);


            this.concurrentHashMap.put(requestId, responseBuffer);
            /*SelectionKey selectionKey = event.getKey();
            SocketChannel channel1 = (SocketChannel) selectionKey.channel();
            channel1.configureBlocking(false);
            channel.write(buffer);*/

            //int pos = responseBuffer.position();
            //debugBuffer(responseBuffer, pos);
            int bytes = safeWrite(channel, responseBuffer);
            System.out.println("WRITTTEN ----  state="+channel.socket().isConnected()+", bytes="+bytes);
        }
    }

    private ByteBuffer handleRequest(ByteBuffer buffer, int numRead) throws Exception {

        buffer.flip();

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        String request = new String(data, "US-ASCII");
        System.out.println("REQUEST STRING ---- "+request);
        request = request.split("\n")[0].trim();


        String response = serverRequest(request);

        buffer.clear();

        buffer.put(response.getBytes());
        return buffer;
    }

    private String serverRequest(String request) throws Exception {
        String response =
                ("HTTP/1.1 200 OK\r\n" +
                        "Connection: Keep-Alive\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: 12\r\n\r\n" +
                        "Hello World!");
        return response;
        /*if (request.startsWith("GET")) {

            // http request parsing and response generation should be done here.


            return response;
        }

        return null;*/
    }

    private int safeWrite(WritableByteChannel channel, ByteBuffer src) throws IOException {
        int written = -1;
        try {
            // Write the response immediately
            written = channel.write(src);
        } catch (IOException e) {
            switch ("" + e.getMessage()) {
                case "null":
                case "Connection reset by peer":
                case "Broken pipe":
                    break;
                default:
                    e.printStackTrace();
            }
            channel.close();
        } catch (CancelledKeyException e) {
            channel.close();
        }
        return written;
    }

    private void debugBuffer(ByteBuffer buffer, int numRead) throws UnsupportedEncodingException {
        buffer.flip();

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        String request = new String(data, "US-ASCII");
        System.out.println("REQUEST STRING ---- "+request);
    }
}