package com.rentacaresv.catalog.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para VehicleBrand
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleBrandDTO {
    private Long id;
    private String name;
    private String logoUrl;
    private Boolean active;
    private Integer modelCount;
}
