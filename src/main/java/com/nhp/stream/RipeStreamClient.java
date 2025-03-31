package com.nhp.stream;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;

@Slf4j
@Component
public class RipeStreamClient {

    private static final String RIS_WS_URL = "wss://ris-live.ripe.net/v1/ws/";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void startStream() {
        log.info("Starting Stream");
        HttpClient.create()
                .websocket(WebsocketClientSpec.builder().build())
                .uri(RIS_WS_URL)
                .handle((inbound, outbound) -> {
                    outbound.sendString(Mono.just(
                            getSubscribeMsg())).then().subscribe();
                    inbound.receive().asString()
                            .doOnNext(s -> log.info(s))
                            .doOnError(error -> log.error("Error while streaming", error))
                            .doOnComplete(() -> log.warn("⚠️ Stream completed/disconnected"))
                            .subscribe();
                    return Mono.never();
                })
                .doOnError(error -> log.error("Websocket error: ", error))
                .subscribe();
    }

    private String getSubscribeMsg() {
        try {
            Map<String, Object> parameters = Map.of(
                    "type", "ris_subscribe",
                    "data", Map.of(
                            "type", "UPDATE",
                            "host", "rrc00.ripe.net"));
            return objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while building subscribe message. {}", e);
        }
    }
}
