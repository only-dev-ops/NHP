package com.nhp.services;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.nhp.dto.AsnOutage;
import com.nhp.dto.OutageEvent;
import com.nhp.repository.AsnOutageRepository;
import com.nhp.repository.OutageEventRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AsnOutageService {

    @Autowired
    private AsnOutageRepository asnOutageRepository;

    @Autowired
    private OutageEventRepository outageEventRepository;

    @Autowired
    private AsnGeolocationService asnGeolocationService;

    // In-memory tracking of active ASN outages
    private final Map<Integer, AsnOutageTracker> activeAsnOutages = new ConcurrentHashMap<>();

    // Timeout for ASN outage correlation (5 minutes)
    private static final Duration ASN_OUTAGE_TIMEOUT = Duration.ofMinutes(5);

    /**
     * Process a new outage event and potentially correlate it with existing ASN
     * outages
     */
    public void processOutageEvent(OutageEvent event) {
        if (!"outage_start".equals(event.getEventType())) {
            return;
        }

        Integer asn = event.getOriginAsn();
        AsnOutageTracker tracker = activeAsnOutages.get(asn);

        if (tracker == null) {
            // Start new ASN outage
            tracker = new AsnOutageTracker(asn, event.getTimestamp());
            activeAsnOutages.put(asn, tracker);
        }

        // Add prefix to the ASN outage
        tracker.addPrefix(event.getPrefix());
        tracker.setLastActivity(Instant.now());

        log.debug("Added prefix {} to ASN {} outage tracker", event.getPrefix(), asn);
    }

    /**
     * Process a recovery event and potentially close ASN outages
     */
    public void processRecoveryEvent(OutageEvent event) {
        if (!"recovery".equals(event.getEventType())) {
            return;
        }

        Integer asn = event.getOriginAsn();
        AsnOutageTracker tracker = activeAsnOutages.get(asn);

        if (tracker != null) {
            tracker.removePrefix(event.getPrefix());
            tracker.setLastActivity(Instant.now());

            // If all prefixes have recovered, close the ASN outage
            if (tracker.getPrefixes().isEmpty()) {
                closeAsnOutage(tracker);
            }

            log.debug("Removed prefix {} from ASN {} outage tracker", event.getPrefix(), asn);
        }
    }

    /**
     * Close an ASN outage and persist it to the database
     */
    private void closeAsnOutage(AsnOutageTracker tracker) {
        Instant endTime = Instant.now();
        Duration duration = Duration.between(tracker.getStartTime(), endTime);

        AsnOutage asnOutage = AsnOutage.builder()
                .asn(tracker.getAsn())
                .startTime(tracker.getStartTime())
                .endTime(endTime)
                .duration(duration.toString())
                .prefixes(tracker.getPrefixes().toArray(new String[0]))
                .severity(calculateSeverity(tracker.getAsn(), tracker.getPrefixes().size()))
                .country(getAsnCountry(tracker.getAsn()))
                .createdAt(Instant.now())
                .build();

        asnOutageRepository.save(asnOutage);
        activeAsnOutages.remove(tracker.getAsn());

        log.info("Closed ASN outage: ASN={}, duration={}, prefixes={}",
                tracker.getAsn(), duration, tracker.getPrefixes());
    }

    /**
     * Scheduled task to close timed-out ASN outages
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void closeTimedOutOutages() {
        Instant now = Instant.now();
        List<Integer> asnsToClose = new ArrayList<>();

        for (Map.Entry<Integer, AsnOutageTracker> entry : activeAsnOutages.entrySet()) {
            AsnOutageTracker tracker = entry.getValue();
            if (now.isAfter(tracker.getLastActivity().plus(ASN_OUTAGE_TIMEOUT))) {
                asnsToClose.add(entry.getKey());
            }
        }

        for (Integer asn : asnsToClose) {
            AsnOutageTracker tracker = activeAsnOutages.get(asn);
            if (tracker != null) {
                closeAsnOutage(tracker);
            }
        }

        if (!asnsToClose.isEmpty()) {
            log.info("Closed {} timed-out ASN outages", asnsToClose.size());
        }
    }

    /**
     * Get ASN outages for a specific ASN
     */
    public List<AsnOutage> getAsnOutages(Integer asn) {
        return asnOutageRepository.findByAsnOrderByStartTimeDesc(asn);
    }

    /**
     * Get active ASN outages
     */
    public List<AsnOutage> getActiveAsnOutages() {
        return asnOutageRepository.findActiveAsnOutages();
    }

    /**
     * Calculate severity as percentage of ASN's total prefixes
     */
    private Integer calculateSeverity(Integer asn, int affectedPrefixes) {
        // This is a simplified calculation
        // In a real implementation, you'd query the total number of prefixes for this
        // ASN
        long totalPrefixes = outageEventRepository.countDistinctPrefixByOriginAsn(asn);
        if (totalPrefixes == 0) {
            return 100; // If we don't know, assume 100%
        }

        return Math.min(100, (int) ((affectedPrefixes * 100) / totalPrefixes));
    }

    /**
     * Get country for ASN using geolocation service
     */
    private String getAsnCountry(Integer asn) {
        try {
            return asnGeolocationService.getAsnInfo(asn).getCountry();
        } catch (Exception e) {
            log.debug("Failed to get country for ASN {}: {}", asn, e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Inner class to track active ASN outages
     */
    private static class AsnOutageTracker {
        private final Integer asn;
        private final Instant startTime;
        private final List<String> prefixes = new ArrayList<>();
        private Instant lastActivity;

        public AsnOutageTracker(Integer asn, Instant startTime) {
            this.asn = asn;
            this.startTime = startTime;
            this.lastActivity = startTime;
        }

        public void addPrefix(String prefix) {
            if (!prefixes.contains(prefix)) {
                prefixes.add(prefix);
            }
        }

        public void removePrefix(String prefix) {
            prefixes.remove(prefix);
        }

        // Getters
        public Integer getAsn() {
            return asn;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public List<String> getPrefixes() {
            return prefixes;
        }

        public Instant getLastActivity() {
            return lastActivity;
        }

        public void setLastActivity(Instant lastActivity) {
            this.lastActivity = lastActivity;
        }
    }
}