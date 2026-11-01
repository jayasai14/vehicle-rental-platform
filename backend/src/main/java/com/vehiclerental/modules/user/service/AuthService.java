package com.vehiclerental.modules.user.service;

import com.vehiclerental.common.exception.BadRequestException;
import com.vehiclerental.common.exception.ResourceNotFoundException;
import com.vehiclerental.common.exception.UnauthorizedException;
import com.vehiclerental.config.JwtTokenProvider;
import com.vehiclerental.modules.user.dto.AuthDto;
import com.vehiclerental.modules.user.entity.*;
import com.vehiclerental.modules.user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final DrivingLicenceRepository drivingLicenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.refresh-token-expiration-days:7}")
    private int refreshTokenExpirationDays;

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number is already registered");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);

        Set<Role> roles = new HashSet<>();
        // Assign default customer role
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_CUSTOMER", "Customer role")));
        roles.add(customerRole);

        // Optional role requested
        if (request.getRole() != null && !request.getRole().isBlank()) {
            String roleName = request.getRole().toUpperCase().trim();
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }
            if ("ROLE_RENTAL_PARTNER".equals(roleName) || "ROLE_VEHICLE_CONTRIBUTOR".equals(roleName)) {
                String finalRoleName = roleName;
                Role extraRole = roleRepository.findByName(roleName)
                        .orElseGet(() -> roleRepository.save(new Role(finalRoleName, "Self-registered role")));
                roles.add(extraRole);
            }
        }
        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // Auto-create customer profile
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(savedUser);
        profile.setPreferredLanguage("English");
        customerProfileRepository.save(profile);

        return createAuthResponse(savedUser);
    }

    @Transactional
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is disabled. Please contact support.");
        }

        return createAuthResponse(user);
    }

    @Transactional
    public AuthDto.AuthResponse refreshToken(String oldRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(oldRefreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.isRevoked() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        User user = token.getUser();
        // Revoke old token for rotation
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        return createAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenHash(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional(readOnly = true)
    public AuthDto.UserProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        CustomerProfile customerProfile = customerProfileRepository.findByUserId(userId).orElse(null);
        DrivingLicence dl = drivingLicenceRepository.findByUserId(userId).orElse(null);

        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

        return AuthDto.UserProfileDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roles(roleNames)
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .active(user.isActive())
                .customerProfile(customerProfile != null ? AuthDto.CustomerProfileDto.builder()
                        .preferredLanguage(customerProfile.getPreferredLanguage())
                        .totalCompletedTrips(customerProfile.getTotalCompletedTrips())
                        .suspended(customerProfile.isSuspended())
                        .build() : null)
                .drivingLicence(dl != null ? AuthDto.DrivingLicenceDto.builder()
                        .licenceNumber(dl.getLicenceNumber())
                        .issuingRto(dl.getIssuingRto())
                        .validFrom(dl.getValidFrom())
                        .validUntil(dl.getValidUntil())
                        .vehicleClassesAuthorized(dl.getVehicleClassesAuthorized())
                        .verificationStatus(dl.getVerificationStatus().name())
                        .rejectionReason(dl.getRejectionReason())
                        .build() : null)
                .build();
    }

    @Transactional
    public AuthDto.DrivingLicenceDto submitDrivingLicence(Long userId, AuthDto.DrivingLicenceSubmitRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        DrivingLicence dl = drivingLicenceRepository.findByUserId(userId).orElseGet(DrivingLicence::new);
        dl.setUser(user);
        dl.setLicenceNumber(request.getLicenceNumber());
        dl.setIssuingRto(request.getIssuingRto());
        dl.setValidFrom(request.getValidFrom() != null ? request.getValidFrom() : java.time.LocalDate.now());
        dl.setValidUntil(request.getValidUntil() != null ? request.getValidUntil() : java.time.LocalDate.now().plusYears(10));
        dl.setVehicleClassesAuthorized(request.getVehicleClassesAuthorized() != null ? request.getVehicleClassesAuthorized() : "LMV, MCWG");
        dl.setStorageObjectKey(request.getStorageObjectKey() != null ? request.getStorageObjectKey() : "default_dl.pdf");
        dl.setVerificationStatus(DrivingLicence.VerificationStatus.VERIFIED); // In MVP, automatically mark verified on customer upload or manual admin
        dl.setVerifiedAt(LocalDateTime.now());
        drivingLicenceRepository.save(dl);

        return AuthDto.DrivingLicenceDto.builder()
                .licenceNumber(dl.getLicenceNumber())
                .issuingRto(dl.getIssuingRto())
                .validFrom(dl.getValidFrom())
                .validUntil(dl.getValidUntil())
                .vehicleClassesAuthorized(dl.getVehicleClassesAuthorized())
                .verificationStatus(dl.getVerificationStatus().name())
                .build();
    }

    private AuthDto.AuthResponse createAuthResponse(User user) {
        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), roleNames);

        // Generate Refresh Token
        String tokenString = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(tokenString);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(tokenString)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roles(roleNames)
                .build();
    }
}
