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
@Table(name = "asn_outages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsnOutage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asn", nullable = false)
    private Integer asn;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "duration")
    private String duration;

    @Column(name = "prefixes", columnDefinition = "TEXT[]")
    private String[] prefixes;

    @Column(name = "severity")
    private Integer severity;

    @Column(name = "country")
    private String country;

    @Column(name = "created_at")
    private Instant createdAt;
}