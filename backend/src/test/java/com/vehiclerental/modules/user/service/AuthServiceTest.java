package com.vehiclerental.modules.user.service;

import com.vehiclerental.common.exception.BadRequestException;
import com.vehiclerental.common.exception.UnauthorizedException;
import com.vehiclerental.config.JwtTokenProvider;
import com.vehiclerental.modules.user.dto.AuthDto;
import com.vehiclerental.modules.user.entity.CustomerProfile;
import com.vehiclerental.modules.user.entity.RefreshToken;
import com.vehiclerental.modules.user.entity.Role;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private CustomerProfileRepository customerProfileRepository;
    @Mock
    private DrivingLicenceRepository drivingLicenceRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationDays", 7);
    }

    @Test
    @DisplayName("Should successfully register a new customer user")
    void testRegisterSuccess() {
        AuthDto.RegisterRequest req = AuthDto.RegisterRequest.builder()
                .fullName("Test Customer")
                .email("test@customer.com")
                .phoneNumber("+919999988888")
                .password("Password@123")
                .build();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(new Role("ROLE_CUSTOMER", "Customer")));

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setEmail("test@customer.com");
        savedUser.setFullName("Test Customer");
        savedUser.setRoles(Set.of(new Role("ROLE_CUSTOMER", "Customer")));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(any(), any())).thenReturn("jwt.token.here");

        AuthDto.AuthResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals("test@customer.com", response.getEmail());
        assertEquals(10L, response.getUserId());
        assertEquals("jwt.token.here", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(customerProfileRepository, times(1)).save(any(CustomerProfile.class));
    }

    @Test
    @DisplayName("Should reject registration if email is already taken")
    void testRegisterDuplicateEmail() {
        AuthDto.RegisterRequest req = AuthDto.RegisterRequest.builder()
                .fullName("Test Customer")
                .email("existing@customer.com")
                .phoneNumber("+919999988888")
                .password("Password@123")
                .build();

        when(userRepository.existsByEmail("existing@customer.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should authenticate and return JWT on valid credentials")
    void testLoginSuccess() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest("test@customer.com", "Password@123");

        User user = new User();
        user.setId(5L);
        user.setEmail("test@customer.com");
        user.setPasswordHash("hashedSecret");
        user.setActive(true);
        user.setRoles(Set.of(new Role("ROLE_CUSTOMER", "Customer")));

        when(userRepository.findByEmail("test@customer.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "hashedSecret")).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any())).thenReturn("mock.jwt.token");

        AuthDto.AuthResponse resp = authService.login(req);

        assertNotNull(resp);
        assertEquals(5L, resp.getUserId());
        assertEquals("mock.jwt.token", resp.getAccessToken());
    }

    @Test
    @DisplayName("Should rotate refresh token on refresh request")
    void testRefreshTokenRotation() {
        String oldToken = "old-refresh-token-uuid";
        User user = new User();
        user.setId(8L);
        user.setEmail("user@test.com");
        user.setRoles(Set.of(new Role("ROLE_CUSTOMER", "Customer")));

        RefreshToken rt = new RefreshToken();
        rt.setToken(oldToken);
        rt.setUser(user);
        rt.setExpiresAt(LocalDateTime.now().plusDays(5));
        rt.setRevoked(false);

        when(refreshTokenRepository.findByToken(oldToken)).thenReturn(Optional.of(rt));
        when(jwtTokenProvider.generateToken(any(), any())).thenReturn("new.access.token");

        AuthDto.AuthResponse response = authService.refreshToken(oldToken);

        assertNotNull(response);
        assertEquals("new.access.token", response.getAccessToken());
        assertTrue(rt.isRevoked(), "Old refresh token must be revoked for rotation security");
    }
}
