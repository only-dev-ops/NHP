package com.nhp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhp.dto.MultipleMonitoredPrefixesRequest;
import com.nhp.dto.SingleMonitoredPrefixRequest;
import com.nhp.stream.RipeStreamClient;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/prefixes")
public class MonitoredPrefixController {

    @Autowired
    private final RipeStreamClient ripeStreamClient;

    public MonitoredPrefixController(RipeStreamClient client) {
        ripeStreamClient = client;
    }

    @PostMapping("/add-one")
    public ResponseEntity<Void> addPrefix(@RequestBody SingleMonitoredPrefixRequest request) {
        // TODO save the prefix to the new kafka topic
        // We shouldn't have to pass list of prefixes here, find a fix
        ripeStreamClient.restartStreamWithPrefixes(List.of("8.8.8.0/24"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/add")
    public ResponseEntity<Void> addPrefixes(@RequestBody MultipleMonitoredPrefixesRequest request) {

        // save in batch
        // TODO save all these prefixes to the kafka topic
        // TODO do something about restartStream needing the list of prefixes

        ripeStreamClient.restartStreamWithPrefixes(List.of("8.8.8.0/24"));

        return ResponseEntity.ok().build();
    }

    // TODO Redis implementation for fetching and deleting the prefixes

    // @GetMapping
    // public List<MonitoredPrefix> getPrefixes() {
    // return monitoredPrefixesRepo.findAll();
    // }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<?> delete(@PathVariable Long id) {
    // monitoredPrefixesRepo.deleteById(id);
    // return ResponseEntity.noContent().build();
    // }
}
