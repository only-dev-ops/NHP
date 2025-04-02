package com.nhp.stream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhp.services.MetricsService;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;
import reactor.core.Disposable;
import reactor.util.retry.Retry;

@Slf4j
@Component
public class RipeStreamClient {

    @Autowired
    private MetricsService metricsService;

    private static final String RIS_WS_URL = "wss://ris-live.ripe.net/v1/ws/";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Disposable connection;

    @PostConstruct
    public void startStream() {
        // TODO fetch this list of prefixes from the kafka stream
        restartStreamWithPrefixes(List.of("8.8.8.0/24"));
    }

    // gracefully handle app shutdown, least thing we can do
    @PreDestroy
    public void stopStream() {
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
            log.info("RIPE stream connection closed on shutdown.");
        }
    }

    public void restartStreamWithPrefixes(List<String> prefixes) {
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
            log.info("Closed previous RIPE stream connection");
        }

        log.info("Connecting to RIPE RIS WebSocket with {} prefix(es)", prefixes.size());

        connection = HttpClient.create()
                .websocket(WebsocketClientSpec.builder().build())
                .uri(RIS_WS_URL)
                .handle((inbound, outbound) -> {
                    // send the subscription message
                    outbound.sendString(Mono.just(buildSubscribeMsg(prefixes)))
                            .then()
                            .subscribe();

                    // consumer logic from this socket now
                    inbound.receive().asString()
                            .doOnNext(msg -> {
                                log.info("BGP Message: {}", msg);
                                metricsService.incrementBgpMessagesReceieved();
                            })
                            .doOnError(error -> log.error("Error while streaming", error))
                            .doOnComplete(() -> log.warn("Stream completed/disconnected"))
                            .subscribe();
                    return Mono.never();
                })
                .doOnError(error -> log.error("WebSocket connection error", error))
                .retryWhen(Retry.backoff(5, java.time.Duration.ofSeconds(5))
                        .doBeforeRetry(retrySignal -> log.warn("Retrying RIPE WebSocket connection (attempt {})",
                                retrySignal.totalRetries() + 1)))
                .subscribe();
    }

    private String buildSubscribeMsg(List<String> prefixes) {
        try {
            Map<String, Object> parameters = Map.of(
                    "type", "ris_subscribe",
                    "data", Map.of(
                            "type", "UPDATE",
                            "prefix", prefixes,
                            "moreSpecific", true));
            return objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while building subscribe message", e);
        }
    }
}
