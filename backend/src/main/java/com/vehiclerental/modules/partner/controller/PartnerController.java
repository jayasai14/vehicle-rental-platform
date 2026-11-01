package com.vehiclerental.modules.partner.controller;

import com.vehiclerental.common.response.ApiResponse;
import com.vehiclerental.common.security.SecurityUtils;
import com.vehiclerental.modules.partner.dto.PartnerDto;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.partner.service.PartnerService;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;
    private final UserRepository userRepository;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<PartnerDto.PartnerProfileDto>> apply(
            @Valid @RequestBody PartnerDto.PartnerApplicationRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PartnerDto.PartnerProfileDto response = partnerService.applyForPartner(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Partner application submitted successfully"));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<PartnerDto.PartnerProfileDto>> getProfile() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PartnerDto.PartnerProfileDto profile = partnerService.getPartnerProfile(user.getId());
        return ResponseEntity.ok(ApiResponse.success(profile, "Partner profile retrieved"));
    }

    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<PartnerDto.PartnerDocumentDto>> uploadDocument(
            @Valid @RequestBody PartnerDto.UploadDocumentRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PartnerDto.PartnerDocumentDto doc = partnerService.uploadDocument(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(doc, "Document uploaded successfully"));
    }

    @PostMapping("/payment-profile")
    @PreAuthorize("hasRole('RENTAL_PARTNER')")
    public ResponseEntity<ApiResponse<PartnerDto.PartnerPaymentProfileDto>> configurePaymentProfile(
            @Valid @RequestBody PartnerDto.ConfigurePaymentProfileRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PartnerDto.PartnerPaymentProfileDto ppp = partnerService.configurePaymentProfile(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(ppp, "Payment profile updated successfully"));
    }

    @GetMapping("/{partnerId}")
    public ResponseEntity<ApiResponse<PartnerDto.PartnerProfileDto>> getPartnerById(@PathVariable Long partnerId) {
        return ResponseEntity.ok(ApiResponse.success(partnerService.getPartnerProfileById(partnerId), "Partner details retrieved"));
    }
}
