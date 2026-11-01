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
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElse(null);
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(paymentService.getDemandByHoldId(userId, holdId), "Payment demand retrieved"));
    }

    @GetMapping("/payments/demands/{demandId}")
    public ResponseEntity<ApiResponse<PaymentDto.PaymentDemandDto>> getDemandById(@PathVariable Long demandId) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElse(null);
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(paymentService.getDemandById(userId, demandId), "Payment demand retrieved"));
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

    @GetMapping({"/refunds/customer", "/payments/refunds/my"})
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

    @PostMapping({"/customer/refunds/{refundId}/respond", "/payments/refunds/{refundId}/respond"})
    public ResponseEntity<ApiResponse<PaymentDto.RentalFeeRefundDto>> customerRespondToRefund(
            @PathVariable Long refundId,
            @Valid @RequestBody PaymentDto.CustomerRefundActionRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.RentalFeeRefundDto resp = paymentService.customerRespondToRefund(user.getId(), refundId, request);
        return ResponseEntity.ok(ApiResponse.success(resp, request.isAcknowledged() ? "Refund acknowledged" : "Refund dispute opened"));
    }

    @PostMapping({"/payments/refunds/{refundId}/acknowledge", "/customer/refunds/{refundId}/acknowledge"})
    public ResponseEntity<ApiResponse<PaymentDto.RentalFeeRefundDto>> customerAcknowledgeRefund(@PathVariable Long refundId) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.CustomerRefundActionRequest req = new PaymentDto.CustomerRefundActionRequest(true, null);
        PaymentDto.RentalFeeRefundDto resp = paymentService.customerRespondToRefund(user.getId(), refundId, req);
        return ResponseEntity.ok(ApiResponse.success(resp, "Refund acknowledged"));
    }

    @PostMapping({"/payments/refunds/{refundId}/dispute", "/customer/refunds/{refundId}/dispute"})
    public ResponseEntity<ApiResponse<PaymentDto.RentalFeeRefundDto>> customerDisputeRefund(
            @PathVariable Long refundId,
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        String reason = body != null ? body.getOrDefault("disputeReason", "Customer disputed refund disbursement") : "Customer disputed refund disbursement";
        PaymentDto.CustomerRefundActionRequest req = new PaymentDto.CustomerRefundActionRequest(false, reason);
        PaymentDto.RentalFeeRefundDto resp = paymentService.customerRespondToRefund(user.getId(), refundId, req);
        return ResponseEntity.ok(ApiResponse.success(resp, "Refund dispute opened"));
    }

    @PostMapping({"/customer/deposits/{depositRefundId}/acknowledge", "/payments/deposits/{depositRefundId}/acknowledge"})
    public ResponseEntity<ApiResponse<PaymentDto.SecurityDepositDto>> customerAcknowledgeDepositRefund(@PathVariable Long depositRefundId) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.CustomerRefundActionRequest req = new PaymentDto.CustomerRefundActionRequest(true, null);
        PaymentDto.SecurityDepositDto resp = paymentService.customerRespondToDepositRefund(user.getId(), depositRefundId, req);
        return ResponseEntity.ok(ApiResponse.success(resp, "Deposit refund acknowledged"));
    }

    @PostMapping({"/customer/deposits/{depositRefundId}/dispute", "/payments/deposits/{depositRefundId}/dispute"})
    public ResponseEntity<ApiResponse<PaymentDto.SecurityDepositDto>> customerDisputeDepositRefund(
            @PathVariable Long depositRefundId,
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        String reason = body != null ? body.getOrDefault("disputeReason", "Customer disputed deposit deduction/refund") : "Customer disputed deposit deduction/refund";
        PaymentDto.CustomerRefundActionRequest req = new PaymentDto.CustomerRefundActionRequest(false, reason);
        PaymentDto.SecurityDepositDto resp = paymentService.customerRespondToDepositRefund(user.getId(), depositRefundId, req);
        return ResponseEntity.ok(ApiResponse.success(resp, "Deposit refund dispute opened"));
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

    @GetMapping({"/contributor/payables", "/contributors/payables"})
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

    @PostMapping({"/contributor/payables/{payableId}/respond", "/contributors/payables/{payableId}/respond"})
    public ResponseEntity<ApiResponse<PaymentDto.ContributorPayableDto>> respondToSettlement(
            @PathVariable Long payableId,
            @RequestParam(required = false) Boolean acknowledged,
            @RequestBody(required = false) java.util.Map<String, Object> body
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        boolean ack = true;
        String reason = null;
        if (acknowledged != null) {
            ack = acknowledged;
        } else if (body != null && body.get("acknowledged") != null) {
            ack = Boolean.parseBoolean(body.get("acknowledged").toString());
        }
        if (body != null) {
            reason = (String) body.get("disputeReason");
        }
        PaymentDto.ContributorPayableDto resp = paymentService.contributorRespondToSettlement(user.getId(), payableId, ack, reason);
        return ResponseEntity.ok(ApiResponse.success(resp, ack ? "Settlement confirmed" : "Settlement disputed"));
    }

    @PostMapping({"/contributors/payables/{payableId}/confirm", "/contributor/payables/{payableId}/confirm"})
    public ResponseEntity<ApiResponse<PaymentDto.ContributorPayableDto>> confirmSettlement(@PathVariable Long payableId) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        PaymentDto.ContributorPayableDto resp = paymentService.contributorRespondToSettlement(user.getId(), payableId, true, null);
        return ResponseEntity.ok(ApiResponse.success(resp, "Settlement confirmed"));
    }

    @PostMapping({"/contributors/payables/{payableId}/dispute", "/contributor/payables/{payableId}/dispute"})
    public ResponseEntity<ApiResponse<PaymentDto.ContributorPayableDto>> disputeSettlement(
            @PathVariable Long payableId,
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        String reason = body != null ? body.getOrDefault("disputeReason", "Contributor disputed settlement amount") : "Contributor disputed settlement amount";
        PaymentDto.ContributorPayableDto resp = paymentService.contributorRespondToSettlement(user.getId(), payableId, false, reason);
        return ResponseEntity.ok(ApiResponse.success(resp, "Settlement disputed"));
    }
}
