package com.vehiclerental.modules.location.controller;

import com.vehiclerental.common.response.ApiResponse;
import com.vehiclerental.common.security.SecurityUtils;
import com.vehiclerental.modules.location.dto.LocationDto;
import com.vehiclerental.modules.location.service.LocationService;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final UserRepository userRepository;

    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<LocationDto.CityDto>>> getCities() {
        return ResponseEntity.ok(ApiResponse.success(locationService.getAllCities(), "Cities retrieved"));
    }

    @GetMapping("/hubs")
    public ResponseEntity<ApiResponse<List<LocationDto.PartnerHubDto>>> getHubs(
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long partnerId
    ) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getHubs(cityId, partnerId), "Hubs retrieved"));
    }

    @GetMapping("/pickups")
    public ResponseEntity<ApiResponse<List<LocationDto.PickupLocationDto>>> getPickupLocations(
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) Long hubId
    ) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getPickupLocations(partnerId, hubId), "Pickup locations retrieved"));
    }

    @PostMapping("/hubs")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LocationDto.PartnerHubDto>> createHub(
            @Valid @RequestBody LocationDto.CreateHubRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(locationService.createHub(user.getId(), request), "Hub created successfully"));
    }

    @PostMapping("/pickups")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LocationDto.PickupLocationDto>> createPickup(
            @Valid @RequestBody LocationDto.CreatePickupLocationRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(locationService.createPickupLocation(user.getId(), request), "Pickup location created successfully"));
    }
}
