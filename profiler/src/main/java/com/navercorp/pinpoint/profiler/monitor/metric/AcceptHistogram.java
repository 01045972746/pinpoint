package com.navercorp.pinpoint.profiler.monitor.metric;

/**
 * @author emeroad
 */
public interface AcceptHistogram {
    boolean addResponseTime(String parentApplicationName, short serviceType, int millis);
}
