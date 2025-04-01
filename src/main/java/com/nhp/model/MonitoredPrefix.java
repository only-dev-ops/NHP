package com.nhp.model;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "monitored_prefixes")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MonitoredPrefix {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String prefix; // CIDR string

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
