package com.nhp.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsnInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer asn;
    private String name;
    private String country;
    private Double latitude;
    private Double longitude;
    private String description;
    private String website;
}