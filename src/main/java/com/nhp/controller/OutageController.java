package com.nhp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhp.dto.OutageEvent;
import com.nhp.dto.AsnOutage;
import com.nhp.dto.AsnInfo;
import com.nhp.services.OutageEventService;
import com.nhp.services.AsnOutageService;
import com.nhp.services.AsnGeolocationService;
import com.nhp.dto.OutageStats;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class OutageController {

    @Autowired
    private OutageEventService outageEventService;

    @Autowired
    private AsnOutageService asnOutageService;

    @Autowired
    private AsnGeolocationService asnGeolocationService;

    /**
     * Get recent outage events
     */
    @GetMapping("/outages/recent")
    public ResponseEntity<List<OutageEvent>> getRecentOutages(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<OutageEvent> outages = outageEventService.getRecentOutages(limit);
            return ResponseEntity.ok(outages);
        } catch (Exception e) {
            log.error("Error fetching recent outages", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get active (ongoing) outages
     */
    @GetMapping("/outages/active")
    public ResponseEntity<List<OutageEvent>> getActiveOutages() {
        try {
            List<OutageEvent> outages = outageEventService.getActiveOutages();
            return ResponseEntity.ok(outages);
        } catch (Exception e) {
            log.error("Error fetching active outages", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get outage events for a specific ASN
     */
    @GetMapping("/asn/{asn}/events")
    public ResponseEntity<List<OutageEvent>> getAsnEvents(@PathVariable Integer asn) {
        try {
            List<OutageEvent> events = outageEventService.getAsnEvents(asn);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("Error fetching events for ASN: {}", asn, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get ASN information including geolocation
     */
    @GetMapping("/asn/{asn}/info")
    public ResponseEntity<AsnInfo> getAsnInfo(@PathVariable Integer asn) {
        try {
            AsnInfo info = asnGeolocationService.getAsnInfo(asn);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Error fetching ASN info for ASN: {}", asn, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get ASN-level outage correlations
     */
    @GetMapping("/asn/{asn}/outages")
    public ResponseEntity<List<AsnOutage>> getAsnOutages(@PathVariable Integer asn) {
        try {
            List<AsnOutage> outages = asnOutageService.getAsnOutages(asn);
            return ResponseEntity.ok(outages);
        } catch (Exception e) {
            log.error("Error fetching ASN outages for ASN: {}", asn, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get outage history for a specific prefix
     */
    @GetMapping("/prefix/{prefix}/history")
    public ResponseEntity<List<OutageEvent>> getPrefixHistory(@PathVariable String prefix) {
        try {
            List<OutageEvent> history = outageEventService.getPrefixHistory(prefix);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching history for prefix: {}", prefix, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get summary statistics
     */
    @GetMapping("/stats/summary")
    public ResponseEntity<OutageStats> getSummaryStats() {
        try {
            OutageStats stats = outageEventService.getSummaryStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching summary stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get global outage map data (for frontend visualization)
     */
    @GetMapping("/outages/map")
    public ResponseEntity<List<OutageEvent>> getOutageMapData(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            List<OutageEvent> outages = outageEventService.getOutagesForMap(hours);
            return ResponseEntity.ok(outages);
        } catch (Exception e) {
            log.error("Error fetching outage map data", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}