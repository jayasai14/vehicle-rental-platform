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
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number is already registered");
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            String roleName = request.getRole().toUpperCase().trim();
            if (roleName.contains("ADMIN")) {
                throw new BadRequestException("Registration as ADMIN is not permitted");
            }
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);

        Set<Role> roles = new HashSet<>();
        // All public registrations receive default ROLE_CUSTOMER
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_CUSTOMER", "Customer role")));
        roles.add(customerRole);
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
        if (oldRefreshToken == null || oldRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String hashed = hashToken(oldRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hashed)
                .or(() -> refreshTokenRepository.findByTokenHash(oldRefreshToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.isRevoked()) {
            // Revoked token reuse detected - immediately revoke entire family for security
            if (token.getFamilyId() != null) {
                refreshTokenRepository.revokeAllByFamilyId(token.getFamilyId());
            }
            throw new UnauthorizedException("Revoked refresh token reuse detected. Token family invalidated.");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new UnauthorizedException("Refresh token is expired");
        }

        User user = token.getUser();
        // Revoke old token for rotation
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        return createAuthResponse(user, token.getFamilyId());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String hashed = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(hashed)
                .or(() -> refreshTokenRepository.findByTokenHash(refreshToken))
                .ifPresent(token -> {
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
    public AuthDto.UserProfileDto updateProfile(Long userId, AuthDto.UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            String newPhone = request.getPhoneNumber().trim();
            if (!newPhone.equals(user.getPhoneNumber()) && userRepository.existsByPhoneNumber(newPhone)) {
                throw new BadRequestException("Phone number is already registered to another user");
            }
            user.setPhoneNumber(newPhone);
        }
        userRepository.save(user);

        if (request.getPreferredLanguage() != null && !request.getPreferredLanguage().isBlank()) {
            CustomerProfile customerProfile = customerProfileRepository.findByUserId(userId).orElseGet(() -> {
                CustomerProfile cp = new CustomerProfile();
                cp.setUser(user);
                return cp;
            });
            customerProfile.setPreferredLanguage(request.getPreferredLanguage().trim());
            customerProfileRepository.save(customerProfile);
        }

        return getProfile(userId);
    }

    @Transactional
    public AuthDto.DrivingLicenceDto submitDrivingLicence(Long userId, AuthDto.DrivingLicenceSubmitRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        DrivingLicence dl = drivingLicenceRepository.findByUserId(userId).orElseGet(DrivingLicence::new);
        dl.setUser(user);
        dl.setLicenceNumber(request.getLicenceNumber().trim());
        dl.setIssuingRto(request.getIssuingRto().trim());
        dl.setValidFrom(request.getValidFrom() != null ? request.getValidFrom() : java.time.LocalDate.now());
        dl.setValidUntil(request.getValidUntil());
        dl.setVehicleClassesAuthorized(request.getVehicleClassesAuthorized() != null ? request.getVehicleClassesAuthorized() : "LMV, MCWG");
        dl.setStorageObjectKey(request.getStorageObjectKey() != null ? request.getStorageObjectKey() : "default_dl.pdf");
        // Customer uploaded DL begins in PENDING until reviewed by Admin
        dl.setVerificationStatus(DrivingLicence.VerificationStatus.PENDING);
        dl.setVerifiedAt(null);
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
        return createAuthResponse(user, null);
    }

    private AuthDto.AuthResponse createAuthResponse(User user, String familyId) {
        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), roleNames);

        // Generate cryptographically random Refresh Token and hash it
        String rawTokenString = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawTokenString);
        String famId = (familyId != null && !familyId.isBlank()) ? familyId : UUID.randomUUID().toString().replace("-", "");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setFamilyId(famId);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        // Also build normalized UserProfileDto directly from in-memory user
        AuthDto.UserProfileDto profileDto = buildUserProfileDto(user);

        return AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawTokenString)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roles(roleNames)
                .user(profileDto)
                .build();
    }

    private AuthDto.UserProfileDto buildUserProfileDto(User user) {
        CustomerProfile customerProfile = customerProfileRepository.findByUserId(user.getId()).orElse(null);
        DrivingLicence dl = drivingLicenceRepository.findByUserId(user.getId()).orElse(null);
        Set<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                : Set.of();

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

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
