package com.nhp.services;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhp.dto.AsnInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AsnGeolocationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory cache for ASN information
    private final ConcurrentHashMap<Integer, AsnInfo> asnCache = new ConcurrentHashMap<>();

    // Cache TTL in Redis (24 hours)
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    // API endpoints for ASN information
    private static final String IPAPI_ENDPOINT = "http://ip-api.com/json/";
    private static final String ASNLOOKUP_ENDPOINT = "https://api.asnlookup.com/v1/asn/";
    private static final String BGPVIEW_ENDPOINT = "https://api.bgpview.io/asn/";

    /**
     * Get ASN information including geolocation
     */
    public AsnInfo getAsnInfo(Integer asn) {
        if (asn == null || asn <= 0) {
            return AsnInfo.builder()
                    .asn(asn)
                    .name("Unknown")
                    .country("Unknown")
                    .latitude(0.0)
                    .longitude(0.0)
                    .build();
        }

        // Check in-memory cache first
        AsnInfo cached = asnCache.get(asn);
        if (cached != null) {
            return cached;
        }

        // Check Redis cache
        String redisKey = "asn:" + asn;
        AsnInfo redisCached = (AsnInfo) redisTemplate.opsForValue().get(redisKey);
        if (redisCached != null) {
            asnCache.put(asn, redisCached);
            return redisCached;
        }

        // Fetch from external API
        AsnInfo asnInfo = fetchAsnInfoFromApi(asn);
        if (asnInfo != null) {
            // Cache in Redis and memory
            redisTemplate.opsForValue().set(redisKey, asnInfo, CACHE_TTL);
            asnCache.put(asn, asnInfo);
        } else {
            // Create fallback info
            asnInfo = createFallbackAsnInfo(asn);
            redisTemplate.opsForValue().set(redisKey, asnInfo, CACHE_TTL);
            asnCache.put(asn, asnInfo);
        }

        return asnInfo;
    }

    /**
     * Fetch ASN information from external APIs
     */
    private AsnInfo fetchAsnInfoFromApi(Integer asn) {
        // Try multiple APIs for redundancy
        AsnInfo info = tryBgpViewApi(asn);
        if (info != null && info.getLatitude() != 0.0) {
            return info;
        }

        info = tryAsnLookupApi(asn);
        if (info != null && info.getLatitude() != 0.0) {
            return info;
        }

        // If no geolocation found, try to get at least ASN name
        return tryGetAsnNameOnly(asn);
    }

    /**
     * Try BGPView API for ASN information
     */
    private AsnInfo tryBgpViewApi(Integer asn) {
        try {
            String url = BGPVIEW_ENDPOINT + asn;
            String response = restTemplate.getForObject(url, String.class);

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                if (root.has("data") && root.get("data").has("asn")) {
                    JsonNode asnData = root.get("data").get("asn");

                    String name = asnData.path("name").asText("Unknown");
                    String country = asnData.path("country_code").asText("Unknown");

                    // Try to get location from prefixes
                    double lat = 0.0, lng = 0.0;
                    if (asnData.has("prefixes") && asnData.get("prefixes").isArray()) {
                        for (JsonNode prefix : asnData.get("prefixes")) {
                            if (prefix.has("latitude") && prefix.has("longitude")) {
                                lat = prefix.get("latitude").asDouble();
                                lng = prefix.get("longitude").asDouble();
                                break;
                            }
                        }
                    }

                    return AsnInfo.builder()
                            .asn(asn)
                            .name(name)
                            .country(country)
                            .latitude(lat)
                            .longitude(lng)
                            .build();
                }
            }
        } catch (Exception e) {
            log.debug("BGPView API failed for ASN {}: {}", asn, e.getMessage());
        }
        return null;
    }

    /**
     * Try ASNLookup API for ASN information
     */
    private AsnInfo tryAsnLookupApi(Integer asn) {
        try {
            String url = ASNLOOKUP_ENDPOINT + asn;
            String response = restTemplate.getForObject(url, String.class);

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                if (root.has("data")) {
                    JsonNode data = root.get("data");

                    String name = data.path("name").asText("Unknown");
                    String country = data.path("country").asText("Unknown");
                    double lat = data.path("latitude").asDouble(0.0);
                    double lng = data.path("longitude").asDouble(0.0);

                    return AsnInfo.builder()
                            .asn(asn)
                            .name(name)
                            .country(country)
                            .latitude(lat)
                            .longitude(lng)
                            .build();
                }
            }
        } catch (Exception e) {
            log.debug("ASNLookup API failed for ASN {}: {}", asn, e.getMessage());
        }
        return null;
    }

    /**
     * Try to get just ASN name from various sources
     */
    private AsnInfo tryGetAsnNameOnly(Integer asn) {
        // Try to get ASN name from whois or other sources
        String name = getAsnNameFromWhois(asn);

        return AsnInfo.builder()
                .asn(asn)
                .name(name != null ? name : "AS" + asn)
                .country("Unknown")
                .latitude(0.0)
                .longitude(0.0)
                .build();
    }

    /**
     * Get ASN name from WHOIS (simplified implementation)
     */
    private String getAsnNameFromWhois(Integer asn) {
        // This is a simplified implementation
        // In a real system, you might use a WHOIS library or service

        // Common ASN names mapping
        switch (asn) {
            case 15169:
                return "Google LLC";
            case 32934:
                return "Facebook, Inc.";
            case 14618:
                return "Amazon.com, Inc.";
            case 8075:
                return "Microsoft Corporation";
            case 174:
                return "Cogent Communications";
            case 3356:
                return "Level 3 Communications";
            case 701:
                return "Verizon Business";
            case 6453:
                return "Tata Communications";
            case 1299:
                return "Telia Company AB";
            case 3257:
                return "GTT Communications";
            default:
                return "AS" + asn;
        }
    }

    /**
     * Create fallback ASN info when APIs fail
     */
    private AsnInfo createFallbackAsnInfo(Integer asn) {
        return AsnInfo.builder()
                .asn(asn)
                .name("AS" + asn)
                .country("Unknown")
                .latitude(0.0)
                .longitude(0.0)
                .build();
    }

    /**
     * Get coordinates for ASN (with fallback to random if no geolocation)
     */
    public double[] getAsnCoordinates(Integer asn) {
        AsnInfo info = getAsnInfo(asn);

        if (info.getLatitude() != 0.0 && info.getLongitude() != 0.0) {
            return new double[] { info.getLatitude(), info.getLongitude() };
        }

        // Fallback to pseudo-random coordinates based on ASN
        return new double[] {
                (asn % 180) - 90, // Latitude between -90 and 90
                (asn % 360) - 180 // Longitude between -180 and 180
        };
    }

    /**
     * Clear cache for an ASN
     */
    public void clearAsnCache(Integer asn) {
        asnCache.remove(asn);
        String redisKey = "asn:" + asn;
        redisTemplate.delete(redisKey);
    }

    /**
     * Clear all ASN caches
     */
    public void clearAllCaches() {
        asnCache.clear();
        // Note: Redis cache will expire automatically
    }
}