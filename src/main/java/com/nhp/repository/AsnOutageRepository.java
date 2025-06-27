package com.nhp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhp.dto.AsnOutage;

@Repository
public interface AsnOutageRepository extends JpaRepository<AsnOutage, Long> {

    List<AsnOutage> findByAsnOrderByStartTimeDesc(Integer asn);

    @Query("SELECT a FROM AsnOutage a WHERE a.endTime IS NULL ORDER BY a.startTime DESC")
    List<AsnOutage> findActiveAsnOutages();

    @Query("SELECT a FROM AsnOutage a WHERE a.startTime >= :since ORDER BY a.startTime DESC")
    List<AsnOutage> findRecentAsnOutages(@Param("since") java.time.Instant since);
}