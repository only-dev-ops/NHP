package com.nhp.dto;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outage_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prefix", nullable = false)
    private String prefix;

    @Column(name = "origin_asn", nullable = false)
    private Integer originAsn;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "last_path")
    private String lastPath;

    @Column(name = "withdrawn_by", columnDefinition = "TEXT[]")
    private String[] withdrawnBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "duration")
    private String duration;

    @Column(name = "created_at")
    private Instant createdAt;
}