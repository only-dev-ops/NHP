package com.nhp.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nhp.dto.OutageEvent;
import com.nhp.dto.OutageStats;
import com.nhp.repository.OutageEventRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OutageEventService {

    @Autowired
    private OutageEventRepository outageEventRepository;

    /**
     * Record the start of an outage event
     */
    public void recordOutageStart(String prefix, String originAsn, String lastPath, Set<String> withdrawnBy) {
        try {
            OutageEvent event = OutageEvent.builder()
                    .prefix(prefix)
                    .originAsn(Integer.parseInt(originAsn))
                    .timestamp(Instant.now())
                    .eventType("outage_start")
                    .lastPath(lastPath)
                    .withdrawnBy(withdrawnBy.toArray(new String[0]))
                    .build();

            outageEventRepository.save(event);
            log.info("Recorded outage start: prefix={}, origin_asn={}", prefix, originAsn);

        } catch (Exception e) {
            log.error("Failed to record outage start: prefix={}, origin_asn={}", prefix, originAsn, e);
        }
    }

    /**
     * Record the recovery of a prefix
     */
    public void recordRecovery(String prefix, String originAsn, String asPath, Set<String> withdrawnBy) {
        try {
            OutageEvent event = OutageEvent.builder()
                    .prefix(prefix)
                    .originAsn(Integer.parseInt(originAsn))
                    .timestamp(Instant.now())
                    .eventType("recovery")
                    .lastPath(asPath)
                    .withdrawnBy(withdrawnBy.toArray(new String[0]))
                    .build();

            outageEventRepository.save(event);
            log.info("Recorded recovery: prefix={}, origin_asn={}", prefix, originAsn);

        } catch (Exception e) {
            log.error("Failed to record recovery: prefix={}, origin_asn={}", prefix, originAsn, e);
        }
    }

    /**
     * Create an outage start event (for ASN correlation)
     */
    public OutageEvent createOutageStartEvent(String prefix, String originAsn, String lastPath,
            Set<String> withdrawnBy) {
        return OutageEvent.builder()
                .prefix(prefix)
                .originAsn(Integer.parseInt(originAsn))
                .timestamp(Instant.now())
                .eventType("outage_start")
                .lastPath(lastPath)
                .withdrawnBy(withdrawnBy.toArray(new String[0]))
                .build();
    }

    /**
     * Create a recovery event (for ASN correlation)
     */
    public OutageEvent createRecoveryEvent(String prefix, String originAsn, String asPath, Set<String> withdrawnBy) {
        return OutageEvent.builder()
                .prefix(prefix)
                .originAsn(Integer.parseInt(originAsn))
                .timestamp(Instant.now())
                .eventType("recovery")
                .lastPath(asPath)
                .withdrawnBy(withdrawnBy.toArray(new String[0]))
                .build();
    }

    /**
     * Get recent outage events
     */
    public List<OutageEvent> getRecentOutages(int limit) {
        return outageEventRepository.findRecentOutages(limit);
    }

    /**
     * Get active (ongoing) outages
     */
    public List<OutageEvent> getActiveOutages() {
        return outageEventRepository.findActiveOutages();
    }

    /**
     * Get outage events for a specific prefix
     */
    public List<OutageEvent> getPrefixHistory(String prefix) {
        return outageEventRepository.findByPrefixOrderByTimestampDesc(prefix);
    }

    /**
     * Get outage events for a specific ASN
     */
    public List<OutageEvent> getAsnEvents(Integer asn) {
        return outageEventRepository.findByOriginAsnOrderByTimestampDesc(asn);
    }

    /**
     * Get outages for map visualization
     */
    public List<OutageEvent> getOutagesForMap(int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return outageEventRepository.findByTimestampAfterOrderByTimestampDesc(since);
    }

    /**
     * Get summary statistics
     */
    public OutageStats getSummaryStats() {
        long totalOutages = outageEventRepository.countByEventType("outage_start");
        long totalRecoveries = outageEventRepository.countByEventType("recovery");
        long activeOutages = outageEventRepository.countByEventTypeAndResolvedAtIsNull("outage_start");

        Instant dayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        long outagesLast24Hours = outageEventRepository.countByEventTypeAndTimestampAfter("outage_start", dayAgo);
        long outagesLast7Days = outageEventRepository.countByEventTypeAndTimestampAfter("outage_start", weekAgo);

        // Calculate unique ASNs and prefixes
        long uniqueAsns = outageEventRepository.countDistinctOriginAsn();
        long uniquePrefixes = outageEventRepository.countDistinctPrefix();

        // Calculate average duration (simplified)
        Double avgDuration = outageEventRepository.findAverageDurationMinutes();

        return OutageStats.builder()
                .totalOutages(totalOutages)
                .activeOutages(activeOutages)
                .totalRecoveries(totalRecoveries)
                .uniqueAsnsAffected(uniqueAsns)
                .uniquePrefixesAffected(uniquePrefixes)
                .averageOutageDurationMinutes(avgDuration != null ? avgDuration : 0.0)
                .outagesLast24Hours(outagesLast24Hours)
                .outagesLast7Days(outagesLast7Days)
                .build();
    }
}