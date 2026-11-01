package com.vehiclerental.modules.contributor.controller;

import com.vehiclerental.common.response.ApiResponse;
import com.vehiclerental.common.security.SecurityUtils;
import com.vehiclerental.modules.contributor.dto.ContributorDto;
import com.vehiclerental.modules.contributor.service.ContributorService;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContributorController {

    private final ContributorService contributorService;
    private final UserRepository userRepository;

    @PostMapping("/contributors/profile")
    public ResponseEntity<ApiResponse<ContributorDto.ContributorProfileDto>> createProfile(
            @Valid @RequestBody ContributorDto.ContributorProfileRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        ContributorDto.ContributorProfileDto profile = contributorService.createOrUpdateProfile(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Contributor profile saved successfully"));
    }

    @GetMapping("/contributors/profile")
    public ResponseEntity<ApiResponse<ContributorDto.ContributorProfileDto>> getProfile() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        ContributorDto.ContributorProfileDto profile = contributorService.getProfile(user.getId());
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved"));
    }

    @PostMapping("/contributors/applications")
    public ResponseEntity<ApiResponse<ContributorDto.ContributorApplicationDto>> submitApplication(
            @Valid @RequestBody ContributorDto.VehicleApplicationRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        ContributorDto.ContributorApplicationDto app = contributorService.submitApplication(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(app, "Vehicle application submitted successfully"));
    }

    @GetMapping("/contributors/applications")
    public ResponseEntity<ApiResponse<List<ContributorDto.ContributorApplicationDto>>> getMyApplications() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(contributorService.getContributorApplications(user.getId()), "Applications retrieved"));
    }

    @GetMapping("/partners/incoming-applications")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ContributorDto.ContributorApplicationDto>>> getIncomingApplications() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(contributorService.getPartnerIncomingApplications(user.getId()), "Incoming applications retrieved"));
    }

    @PostMapping("/partners/propose-agreement")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContributorDto.AgreementDto>> proposeAgreement(
            @Valid @RequestBody ContributorDto.ProposeAgreementRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        ContributorDto.AgreementDto ag = contributorService.proposeAgreement(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(ag, "Agreement proposed successfully"));
    }

    @PostMapping("/contributors/agreements/{agreementId}/sign")
    public ResponseEntity<ApiResponse<ContributorDto.AgreementDto>> signAgreement(@PathVariable Long agreementId) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        ContributorDto.AgreementDto ag = contributorService.signAgreementByContributor(user.getId(), agreementId);
        return ResponseEntity.ok(ApiResponse.success(ag, "Agreement signed and vehicle published"));
    }

    @GetMapping("/contributors/agreements")
    public ResponseEntity<ApiResponse<List<ContributorDto.AgreementDto>>> getContributorAgreements() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(contributorService.getContributorAgreements(user.getId()), "Agreements retrieved"));
    }

    @GetMapping("/contributors/vehicles")
    public ResponseEntity<ApiResponse<List<ContributorDto.ContributorVehicleDto>>> getContributorVehicles() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(contributorService.getContributorVehicles(user.getId()), "Contributor vehicles retrieved"));
    }

    @PostMapping("/contributors/agreements/{agreementId}/withdraw")
    public ResponseEntity<ApiResponse<ContributorDto.AgreementDto>> requestWithdrawal(
            @PathVariable Long agreementId,
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        String reason = body != null ? body.getOrDefault("reason", "Contributor requested withdrawal") : "Contributor requested withdrawal";
        ContributorDto.AgreementDto ag = contributorService.requestWithdrawal(user.getId(), agreementId, reason);
        return ResponseEntity.ok(ApiResponse.success(ag, "Agreement withdrawal processed"));
    }

    @PostMapping({"/partners/applications/{applicationId}/reject", "/partners/incoming-applications/{applicationId}/reject"})
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContributorDto.ContributorApplicationDto>> rejectApplication(
            @PathVariable Long applicationId,
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        String reason = body != null ? body.getOrDefault("reason", body.get("rejectionReason")) : "Application rejected by partner";
        ContributorDto.ContributorApplicationDto app = contributorService.rejectApplication(user.getId(), applicationId, reason);
        return ResponseEntity.ok(ApiResponse.success(app, "Application rejected"));
    }

    @GetMapping("/partners/agreements")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ContributorDto.AgreementDto>>> getPartnerAgreements() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(contributorService.getPartnerAgreements(user.getId()), "Partner agreements retrieved"));
    }
}
