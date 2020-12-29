package org.architecture;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class DisruptorTest {
    final static Logger logger = Logger.getLogger(DisruptorTest.class);
    @SuppressWarnings({"unchecked"})
    public static void main(String[] args) throws InterruptedException, IOException, AlertException, TimeoutException {
        final int cores = Runtime.getRuntime().availableProcessors();

        Executor executor = Executors.newFixedThreadPool(cores, new NamedThreadFactory("EXECUTOR - "));

        final int bufferSize = 8 * 1024;
        int ringSize = new Double(Math.pow(2, cores + 1)).intValue();

        final RingBuffer<ByteBuffer> bufferRing = new Disruptor<ByteBuffer>(
                    new EventFactory<ByteBuffer>() {
                        @Override public ByteBuffer newInstance() {
                            return ByteBuffer.allocate(bufferSize);
                        }
                    },
                    ringSize,
                    executor,
                    ProducerType.SINGLE,//new SingleThreadedClaimStrategy(ringSize),
                    new BlockingWaitStrategy()
        ).start();


        int handlerCount = cores;
        WorkHandler<SelectionEvent>[] handlers = new WorkHandler[handlerCount];
        for (int i = 0; i < handlerCount; i++) {
            handlers[i] = new WorkHandler<SelectionEvent>() {

                    ByteBuffer msg = ByteBuffer.wrap(
                        ("HTTP/1.1 200 OK\r\n" +
                        "Connection: Keep-Alive\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: 12\r\n\r\n" +
                        "Hello World!").getBytes());

                    @Override public void onEvent(SelectionEvent ev) throws Exception {
                        // Allocate a ByteBuffer from a RingBuffer
                        ByteBuffer buffer = bufferRing.get(ev.bufferId);
                        if (buffer.position() > 0) {
                            buffer.clear();
                        }
                        logger.debug(ev.key.isReadable());
                        try {
                            int read = -1;
                            try {
                                // Read data from the Channel
                                read = ev.channel.read(buffer);
                            } catch (IOException e) {

                                switch (e.getMessage()) {
                                    case "Connection reset by peer":
                                    case "Broken pipe":
                                    break;
                                    default:
                                        e.printStackTrace();
                                }
                            } catch (CancelledKeyException e) {
                                ev.channel.close();
                            }
                            logger.debug(" ev.bufferId="+ev.bufferId+", read = "+read);
                            if (read > 0) {
                                try {
                                    // Write the response immediately
                                    ev.channel.write(msg.duplicate());
                                } catch (IOException e) {

                                    switch (e.getMessage()) {
                                        case "Connection reset by peer":
                                        case "Broken pipe":
                                        break;
                                        default:
                                            e.printStackTrace();
                                    }
                                } catch (CancelledKeyException e) {
                                    // Close the channel if something goes wrong
                                    ev.channel.close();
                                }

                                // Read the data into memory
                                buffer.flip();
                                byte[] bytes = new byte[buffer.remaining()];
                                buffer.get(bytes);
                                //ev.channel.close();
                                logger.debug("is Connected "+ev.channel.isConnected());
                                //String input = new String(bytes);
                            } else {
                                ev.key.cancel();
                            }
                        } finally {
                            // Put the ByteBuffer back into the RingBuffer for re-use
                            bufferRing.publish(ev.bufferId);
                        }
                    }
            };
        }

        // Use a WorkerPool for handling requests
        WorkerPool acceptPool = new WorkerPool(
                new EventFactory<SelectionEvent>() {
                    @Override public SelectionEvent newInstance() {
                        return new SelectionEvent();
                    }
                },
                //ringSize,
               // new SingleThreadedClaimStrategy(ringSize),
               // new BlockingWaitStrategy(),
                new FatalExceptionHandler(),
                handlers);

        final RingBuffer<SelectionEvent> workerRing = acceptPool.start(executor);

        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        final Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", 3000), 1024);

        // Allocate the first worker
        long workerId = workerRing.next();
        logger.debug("WORKER POOL ID = "+workerId);
        while (true) {
            int cnt = 0;
            try {
                cnt = selector.select();
            } catch (CancelledKeyException ignored) {
            // There's a bug on Mac OS X's JVM that might throw a bogus CancelledKeyException
            }
            if (cnt > 0) {
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    try {
                        logger.debug("Before vlaidation key = "+key);
                        if (key.isValid()) {
                            SelectionEvent event;
                            logger.debug("key = "+key);
                            if (key.isAcceptable()) {
                                if (serverSocketChannel.isOpen()) {
                                    ServerSocket serverSocket = serverSocketChannel.socket();
                                    serverSocket.setReceiveBufferSize(bufferSize);
                                    serverSocket.setReuseAddress(true);

                                    SocketChannel channel;
                                    while (null != (channel = serverSocketChannel.accept())) {
                                        channel.configureBlocking(false);
                                        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                                        channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                                        channel.setOption(StandardSocketOptions.SO_RCVBUF, bufferSize);
                                        channel.setOption(StandardSocketOptions.SO_SNDBUF, bufferSize);
                                        channel.register(selector, SelectionKey.OP_READ);

                                        // Allocate an Event object for dispatching to the handler
                                        logger.debug("INSIDE ACCEPTABLE WORKER POOL ID = "+workerId);
                                        event = workerRing.get(workerId);
                                        event.id = workerId;
                                        event.channel = channel;
                                        event.selector = selector;
                                        event.key = key;
                                        event.serverChannel = serverSocketChannel;
                                        // Allocate a new ByteBuffer from this thread (since I'm using a SingleThreadedClaimStrategy)
                                        event.bufferId = bufferRing.next();
                                        logger.debug("INSIDE ACCEPTABLE BUFFER RING ID = "+event.bufferId);
                                        // Dispatch this event to a handler
                                        workerRing.publish(workerId);
                                        // Immediately allocate the next worker ID
                                        workerId = workerRing.next();
                                    }
                                }
                            }

                            if (key.isReadable() || key.isWritable()) {
                                logger.debug("INSIDE READABLE WORKER POOL ID = "+workerId);
                                // Allocate an Event object for dispatching to the handler
                                event = workerRing.get(workerId);
                                event.id = workerId;
                                event.channel = (SocketChannel) key.channel();
                                event.selector = selector;
                                event.key = key;
                                event.serverChannel = serverSocketChannel;
                                // Allocate a new ByteBuffer from this thread (since I'm using a SingleThreadedClaimStrategy)
                                event.bufferId = bufferRing.next();
                                logger.debug("INSIDE READABLE BUFFER RING ID = "+event.bufferId);

                                // Dispatch this event to a handler
                                workerRing.publish(workerId);
                                // Immediately allocate the next worker ID
                                workerId = workerRing.next();
                            }

                        }
                    } catch (CancelledKeyException e) {
                        key.channel().close();
                    }
                }
            }

        }
    }

    private static class SelectionEvent {

        Long id;
        Selector selector;
        ServerSocketChannel serverChannel;
        SelectionKey key;
        SocketChannel channel;
        long bufferId = -1L;

        public SelectionEvent() {
        }

    }
}
