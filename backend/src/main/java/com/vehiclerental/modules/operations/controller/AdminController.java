package com.vehiclerental.modules.operations.controller;

import com.vehiclerental.common.response.ApiResponse;
import com.vehiclerental.common.response.PagedResponse;
import com.vehiclerental.common.security.SecurityUtils;
import com.vehiclerental.modules.operations.dto.OperationsDto;
import com.vehiclerental.modules.operations.entity.AuditLog;
import com.vehiclerental.modules.operations.service.AdminService;
import com.vehiclerental.modules.operations.service.AuditLogService;
import com.vehiclerental.modules.operations.service.SupportTicketService;
import com.vehiclerental.modules.partner.dto.PartnerDto;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.partner.service.PartnerService;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PartnerService partnerService;
    private final SupportTicketService ticketService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<OperationsDto.AdminPlatformStatsDto>> getPlatformStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getPlatformStats(), "Platform stats retrieved"));
    }

    @GetMapping("/partners")
    public ResponseEntity<ApiResponse<List<PartnerDto.PartnerProfileDto>>> listPartners(
            @RequestParam(required = false) String status
    ) {
        PartnerProfile.PartnerVerificationStatus verificationStatus = null;
        if (status != null && !status.isBlank()) {
            verificationStatus = PartnerProfile.PartnerVerificationStatus.valueOf(status.toUpperCase());
        }
        return ResponseEntity.ok(ApiResponse.success(partnerService.listAllPartners(verificationStatus), "Partners retrieved"));
    }

    @PostMapping("/partners/{partnerId}/review")
    public ResponseEntity<ApiResponse<PartnerDto.PartnerProfileDto>> reviewPartner(
            @PathVariable Long partnerId,
            @RequestBody PartnerDto.ReviewApplicationRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User admin = userRepository.findByEmail(email).orElseThrow();
        PartnerDto.PartnerProfileDto resp = partnerService.reviewApplication(partnerId, admin.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(resp, request.isApproved() ? "Partner approved" : "Partner rejected"));
    }

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<PagedResponse<OperationsDto.TicketDto>>> listTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OperationsDto.TicketDto> result = ticketService.getAllTickets(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(result), "All tickets retrieved"));
    }

    @PostMapping("/refunds/{refundId}/arbitrate")
    public ResponseEntity<ApiResponse<Void>> arbitrateRefund(
            @PathVariable Long refundId,
            @Valid @RequestBody OperationsDto.ResolveDisputeRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User admin = userRepository.findByEmail(email).orElseThrow();
        adminService.arbitrateRefundDispute(admin.getId(), refundId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Refund dispute arbitrated"));
    }

    @PostMapping("/deductions/{deductionId}/arbitrate")
    public ResponseEntity<ApiResponse<Void>> arbitrateDeduction(
            @PathVariable Long deductionId,
            @Valid @RequestBody OperationsDto.ResolveDisputeRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User admin = userRepository.findByEmail(email).orElseThrow();
        adminService.arbitrateDepositDeduction(admin.getId(), deductionId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Deposit deduction arbitrated"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> result = auditLogService.getLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(result), "Audit logs retrieved"));
    }
}
