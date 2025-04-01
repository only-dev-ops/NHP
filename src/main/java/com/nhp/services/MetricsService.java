package com.nhp.services;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class MetricsService {

    private final Counter bgpMessagesReceived;
    private final Counter streamRestarts;
    private final Counter websocketErrors;

    public MetricsService(MeterRegistry registry) {
        this.bgpMessagesReceived = registry.counter("ripe.bgp.messages.received");
        this.streamRestarts = registry.counter("ripe.stream.restarts");
        this.websocketErrors = registry.counter("ripe.websocket.errors");
    }

    public void incrementBgpMessagesReceieved() {
        bgpMessagesReceived.increment();
    }

    public void incrementStreamRestarts() {
        streamRestarts.increment();
    }

    public void recordWebsocketError() {
        websocketErrors.increment();
    }

}
