package com.vehiclerental.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class AuthDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        private String email;

        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private String role; // "CUSTOMER", "RENTAL_PARTNER", "VEHICLE_CONTRIBUTOR"
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private Long userId;
        private String fullName;
        private String email;
        private String phoneNumber;
        private Set<String> roles;
        private UserProfileDto user;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest {
        private String fullName;
        private String phoneNumber;
        private String preferredLanguage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenRefreshRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserProfileDto {
        private Long id;
        private String fullName;
        private String email;
        private String phoneNumber;
        private Set<String> roles;
        private boolean emailVerified;
        private boolean phoneVerified;
        private boolean active;
        private CustomerProfileDto customerProfile;
        private DrivingLicenceDto drivingLicence;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerProfileDto {
        private String preferredLanguage;
        private Integer totalCompletedTrips;
        private boolean suspended;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DrivingLicenceDto {
        private String licenceNumber;
        private String issuingRto;
        private LocalDate validFrom;
        private LocalDate validUntil;
        private String vehicleClassesAuthorized;
        private String verificationStatus;
        private String rejectionReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DrivingLicenceSubmitRequest {
        @NotBlank
        private String licenceNumber;
        @NotBlank
        private String issuingRto;
        private LocalDate validFrom;
        private LocalDate validUntil;
        private String vehicleClassesAuthorized;
        private String storageObjectKey;
    }
}
