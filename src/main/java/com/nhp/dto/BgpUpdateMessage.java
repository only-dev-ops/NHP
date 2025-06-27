package com.nhp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BgpUpdateMessage {
    private String prefix;
    private String collector;
    private String originAsn;
    private String asPath;
    private boolean announcement;
    private boolean withdrawal;
}