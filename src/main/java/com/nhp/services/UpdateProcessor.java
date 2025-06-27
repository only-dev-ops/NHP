package com.nhp.services;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhp.dto.BgpUpdateMessage;
import com.nhp.dto.PrefixState;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UpdateProcessor {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private OutageEventService outageEventService;

    @Autowired
    private AsnOutageService asnOutageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory cache for active prefix states to reduce Redis calls
    private final ConcurrentHashMap<String, PrefixState> prefixStateCache = new ConcurrentHashMap<>();

    // TTL for prefix state in Redis (24 hours)
    private static final Duration PREFIX_TTL = Duration.ofHours(24);

    /**
     * Process incoming BGP UPDATE message
     */
    public void processBgpUpdate(String message) {
        try {
            BgpUpdateMessage bgpUpdate = parseBgpMessage(message);
            if (bgpUpdate == null) {
                return;
            }

            String prefix = bgpUpdate.getPrefix();
            String collector = bgpUpdate.getCollector();
            String originAsn = bgpUpdate.getOriginAsn();
            String asPath = bgpUpdate.getAsPath();

            if (bgpUpdate.isAnnouncement()) {
                processAnnouncement(prefix, collector, originAsn, asPath);
            } else if (bgpUpdate.isWithdrawal()) {
                processWithdrawal(prefix, collector, originAsn, asPath);
            }

            metricsService.incrementBgpMessagesProcessed();

        } catch (Exception e) {
            log.error("Error processing BGP update: {}", message, e);
            metricsService.incrementBgpProcessingErrors();
        }
    }

    /**
     * Process BGP announcement - prefix is being advertised
     */
    private void processAnnouncement(String prefix, String collector, String originAsn, String asPath) {
        String redisKey = "prefix:" + prefix;

        PrefixState state = getOrCreatePrefixState(redisKey);
        boolean wasWithdrawn = state.isWithdrawn();

        // Add collector to visibility set
        state.getVisibleCollectors().add(collector);
        state.setOriginAsn(originAsn);
        state.setLastPath(asPath);
        state.setLastSeen(Instant.now());
        state.setWithdrawn(false);

        // Update Redis
        savePrefixState(redisKey, state);

        log.debug("Announcement: prefix={}, collector={}, origin_asn={}", prefix, collector, originAsn);

        // If prefix was previously withdrawn, this is a recovery
        if (wasWithdrawn) {
            log.info("RECOVERY detected: prefix={}, origin_asn={}", prefix, originAsn);
            outageEventService.recordRecovery(prefix, originAsn, asPath, state.getWithdrawnBy());
            metricsService.incrementPrefixRecoveries();

            // Process recovery for ASN correlation
            asnOutageService.processRecoveryEvent(
                    outageEventService.createRecoveryEvent(prefix, originAsn, asPath, state.getWithdrawnBy()));
        }
    }

    /**
     * Process BGP withdrawal - prefix is being withdrawn
     */
    private void processWithdrawal(String prefix, String collector, String originAsn, String asPath) {
        String redisKey = "prefix:" + prefix;

        PrefixState state = getOrCreatePrefixState(redisKey);

        // Remove collector from visibility set
        state.getVisibleCollectors().remove(collector);
        state.getWithdrawnBy().add(collector);
        state.setLastSeen(Instant.now());

        // Check if prefix is now globally withdrawn
        if (state.getVisibleCollectors().isEmpty()) {
            state.setWithdrawn(true);
            log.info("OUTAGE detected: prefix={}, origin_asn={}, withdrawn_by={}",
                    prefix, originAsn, state.getWithdrawnBy());

            // Record outage event
            outageEventService.recordOutageStart(prefix, originAsn, state.getLastPath(), state.getWithdrawnBy());
            metricsService.incrementPrefixOutages();

            // Process outage for ASN correlation
            asnOutageService.processOutageEvent(
                    outageEventService.createOutageStartEvent(prefix, originAsn, state.getLastPath(),
                            state.getWithdrawnBy()));
        }

        // Update Redis
        savePrefixState(redisKey, state);

        log.debug("Withdrawal: prefix={}, collector={}, origin_asn={}", prefix, collector, originAsn);
    }

    /**
     * Get or create prefix state from Redis/cache
     */
    private PrefixState getOrCreatePrefixState(String redisKey) {
        // Check cache first
        PrefixState cached = prefixStateCache.get(redisKey);
        if (cached != null) {
            return cached;
        }

        // Try to get from Redis
        PrefixState state = (PrefixState) redisTemplate.opsForValue().get(redisKey);
        if (state == null) {
            state = new PrefixState();
        }

        // Cache the state
        prefixStateCache.put(redisKey, state);
        return state;
    }

    /**
     * Save prefix state to Redis and cache
     */
    private void savePrefixState(String redisKey, PrefixState state) {
        // Update cache
        prefixStateCache.put(redisKey, state);

        // Save to Redis with TTL
        redisTemplate.opsForValue().set(redisKey, state, PREFIX_TTL);
    }

    /**
     * Parse BGP message from JSON string
     */
    private BgpUpdateMessage parseBgpMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            // Check if this is a valid BGP update message
            if (!root.has("type") || !"ris_message".equals(root.path("type").asText())) {
                return null;
            }

            JsonNode data = root.path("data");
            if (data.isMissingNode()) {
                return null;
            }

            String prefix = data.path("prefix").asText();
            String collector = data.path("peer").asText();
            String originAsn = extractOriginAsn(data.path("path").asText());
            String asPath = data.path("path").asText();

            // Determine if this is an announcement or withdrawal
            boolean isAnnouncement = data.has("announcements") && !data.path("announcements").isEmpty();
            boolean isWithdrawal = data.has("withdrawals") && !data.path("withdrawals").isEmpty();

            return BgpUpdateMessage.builder()
                    .prefix(prefix)
                    .collector(collector)
                    .originAsn(originAsn)
                    .asPath(asPath)
                    .announcement(isAnnouncement)
                    .withdrawal(isWithdrawal)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse BGP message: {}", message, e);
            return null;
        }
    }

    /**
     * Extract origin ASN from AS path
     */
    private String extractOriginAsn(String asPath) {
        if (asPath == null || asPath.trim().isEmpty()) {
            return "0";
        }

        String[] pathElements = asPath.trim().split("\\s+");
        if (pathElements.length > 0) {
            return pathElements[pathElements.length - 1];
        }

        return "0";
    }

    /**
     * Get current prefix state (for monitoring/debugging)
     */
    public PrefixState getPrefixState(String prefix) {
        String redisKey = "prefix:" + prefix;
        return getOrCreatePrefixState(redisKey);
    }

    /**
     * Clear prefix state (for testing/cleanup)
     */
    public void clearPrefixState(String prefix) {
        String redisKey = "prefix:" + prefix;
        prefixStateCache.remove(redisKey);
        redisTemplate.delete(redisKey);
    }
}