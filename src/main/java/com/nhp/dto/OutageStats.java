package com.nhp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutageStats {
    private long totalOutages;
    private long activeOutages;
    private long totalRecoveries;
    private long uniqueAsnsAffected;
    private long uniquePrefixesAffected;
    private double averageOutageDurationMinutes;
    private long outagesLast24Hours;
    private long outagesLast7Days;
}