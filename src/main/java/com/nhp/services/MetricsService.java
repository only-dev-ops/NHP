package com.nhp.services;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class MetricsService {

    private final Counter bgpMessagesReceived;
    private final Counter bgpMessagesProcessed;
    private final Counter bgpProcessingErrors;
    private final Counter prefixOutages;
    private final Counter prefixRecoveries;
    private final Counter streamRestarts;
    private final Counter websocketErrors;

    public MetricsService(MeterRegistry registry) {
        this.bgpMessagesReceived = registry.counter("ripe.bgp.messages.received");
        this.bgpMessagesProcessed = registry.counter("ripe.bgp.messages.processed");
        this.bgpProcessingErrors = registry.counter("ripe.bgp.processing.errors");
        this.prefixOutages = registry.counter("ripe.prefix.outages");
        this.prefixRecoveries = registry.counter("ripe.prefix.recoveries");
        this.streamRestarts = registry.counter("ripe.stream.restarts");
        this.websocketErrors = registry.counter("ripe.websocket.errors");
    }

    public void incrementBgpMessagesReceieved() {
        bgpMessagesReceived.increment();
    }

    public void incrementBgpMessagesProcessed() {
        bgpMessagesProcessed.increment();
    }

    public void incrementBgpProcessingErrors() {
        bgpProcessingErrors.increment();
    }

    public void incrementPrefixOutages() {
        prefixOutages.increment();
    }

    public void incrementPrefixRecoveries() {
        prefixRecoveries.increment();
    }

    public void incrementStreamRestarts() {
        streamRestarts.increment();
    }

    public void recordWebsocketError() {
        websocketErrors.increment();
    }

}
