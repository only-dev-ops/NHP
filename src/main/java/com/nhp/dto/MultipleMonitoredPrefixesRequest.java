package com.nhp.dto;

import java.util.List;

import lombok.Data;

@Data
public class MultipleMonitoredPrefixesRequest {
    private List<String> prefixes;
}
