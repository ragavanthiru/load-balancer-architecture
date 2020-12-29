package org.architecture.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.RandomStringUtils;
import org.architecture.disruptor.context.ConfigurationContext;
import org.architecture.disruptor.description.TransportInDescription;

public class DisruptorListener {

    private Selector selector;
    private HttpEventFactory factory;
    private Executor executor;
    private HttpEventProducer producer ;
    private TransportInDescription transportInDescription;
    private int bufferSize = 1024;
    private ConcurrentHashMap concurrentHashMapResponse;
    private ConcurrentHashMap concurrentHashMapKey;
    final Map<Long, SocketChannel> channels = new ConcurrentSkipListMap<>();
    final Map<Long, SelectionKey> selectionKeys = new ConcurrentSkipListMap<>();

    public DisruptorListener(){}

    public void init(TransportInDescription transportInDescription, ConfigurationContext cfg){

        this.transportInDescription = transportInDescription;
        this.setConcurrentHashMapResponse(new ConcurrentHashMap<>());
        this.concurrentHashMapKey = new ConcurrentHashMap<>();

        this.factory = new HttpEventFactory();
        this.executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()); // a thread pool to which we can assign tasks


        /*ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;

        WaitStrategy waitStrategy = new BusySpinWaitStrategy();
        Disruptor<ValueEvent> disruptor
                = new Disruptor<>(
                ValueEvent.EVENT_FACTORY,
                16,
                threadFactory,
                ProducerType.SINGLE,
                waitStrategy);*/
        Disruptor<HttpEvent> disruptor = new Disruptor<HttpEvent>(factory, bufferSize, executor);

        HttpEventHandler [] handlers = new HttpEventHandler[Runtime.getRuntime().availableProcessors()];
        for(int i = 0; i<Runtime.getRuntime().availableProcessors();i++){
            handlers[i] = new HttpEventHandler(i, concurrentHashMapResponse);
        }

        disruptor.handleEventsWith(handlers);
        disruptor.start();

        RingBuffer<HttpEvent> ringBuffer = disruptor.getRingBuffer();
        producer = new HttpEventProducer(ringBuffer, concurrentHashMapResponse);


    }

    public static void main(String[] args) throws Exception
    {
        System.out.println("----- Running the server on machine with "+Runtime.getRuntime().availableProcessors()+" cores -----");

        //DisruptorListener server = new DisruptorListener(InetAddress.getByName("localhost"), 4333);
        //HttpEventFactory factory = new HttpEventFactory();


        //Executor executor = Executors.newFixedThreadPool(
        //        Runtime.getRuntime().availableProcessors()); // a thread pool to which we can assign tasks

        //Disruptor<HttpEvent> disruptor = new Disruptor<HttpEvent>(factory, bufferSize, executor);

        /*HttpEventHandler [] handlers = new HttpEventHandler[Runtime.getRuntime().availableProcessors()];
        for(int i = 0; i<Runtime.getRuntime().availableProcessors();i++){
            handlers[i] = new HttpEventHandler(i, server.getConcurrentHashMapResponse());
        }*/

        //disruptor.handleEventsWith(handlers);
        //disruptor.start();

        //RingBuffer<HttpEvent> ringBuffer = disruptor.getRingBuffer();
        //server.setProducer(new HttpEventProducer(ringBuffer, server.getConcurrentHashMapResponse()));

        /*try {
            System.out.println("\n====================Server Details====================");
            System.out.println("Server Machine: "+ InetAddress.getLocalHost().getCanonicalHostName());
            System.out.println("Port number: " + server.getPort());

        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        }*/
        /*try {
            server.start();
        } catch (IOException e) {
            System.err.println("Error occured in DisruptorListener:" + e.getMessage());
            System.exit(0);
        }*/
    }
    public void start() throws IOException {
        this.selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);


        InetSocketAddress listenAddr = new InetSocketAddress(this.transportInDescription.getAddr(), this.transportInDescription.getPort());
        serverChannel.socket().bind(listenAddr);
        serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server ready. Ctrl-C to stop.");

        while (true) {

            this.selector.select();
            Iterator keys = this.selector.selectedKeys().iterator();
            while (keys.hasNext()) {

                SelectionKey key = (SelectionKey) keys.next();
                keys.remove();
                System.out.println("key ....."+ key);
                if (! key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    System.out.println("ACCEPTABLE ....."+ key);
                   /* event = workerRing.get(workerId);
                    event.id = workerId;
                    channels.put(workerId, (SocketChannel) key.channel());
                    selectionKeys.put(workerId, key);*/
                    this.accept(key);
                }
                else if (key.isReadable()) {
                    System.out.println("READABLE ....."+ key);
                    /*event = workerRing.get(workerId);
                    event.id = workerId;
                    channels.put(workerId, (SocketChannel) key.channel());
                    selectionKeys.put(workerId, key);*/
                    this.read(key);
                }
                else if (key.isWritable()) {
                    System.out.println("WRITABLE ....."+ key);

                    /*event = workerRing.get(workerId);
                    event.id = workerId;
                    channels.put(workerId, (SocketChannel) key.channel());
                    selectionKeys.put(workerId, key);*/
                    this.write(key);
                }
            }

            System.out.println("NO NEXT .....");
        }

    }

    private void accept(SelectionKey key) throws IOException {

        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);

        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        channel.register(this.selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int numRead = -1;
        try {
            numRead = channel.read(buffer);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("NUMREAD  ....."+numRead);
        if (numRead == -1) {

            Socket socket = channel.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            channel.close();
            key.cancel();
            return;

        }
        String requestID = RandomStringUtils.random(15, true, true);

        while(concurrentHashMapKey.containsValue(requestID) || concurrentHashMapResponse.containsKey(requestID)){
            requestID = RandomStringUtils.random(15, true, true);
        }

        concurrentHashMapKey.put(key, requestID);

        try {
            //ByteBuffer res = handleRequest(buffer, numRead);
            //int b = safeWrite(channel, res);

            //System.out.println(" B when read....."+b);

            this.producer.onData(requestID, buffer, numRead, channel, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(" COmpleted on Data .....");
        //channel.register(this.selector, SelectionKey.OP_WRITE, buffer);
    }

    private boolean responseReady(SelectionKey key){

        String requestId = concurrentHashMapKey.get(key).toString();
        String response = concurrentHashMapResponse.get(requestId).toString();
        System.out.println("responseReady resposne ....."+response);
        if(response.equals("0")){
            concurrentHashMapKey.remove(key);
            concurrentHashMapResponse.remove(requestId);
            return true;
        }else{
            return false;
        }

    }

    private void write(SelectionKey key) throws IOException {
        boolean isExist = responseReady(key);
        System.out.println("responseReady(key) ....."+isExist);
        if(isExist) {

            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer inputBuffer = (ByteBuffer) key.attachment();

            System.out.println("inputBuffer ....."+inputBuffer.toString());
            inputBuffer.flip();
            channel.write(inputBuffer);
            channel.close();
            key.cancel();

        }

    }

    public HttpEventProducer getProducer() {
        return producer;
    }

    public void setProducer(HttpEventProducer producer) {
        this.producer = producer;
    }

    public ConcurrentHashMap getConcurrentHashMapResponse() {
        return concurrentHashMapResponse;
    }

    public void setConcurrentHashMapResponse(ConcurrentHashMap concurrentHashMapResponse) {
        this.concurrentHashMapResponse = concurrentHashMapResponse;
    }

    public Selector getSelector() {
        return selector;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
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

}
