package com.vehiclerental.modules.user.controller;

import com.vehiclerental.common.response.ApiResponse;
import com.vehiclerental.common.security.SecurityUtils;
import com.vehiclerental.modules.user.dto.AuthDto;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import com.vehiclerental.modules.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        AuthDto.AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> refresh(@Valid @RequestBody AuthDto.TokenRefreshRequest request) {
        AuthDto.AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody AuthDto.TokenRefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping({"/me", "/profile"})
    public ResponseEntity<ApiResponse<AuthDto.UserProfileDto>> getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        AuthDto.UserProfileDto profile = authService.getProfile(user.getId());
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved successfully"));
    }

    @PutMapping({"/me", "/profile"})
    public ResponseEntity<ApiResponse<AuthDto.UserProfileDto>> updateProfile(
            @Valid @RequestBody AuthDto.UpdateProfileRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        AuthDto.UserProfileDto updated = authService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Profile updated successfully"));
    }

    @PostMapping({"/driving-licence", "/kyc"})
    public ResponseEntity<ApiResponse<AuthDto.DrivingLicenceDto>> submitDrivingLicence(
            @Valid @RequestBody AuthDto.DrivingLicenceSubmitRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        AuthDto.DrivingLicenceDto dl = authService.submitDrivingLicence(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(dl, "Driving licence submitted successfully"));
    }
}
