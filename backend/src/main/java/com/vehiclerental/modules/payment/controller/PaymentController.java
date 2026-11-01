package com.vehiclerental.modules.payment.controller;

import com.vehiclerental.common.response.ApiResponse;
import com.vehiclerental.common.security.SecurityUtils;
import com.vehiclerental.modules.payment.dto.PaymentDto;
import com.vehiclerental.modules.payment.service.PaymentService;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping({"/payments/submit", "/payments/submissions"})
    public ResponseEntity<ApiResponse<PaymentDto.PaymentSubmissionDto>> submitPayment(
            @Valid @RequestBody PaymentDto.PaymentSubmissionRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.PaymentSubmissionDto resp = paymentService.submitPayment(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Payment submitted for partner verification"));
    }

    @PostMapping({"/partners/payments/{submissionId}/verify", "/partners/payments/submissions/{submissionId}/verify"})
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto.PaymentSubmissionDto>> verifySubmission(
            @PathVariable Long submissionId,
            @RequestBody(required = false) PaymentDto.VerifySubmissionRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.VerifySubmissionRequest req = request != null ? request : new PaymentDto.VerifySubmissionRequest(true, null);
        PaymentDto.PaymentSubmissionDto resp = paymentService.verifySubmission(user.getId(), submissionId, req);
        return ResponseEntity.ok(ApiResponse.success(resp, "Payment submission processed"));
    }

    @PostMapping({"/partners/payments/{submissionId}/reject", "/partners/payments/submissions/{submissionId}/reject"})
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto.PaymentSubmissionDto>> rejectSubmission(
            @PathVariable Long submissionId,
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        String reason = body != null ? body.getOrDefault("rejectionReason", "Payment details could not be verified") : "Payment details could not be verified";
        PaymentDto.PaymentSubmissionDto resp = paymentService.rejectSubmission(user.getId(), submissionId, reason);
        return ResponseEntity.ok(ApiResponse.success(resp, "Payment submission rejected"));
    }

    @GetMapping({"/partners/payments/submissions", "/partners/payments/pending"})
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentDto.PaymentSubmissionDto>>> getPartnerSubmissions() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPendingSubmissionsForPartner(user.getId()), "Submissions retrieved"));
    }

    @GetMapping("/payments/demands/hold/{holdId}")
    public ResponseEntity<ApiResponse<PaymentDto.PaymentDemandDto>> getDemandByHold(@PathVariable Long holdId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getDemandByHoldId(holdId), "Payment demand retrieved"));
    }

    @GetMapping("/payments/demands/{demandId}")
    public ResponseEntity<ApiResponse<PaymentDto.PaymentDemandDto>> getDemandById(@PathVariable Long demandId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getDemandById(demandId), "Payment demand retrieved"));
    }

    @PostMapping("/payments/reapply")
    public ResponseEntity<ApiResponse<PaymentDto.PaymentSubmissionDto>> reapplyPayment(
            @Valid @RequestBody PaymentDto.ReapplyPaymentRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.PaymentSubmissionDto resp = paymentService.reapplyPaymentToNewHold(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Payment reapplied to new hold successfully"));
    }

    @GetMapping("/payments/demands/{demandId}/refundable-balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getRefundableBalance(@PathVariable Long demandId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.calculateAvailableRefundableBalance(demandId), "Available balance calculated"));
    }

    @GetMapping("/refunds/customer")
    public ResponseEntity<ApiResponse<List<PaymentDto.RentalFeeRefundDto>>> getCustomerRefunds() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(paymentService.getRefundsForCustomer(user.getId()), "Customer refunds retrieved"));
    }

    @GetMapping("/refunds/partner")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentDto.RentalFeeRefundDto>>> getPartnerRefunds() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(paymentService.getRefundsForPartner(user.getId()), "Partner refunds retrieved"));
    }

    @PostMapping("/partners/refunds/{refundId}/disburse")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto.RentalFeeRefundDto>> disburseRefund(
            @PathVariable Long refundId,
            @Valid @RequestBody PaymentDto.RecordRefundDisbursementRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.RentalFeeRefundDto resp = paymentService.recordRefundDisbursement(user.getId(), refundId, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Refund disbursement recorded"));
    }

    @PostMapping("/customer/refunds/{refundId}/respond")
    public ResponseEntity<ApiResponse<PaymentDto.RentalFeeRefundDto>> customerRespondToRefund(
            @PathVariable Long refundId,
            @Valid @RequestBody PaymentDto.CustomerRefundActionRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.RentalFeeRefundDto resp = paymentService.customerRespondToRefund(user.getId(), refundId, request);
        return ResponseEntity.ok(ApiResponse.success(resp, request.isAcknowledged() ? "Refund acknowledged" : "Refund dispute opened"));
    }

    @PostMapping("/partners/deposits/{bookingId}/collect")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto.SecurityDepositDto>> recordDepositCollection(
            @PathVariable Long bookingId,
            @Valid @RequestBody PaymentDto.RecordDepositCollectionRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.SecurityDepositDto resp = paymentService.recordDepositCollection(user.getId(), bookingId, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Deposit collection recorded"));
    }

    @PostMapping("/partners/deposits/deductions")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> proposeDeduction(
            @Valid @RequestBody PaymentDto.ProposeDeductionRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        paymentService.proposeDepositDeduction(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Deposit deduction proposed"));
    }

    @PostMapping("/partners/deposits/{depositId}/refund")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto.SecurityDepositDto>> recordDepositRefund(
            @PathVariable Long depositId,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "UPI_TRANSFER") String method,
            @RequestParam String utr
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.SecurityDepositDto resp = paymentService.recordDepositRefund(user.getId(), depositId, amount, method, utr);
        return ResponseEntity.ok(ApiResponse.success(resp, "Deposit refund recorded"));
    }

    @GetMapping("/contributor/payables")
    public ResponseEntity<ApiResponse<List<PaymentDto.ContributorPayableDto>>> getContributorPayables() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPayablesForContributor(user.getId()), "Payables retrieved"));
    }

    @GetMapping("/partners/payables")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentDto.ContributorPayableDto>>> getPartnerPayables() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPayablesForPartner(user.getId()), "Payables retrieved"));
    }

    @PostMapping("/partners/payables/{payableId}/settle")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto.ContributorPayableDto>> settlePayable(
            @PathVariable Long payableId,
            @Valid @RequestBody PaymentDto.RecordContributorSettlementRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.ContributorPayableDto resp = paymentService.recordContributorSettlement(user.getId(), payableId, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Contributor settlement recorded"));
    }

    @PostMapping("/contributor/payables/{payableId}/respond")
    public ResponseEntity<ApiResponse<PaymentDto.ContributorPayableDto>> respondToSettlement(
            @PathVariable Long payableId,
            @RequestParam boolean acknowledged,
            @RequestParam(required = false) String disputeReason
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.ContributorPayableDto resp = paymentService.contributorRespondToSettlement(user.getId(), payableId, acknowledged, disputeReason);
        return ResponseEntity.ok(ApiResponse.success(resp, acknowledged ? "Settlement confirmed" : "Settlement disputed"));
    }
}
