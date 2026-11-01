package com.vehiclerental.modules.vehicle.controller;

import com.vehiclerental.common.response.ApiResponse;
import com.vehiclerental.common.response.PagedResponse;
import com.vehiclerental.common.security.SecurityUtils;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import com.vehiclerental.modules.vehicle.dto.VehicleDto;
import com.vehiclerental.modules.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final UserRepository userRepository;

    @GetMapping({"/vehicles", "/vehicles/search"})
    public ResponseEntity<ApiResponse<PagedResponse<VehicleDto.VehicleCardDto>>> searchVehicles(
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long hubId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) BigDecimal maxDailyRate,
            @RequestParam(required = false) String transmission,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VehicleDto.VehicleCardDto> result = vehicleService.searchVehicles(
                cityId, hubId, category, startTime, endTime, maxDailyRate, transmission, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(result), "Vehicles retrieved"));
    }

    @GetMapping("/vehicles/categories")
    public ResponseEntity<ApiResponse<List<String>>> getVehicleCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                List.of("SUV", "SEDAN", "HATCHBACK", "EV", "LUXURY", "COMMERCIAL"),
                "Vehicle categories retrieved"
        ));
    }

    @GetMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<VehicleDto.VehicleDetailDto>> getVehicleDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.getVehicleDetail(id), "Vehicle details retrieved"));
    }

    @GetMapping("/vehicles/{id}/availability")
    public ResponseEntity<ApiResponse<Boolean>> checkAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        boolean available = vehicleService.isVehicleAvailable(id, startTime, endTime, null, null);
        return ResponseEntity.ok(ApiResponse.success(available, available ? "Vehicle is available" : "Vehicle is unavailable"));
    }

    @GetMapping("/partners/fleet")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<VehicleDto.VehicleCardDto>>> getPartnerFleet() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(vehicleService.getPartnerFleet(user.getId()), "Fleet retrieved"));
    }

    @PostMapping("/partners/vehicles")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VehicleDto.VehicleDetailDto>> createVehicle(
            @Valid @RequestBody VehicleDto.VehicleCreateRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        VehicleDto.VehicleDetailDto created = vehicleService.createVehicle(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(created, "Vehicle added and published successfully"));
    }

    @PostMapping("/partners/vehicles/{id}/maintenance-blocks")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VehicleDto.MaintenanceBlockDto>> createMaintenanceBlock(
            @PathVariable Long id,
            @Valid @RequestBody VehicleDto.CreateMaintenanceBlockRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        VehicleDto.MaintenanceBlockDto block = vehicleService.createMaintenanceBlock(user.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(block, "Maintenance block created"));
    }
}
