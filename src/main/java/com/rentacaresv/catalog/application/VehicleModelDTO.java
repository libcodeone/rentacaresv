package com.rentacaresv.catalog.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para VehicleModel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleModelDTO {
    private Long id;
    private Long brandId;
    private String brandName;
    private String name;
    private String fullName;
    private String vehicleType;
    private String vehicleTypeLabel;
    private Integer yearStart;
    private Integer yearEnd;
    private Boolean active;
}
