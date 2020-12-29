package org.architecture.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

public class HttpEventProducer {

    private final RingBuffer<HttpEvent> ringBuffer;
    private final ConcurrentHashMap concurrentHashMap;

    public HttpEventProducer(RingBuffer<HttpEvent> ringBuffer, ConcurrentHashMap concurrentHashMap)
    {
        this.ringBuffer = ringBuffer;
        this.concurrentHashMap = concurrentHashMap;
    }

    public void onData(String requestId, ByteBuffer buffer, int numRead, SocketChannel channel, SelectionKey selectionKey)
    {
        long sequence = ringBuffer.next();
        System.out.println("sequence  ....."+  sequence +",  channelstate="+channel.isConnected());
        try
        {
            HttpEvent event = ringBuffer.get(sequence);
            event.setBuffer(buffer);
            event.setRequestId(requestId);
            event.setNumRead(numRead);
            event.setChannel(channel);
            event.setKey(selectionKey);
        }
        finally
        {
            concurrentHashMap.put(requestId, "0");
            ringBuffer.publish(sequence);
        }
    }
}
