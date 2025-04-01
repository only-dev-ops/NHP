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
import com.nhp.model.MonitoredPrefix;
import com.nhp.repos.MonitoredPrefixesRepo;
import com.nhp.stream.RipeStreamClient;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/prefixes")
public class MonitoredPrefixController {

    @Autowired
    private final MonitoredPrefixesRepo monitoredPrefixesRepo;

    @Autowired
    private final RipeStreamClient ripeStreamClient;

    public MonitoredPrefixController(MonitoredPrefixesRepo repo, RipeStreamClient client) {
        monitoredPrefixesRepo = repo;
        ripeStreamClient = client;
    }

    @PostMapping("/add-one")
    public ResponseEntity<Void> addPrefix(@RequestBody SingleMonitoredPrefixRequest request) {
        monitoredPrefixesRepo.save(
                MonitoredPrefix.builder()
                        .prefix(request.getPrefix())
                        .createdAt(Instant.now())
                        .build());
        // restart the live streamer so we can track this new prefix
        ripeStreamClient.restartStreamWithPrefixes();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/add")
    public ResponseEntity<Void> addPrefixes(@RequestBody MultipleMonitoredPrefixesRequest request) {
        List<MonitoredPrefix> prefixes = request.getPrefixes()
                .stream()
                .map(prefix -> MonitoredPrefix.builder().prefix(prefix).createdAt(Instant.now()).build())
                .toList();
        // save in batch
        monitoredPrefixesRepo.saveAll(prefixes);

        ripeStreamClient.restartStreamWithPrefixes();

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<MonitoredPrefix> getPrefixes() {
        return monitoredPrefixesRepo.findAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        monitoredPrefixesRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
