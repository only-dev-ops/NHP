package com.nhp.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class PrefixState implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<String> visibleCollectors = new HashSet<>();
    private Set<String> withdrawnBy = new HashSet<>();
    private String originAsn;
    private String lastPath;
    private Instant lastSeen;
    private boolean withdrawn = false;
}