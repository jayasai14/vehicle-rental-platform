package com.vehiclerental.modules.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

public class LocationDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CityDto {
        private Long id;
        private String name;
        private String state;
        private String country;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartnerHubDto {
        private Long id;
        private Long partnerId;
        private String name;
        private Long cityId;
        private String cityName;
        private String contactPhone;
        private String fullAddress;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PickupLocationDto {
        private Long id;
        private Long partnerId;
        private Long partnerHubId;
        private String locationName;
        private String locationType;
        private BigDecimal doorstepDeliverySurcharge;
        private String fullAddress;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateHubRequest {
        @NotBlank
        private String name;
        @NotNull
        private Long cityId;
        @NotBlank
        private String contactPhone;
        @NotBlank
        private String addressLine1;
        private String addressLine2;
        @NotBlank
        private String postalCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatePickupLocationRequest {
        @NotBlank
        private String locationName;
        private Long partnerHubId;
        @NotBlank
        private String locationType;
        private BigDecimal doorstepDeliverySurcharge;
        @NotBlank
        private String addressLine1;
        private String addressLine2;
        @NotNull
        private Long cityId;
        @NotBlank
        private String postalCode;
    }
}
