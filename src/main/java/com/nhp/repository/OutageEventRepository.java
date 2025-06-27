package com.nhp.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhp.dto.OutageEvent;

@Repository
public interface OutageEventRepository extends JpaRepository<OutageEvent, Long> {

    List<OutageEvent> findByPrefixOrderByTimestampDesc(String prefix);

    List<OutageEvent> findByOriginAsnOrderByTimestampDesc(Integer originAsn);

    List<OutageEvent> findByTimestampAfterOrderByTimestampDesc(Instant since);

    long countByEventType(String eventType);

    long countByEventTypeAndResolvedAtIsNull(String eventType);

    long countByEventTypeAndTimestampAfter(String eventType, Instant since);

    @Query("SELECT COUNT(DISTINCT o.originAsn) FROM OutageEvent o")
    long countDistinctOriginAsn();

    @Query("SELECT COUNT(DISTINCT o.prefix) FROM OutageEvent o")
    long countDistinctPrefix();

    @Query("SELECT COUNT(DISTINCT o.prefix) FROM OutageEvent o WHERE o.originAsn = :asn")
    long countDistinctPrefixByOriginAsn(@Param("asn") Integer asn);

    @Query("SELECT AVG(EXTRACT(EPOCH FROM o.duration)/60) FROM OutageEvent o WHERE o.duration IS NOT NULL")
    Double findAverageDurationMinutes();

    @Query("SELECT o FROM OutageEvent o ORDER BY o.timestamp DESC LIMIT :limit")
    List<OutageEvent> findRecentOutages(@Param("limit") int limit);

    @Query("SELECT o FROM OutageEvent o WHERE o.eventType = 'outage_start' AND o.resolvedAt IS NULL ORDER BY o.timestamp DESC")
    List<OutageEvent> findActiveOutages();
}