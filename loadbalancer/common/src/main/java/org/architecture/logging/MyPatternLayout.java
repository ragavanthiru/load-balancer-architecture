package org.architecture.logging;

import org.apache.log4j.MDC;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

public class MyPatternLayout extends PatternLayout {

    private final ThreadLocal<String> threadId = new ThreadLocal<String>() {

        @Override
        protected String initialValue() {
            String t = Thread.currentThread().getName()+" "+Long.toString(Thread.currentThread().getId());
            MDC.put("threadId", t);
            return t;
        }
    };

    @Override
    public String format(LoggingEvent event) {

        this.threadId.get();
        return super.format(event);
    }
}
