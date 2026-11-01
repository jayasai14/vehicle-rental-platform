package com.vehiclerental.modules.payment.service;

import com.vehiclerental.common.exception.BadRequestException;
import com.vehiclerental.common.exception.ResourceNotFoundException;
import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.booking.entity.BookingHold;
import com.vehiclerental.modules.booking.repository.BookingHoldRepository;
import com.vehiclerental.modules.booking.repository.BookingRepository;
import com.vehiclerental.modules.booking.service.BookingService;
import com.vehiclerental.modules.operations.entity.SupportTicket;
import com.vehiclerental.modules.operations.repository.SupportTicketRepository;
import com.vehiclerental.modules.partner.entity.PartnerPaymentProfile;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.partner.repository.PartnerPaymentProfileRepository;
import com.vehiclerental.modules.partner.repository.PartnerProfileRepository;
import com.vehiclerental.modules.payment.dto.PaymentDto;
import com.vehiclerental.modules.payment.entity.*;
import com.vehiclerental.modules.payment.repository.*;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RentalPaymentDemandRepository demandRepository;
    private final RentalPaymentSubmissionRepository submissionRepository;
    private final RentalFeeRefundRepository refundRepository;
    private final SecurityDepositRepository depositRepository;
    private final DepositDeductionRepository deductionRepository;
    private final SecurityDepositRefundRepository depositRefundRepository;
    private final ContributorPayableRepository contributorPayableRepository;
    private final BookingHoldRepository holdRepository;
    private final BookingRepository bookingRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final PartnerPaymentProfileRepository partnerPaymentProfileRepository;
    private final UserRepository userRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final BookingService bookingService;

    // ------------------------------------------------------------------------
    // PAYMENT SUBMISSION & VERIFICATION (PARTNER-DIRECT)
    // ------------------------------------------------------------------------

    @Transactional
    public PaymentDto.PaymentSubmissionDto submitPayment(Long customerUserId, PaymentDto.PaymentSubmissionRequest request) {
        User customer = userRepository.findById(customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        RentalPaymentDemand demand = demandRepository.findById(request.getDemandId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment demand not found with id: " + request.getDemandId()));

        if (!demand.getCustomerUser().getId().equals(customerUserId)) {
            throw new BadRequestException("Unauthorized access to this payment demand");
        }

        // Prevent duplicate UTR submission for this partner
        if (request.getTransactionReference() != null && !request.getTransactionReference().isBlank()) {
            submissionRepository.findByManagerPartnerIdAndTransactionReference(
                    demand.getManagerPartner().getId(),
                    request.getTransactionReference().trim()
            ).ifPresent(existing -> {
                throw new BadRequestException("Payment reference/UTR " + request.getTransactionReference() + " has already been submitted for this partner");
            });
        }

        String subRef = "SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        RentalPaymentSubmission.PaymentMethod method;
        String rawMethod = request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase().trim() : "UPI_QR";
        if ("UPI".equals(rawMethod) || "UPI_QR".equals(rawMethod)) {
            method = RentalPaymentSubmission.PaymentMethod.UPI_QR;
        } else if ("UPI_ID".equals(rawMethod)) {
            method = RentalPaymentSubmission.PaymentMethod.UPI_ID;
        } else if ("CASH".equals(rawMethod) || "CASH_AT_PICKUP".equals(rawMethod)) {
            method = RentalPaymentSubmission.PaymentMethod.CASH_AT_PICKUP;
        } else if ("EXTERNAL_LINK".equals(rawMethod) || "LINK".equals(rawMethod)) {
            method = RentalPaymentSubmission.PaymentMethod.EXTERNAL_LINK;
        } else {
            method = RentalPaymentSubmission.PaymentMethod.UPI_QR;
        }

        RentalPaymentSubmission submission = RentalPaymentSubmission.builder()
                .submissionReference(subRef)
                .demand(demand)
                .managerPartner(demand.getManagerPartner())
                .customerUser(customer)
                .paymentMethod(method)
                .transactionReference(request.getTransactionReference() != null ? request.getTransactionReference().trim() : null)
                .amountSubmitted(request.getAmountSubmitted())
                .currency("INR")
                .receiptObjectKey(request.getReceiptObjectKey())
                .transactionTimestamp(LocalDateTime.now())
                .submissionStatus(RentalPaymentSubmission.SubmissionStatus.SUBMITTED_FOR_VERIFICATION)
                .reappliedAmount(BigDecimal.ZERO)
                .build();
        submission = submissionRepository.save(submission);

        // Update hold status to VERIFICATION_PENDING if currently ACTIVE
        if (demand.getHold() != null && demand.getHold().getStatus() == BookingHold.HoldStatus.ACTIVE) {
            demand.getHold().setStatus(BookingHold.HoldStatus.VERIFICATION_PENDING);
            holdRepository.save(demand.getHold());
        }

        return mapSubmissionToDto(submission);
    }

    @Transactional
    public PaymentDto.PaymentSubmissionDto verifySubmission(
            Long partnerUserId,
            Long submissionId,
            PaymentDto.VerifySubmissionRequest request
    ) {
        RentalPaymentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        if (!submission.getManagerPartner().getUser().getId().equals(partnerUserId)) {
            throw new BadRequestException("Only the managing partner can verify this submission");
        }

        if (submission.getSubmissionStatus() != RentalPaymentSubmission.SubmissionStatus.SUBMITTED_FOR_VERIFICATION) {
            throw new BadRequestException("Submission has already been processed with status: " + submission.getSubmissionStatus());
        }

        User partnerUser = userRepository.findById(partnerUserId).orElseThrow();

        if (!request.isVerified()) {
            submission.setSubmissionStatus(RentalPaymentSubmission.SubmissionStatus.REJECTED);
            submission.setRejectionReason(request.getRejectionReason());
            submission.setVerifiedAt(LocalDateTime.now());
            submission.setVerifiedByUser(partnerUser);
            return mapSubmissionToDto(submissionRepository.save(submission));
        }

        // Explicit partner verification
        submission.setSubmissionStatus(RentalPaymentSubmission.SubmissionStatus.VERIFIED);
        submission.setVerifiedAt(LocalDateTime.now());
        submission.setVerifiedByUser(partnerUser);
        submission = submissionRepository.save(submission);

        // Lock demand and update financial figures
        RentalPaymentDemand demand = demandRepository.findByIdWithLock(submission.getDemand().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Demand not found"));

        BigDecimal verifiedTotal = demand.getTotalVerifiedSubmitted().add(submission.getAmountSubmitted());
        demand.setTotalVerifiedSubmitted(verifiedTotal);

        BigDecimal needed = demand.getAmountDemanded().subtract(demand.getAmountSatisfied());
        if (needed.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal toSatisfy = submission.getAmountSubmitted().min(needed);
            demand.setAmountSatisfied(demand.getAmountSatisfied().add(toSatisfy));
            BigDecimal excess = submission.getAmountSubmitted().subtract(toSatisfy);
            if (excess.compareTo(BigDecimal.ZERO) > 0) {
                demand.setExcessAmount(demand.getExcessAmount().add(excess));
            }
        } else {
            demand.setExcessAmount(demand.getExcessAmount().add(submission.getAmountSubmitted()));
        }

        if (demand.getAmountSatisfied().compareTo(demand.getAmountDemanded()) >= 0) {
            demand.setStatus(RentalPaymentDemand.DemandStatus.SATISFIED);
        } else {
            demand.setStatus(RentalPaymentDemand.DemandStatus.PARTIALLY_SATISFIED);
        }
        demand = demandRepository.save(demand);

        // If booking confirmation demand is satisfied and hold is present, confirm the booking
        if (demand.getDemandType() == RentalPaymentDemand.DemandType.BOOKING_CONFIRMATION
                && demand.getStatus() == RentalPaymentDemand.DemandStatus.SATISFIED
                && demand.getHold() != null) {
            bookingService.confirmBookingFromHold(demand.getHold().getId());
        }

        return mapSubmissionToDto(submission);
    }

    // ------------------------------------------------------------------------
    // FINANCIAL SAFEGUARD A: LATE-PAYMENT REAPPLICATION
    // ------------------------------------------------------------------------

    @Transactional
    public PaymentDto.PaymentSubmissionDto reapplyPaymentToNewHold(Long customerUserId, PaymentDto.ReapplyPaymentRequest request) {
        RentalPaymentSubmission originalSub = submissionRepository.findById(request.getOriginalSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        if (!originalSub.getCustomerUser().getId().equals(customerUserId)) {
            throw new BadRequestException("Unauthorized access to this payment submission");
        }

        if (originalSub.getSubmissionStatus() != RentalPaymentSubmission.SubmissionStatus.VERIFIED) {
            throw new BadRequestException("Only verified payments can be reapplied");
        }

        // Prevent reuse of the same verified money more than once
        if (originalSub.getReappliedAmount().compareTo(BigDecimal.ZERO) > 0 || originalSub.getRecoveredForDemand() != null) {
            throw new BadRequestException("This payment has already been reapplied to demand ID: " +
                    (originalSub.getRecoveredForDemand() != null ? originalSub.getRecoveredForDemand().getId() : "unknown"));
        }

        // Check original demand is indeed expired/cancelled
        RentalPaymentDemand originalDemand = originalSub.getDemand();

        // Lock new hold and new demand
        BookingHold newHold = holdRepository.findByIdWithLock(request.getNewHoldId())
                .orElseThrow(() -> new ResourceNotFoundException("New hold not found"));

        if (newHold.getStatus() != BookingHold.HoldStatus.ACTIVE) {
            throw new BadRequestException("New hold is not active");
        }

        RentalPaymentDemand newDemand = demandRepository.findByHoldId(newHold.getId())
                .orElseThrow(() -> new ResourceNotFoundException("New demand not found"));

        // Durable audit relationship
        originalSub.setRecoveredForDemand(newDemand);
        originalSub.setReappliedAmount(originalSub.getAmountSubmitted());
        originalSub.setReappliedAt(LocalDateTime.now());
        submissionRepository.save(originalSub);

        // Link new hold to original expired hold
        if (originalDemand.getHold() != null) {
            newHold.setRecoveredFromHold(originalDemand.getHold());
            holdRepository.save(newHold);
        }

        // Credit new demand
        newDemand.setTotalVerifiedSubmitted(newDemand.getTotalVerifiedSubmitted().add(originalSub.getAmountSubmitted()));
        BigDecimal needed = newDemand.getAmountDemanded().subtract(newDemand.getAmountSatisfied());
        BigDecimal toSatisfy = originalSub.getAmountSubmitted().min(needed);
        newDemand.setAmountSatisfied(newDemand.getAmountSatisfied().add(toSatisfy));
        if (originalSub.getAmountSubmitted().compareTo(needed) > 0) {
            newDemand.setExcessAmount(newDemand.getExcessAmount().add(originalSub.getAmountSubmitted().subtract(needed)));
        }

        if (newDemand.getAmountSatisfied().compareTo(newDemand.getAmountDemanded()) >= 0) {
            newDemand.setStatus(RentalPaymentDemand.DemandStatus.SATISFIED);
        } else {
            newDemand.setStatus(RentalPaymentDemand.DemandStatus.PARTIALLY_SATISFIED);
        }
        demandRepository.save(newDemand);

        if (newDemand.getStatus() == RentalPaymentDemand.DemandStatus.SATISFIED) {
            bookingService.confirmBookingFromHold(newHold.getId());
        }

        return mapSubmissionToDto(originalSub);
    }

    // ------------------------------------------------------------------------
    // FINANCIAL SAFEGUARD B: REFUNDABLE BALANCE & RENTAL FEE REFUNDS
    // ------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public BigDecimal calculateAvailableRefundableBalance(Long demandId) {
        RentalPaymentDemand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResourceNotFoundException("Demand not found"));

        BigDecimal verified = demand.getTotalVerifiedSubmitted();
        BigDecimal retainedCancellation = demand.getCancellationFeeRetained() != null ? demand.getCancellationFeeRetained() : BigDecimal.ZERO;
        BigDecimal unvoidedRefunds = refundRepository.getCumulativeUnvoidedRefundAmountForDemand(demandId);

        BigDecimal available = verified.subtract(retainedCancellation).subtract(unvoidedRefunds);
        return available.max(BigDecimal.ZERO);
    }

    @Transactional
    public PaymentDto.RentalFeeRefundDto recordRefundDisbursement(
            Long partnerUserId,
            Long refundId,
            PaymentDto.RecordRefundDisbursementRequest request
    ) {
        RentalFeeRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));

        if (!refund.getManagerPartner().getUser().getId().equals(partnerUserId)) {
            throw new BadRequestException("Unauthorized: Only the managing partner can record disbursement");
        }

        if (refund.getRefundStatus() != RentalFeeRefund.RefundStatus.PENDING_PARTNER_DISBURSEMENT) {
            throw new BadRequestException("Refund is not in pending disbursement status: " + refund.getRefundStatus());
        }

        final Long currentRefundId = refund.getId();
        // Prevent duplicate partner UTR
        if (request.getPartnerTransactionReference() != null && !request.getPartnerTransactionReference().isBlank()) {
            refundRepository.findByManagerPartnerIdAndPartnerTransactionReference(
                    refund.getManagerPartner().getId(),
                    request.getPartnerTransactionReference().trim()
            ).ifPresent(existing -> {
                if (!existing.getId().equals(currentRefundId)) {
                    throw new BadRequestException("Refund transaction reference " + request.getPartnerTransactionReference() + " already used");
                }
            });
        }

        refund.setRefundMethod(RentalFeeRefund.RefundMethod.valueOf(request.getRefundMethod()));
        refund.setPartnerTransactionReference(request.getPartnerTransactionReference().trim());
        refund.setReceiptObjectKey(request.getReceiptObjectKey());
        refund.setRefundStatus(RentalFeeRefund.RefundStatus.DISBURSED_BY_PARTNER);
        refund.setDisbursedAt(LocalDateTime.now());
        refund = refundRepository.save(refund);

        return mapRefundToDto(refund);
    }

    @Transactional
    public PaymentDto.RentalFeeRefundDto customerRespondToRefund(
            Long customerUserId,
            Long refundId,
            PaymentDto.CustomerRefundActionRequest request
    ) {
        RentalFeeRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));

        if (!refund.getCustomerUser().getId().equals(customerUserId)) {
            throw new BadRequestException("Unauthorized access to this refund");
        }

        if (refund.getRefundStatus() != RentalFeeRefund.RefundStatus.DISBURSED_BY_PARTNER) {
            throw new BadRequestException("Refund can only be responded to when disbursed by partner");
        }

        if (request.isAcknowledged()) {
            refund.setRefundStatus(RentalFeeRefund.RefundStatus.CUSTOMER_ACKNOWLEDGED);
            refund.setAcknowledgedAt(LocalDateTime.now());
        } else {
            refund.setRefundStatus(RentalFeeRefund.RefundStatus.DISPUTED_BY_CUSTOMER);
            refund.setDisputeReason(request.getDisputeReason());
            refund.setDisputeOpenedAt(LocalDateTime.now());

            // Create dispute support ticket routed to Admin
            String ticketRef = "TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            SupportTicket ticket = SupportTicket.builder()
                    .ticketReference(ticketRef)
                    .user(refund.getCustomerUser())
                    .booking(refund.getBooking())
                    .subject("Disputed Rental Refund: " + refund.getRefundReference())
                    .category(SupportTicket.TicketCategory.REFUND_DISPUTE)
                    .status(SupportTicket.TicketStatus.OPEN)
                    .priority(SupportTicket.TicketPriority.HIGH)
                    .build();
            ticket = supportTicketRepository.save(ticket);
            refund.setDisputeTicket(ticket);
        }

        refund = refundRepository.save(refund);
        return mapRefundToDto(refund);
    }

    // ------------------------------------------------------------------------
    // SECURITY DEPOSITS, DEDUCTIONS & DISPUTES
    // ------------------------------------------------------------------------

    @Transactional
    public PaymentDto.SecurityDepositDto recordDepositCollection(
            Long partnerUserId,
            Long bookingId,
            PaymentDto.RecordDepositCollectionRequest request
    ) {
        SecurityDeposit deposit = depositRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit not found for booking: " + bookingId));

        if (!deposit.getManagerPartner().getUser().getId().equals(partnerUserId)) {
            throw new BadRequestException("Unauthorized: Only managing partner can record deposit collection");
        }

        deposit.setCollectedAmount(request.getCollectedAmount());
        deposit.setHeldAmount(request.getCollectedAmount());
        deposit.setCollectionMethod(SecurityDeposit.CollectionMethod.valueOf(request.getCollectionMethod()));
        deposit.setCollectionTransactionReference(request.getCollectionTransactionReference());
        deposit.setStatus(SecurityDeposit.DepositStatus.HELD_BY_PARTNER);
        deposit.setCollectedAt(LocalDateTime.now());
        deposit = depositRepository.save(deposit);

        return mapDepositToDto(deposit);
    }

    @Transactional
    public void proposeDepositDeduction(Long partnerUserId, PaymentDto.ProposeDeductionRequest request) {
        SecurityDeposit deposit = depositRepository.findById(request.getDepositId())
                .orElseThrow(() -> new ResourceNotFoundException("Deposit not found"));

        if (!deposit.getManagerPartner().getUser().getId().equals(partnerUserId)) {
            throw new BadRequestException("Unauthorized: Only managing partner can propose deduction");
        }

        User partnerUser = userRepository.findById(partnerUserId).orElseThrow();

        DepositDeduction deduction = DepositDeduction.builder()
                .deposit(deposit)
                .deductionReason(DepositDeduction.DeductionReason.valueOf(request.getDeductionReason()))
                .proposedDeductionAmount(request.getProposedDeductionAmount())
                .proposedByUser(partnerUser)
                .evidenceDocumentKey(request.getEvidenceDocumentKey())
                .status(DepositDeduction.DeductionStatus.PROPOSED)
                .build();
        deductionRepository.save(deduction);
    }

    @Transactional
    public PaymentDto.SecurityDepositDto recordDepositRefund(
            Long partnerUserId,
            Long depositId,
            BigDecimal refundAmount,
            String refundMethod,
            String utr
    ) {
        SecurityDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit not found"));

        if (!deposit.getManagerPartner().getUser().getId().equals(partnerUserId)) {
            throw new BadRequestException("Unauthorized partner");
        }

        BigDecimal cumulativeRefunds = depositRefundRepository.getCumulativeUnvoidedRefundAmountForDeposit(depositId);
        BigDecimal maxRefundable = deposit.getHeldAmount().subtract(cumulativeRefunds);
        if (refundAmount.compareTo(maxRefundable) > 0) {
            throw new BadRequestException("Refund amount " + refundAmount + " exceeds available held deposit " + maxRefundable);
        }

        SecurityDepositRefund refund = SecurityDepositRefund.builder()
                .deposit(deposit)
                .managerPartner(deposit.getManagerPartner())
                .customerUser(deposit.getCustomerUser())
                .refundAmount(refundAmount)
                .refundMethod(SecurityDepositRefund.RefundMethod.valueOf(refundMethod))
                .partnerTransactionReference(utr)
                .refundStatus(SecurityDepositRefund.RefundStatus.DISBURSED_BY_PARTNER)
                .disbursedAt(LocalDateTime.now())
                .build();
        depositRefundRepository.save(refund);

        deposit.setRefundedAmount(deposit.getRefundedAmount().add(refundAmount));
        if (deposit.getRefundedAmount().compareTo(deposit.getHeldAmount()) >= 0) {
            deposit.setStatus(SecurityDeposit.DepositStatus.FULLY_REFUNDED);
        } else {
            deposit.setStatus(SecurityDeposit.DepositStatus.PARTIALLY_REFUNDED);
        }
        deposit = depositRepository.save(deposit);

        return mapDepositToDto(deposit);
    }

    // ------------------------------------------------------------------------
    // CONTRIBUTOR PAYABLE SETTLEMENT
    // ------------------------------------------------------------------------

    @Transactional
    public PaymentDto.ContributorPayableDto recordContributorSettlement(
            Long partnerUserId,
            Long payableId,
            PaymentDto.RecordContributorSettlementRequest request
    ) {
        ContributorPayable payable = contributorPayableRepository.findById(payableId)
                .orElseThrow(() -> new ResourceNotFoundException("Contributor payable not found"));

        if (!payable.getPartner().getUser().getId().equals(partnerUserId)) {
            throw new BadRequestException("Unauthorized: Only managing partner can settle this payable");
        }

        payable.setPartnerPaymentMethod(ContributorPayable.PartnerPaymentMethod.valueOf(request.getPartnerPaymentMethod()));
        payable.setPartnerTransactionReference(request.getPartnerTransactionReference());
        payable.setPartnerReceiptObjectKey(request.getPartnerReceiptObjectKey());
        payable.setPayableStatus(ContributorPayable.PayableStatus.SETTLEMENT_RECORDED);
        payable.setSettledAt(LocalDateTime.now());
        payable = contributorPayableRepository.save(payable);

        return mapPayableToDto(payable);
    }

    @Transactional
    public PaymentDto.ContributorPayableDto contributorRespondToSettlement(
            Long contributorUserId,
            Long payableId,
            boolean acknowledged,
            String disputeReason
    ) {
        ContributorPayable payable = contributorPayableRepository.findById(payableId)
                .orElseThrow(() -> new ResourceNotFoundException("Contributor payable not found"));

        if (!payable.getContributorUser().getId().equals(contributorUserId)) {
            throw new BadRequestException("Unauthorized: Only the vehicle contributor can respond");
        }

        if (acknowledged) {
            payable.setPayableStatus(ContributorPayable.PayableStatus.CONFIRMED_BY_CONTRIBUTOR);
            payable.setConfirmedAt(LocalDateTime.now());
        } else {
            payable.setPayableStatus(ContributorPayable.PayableStatus.DISPUTED);
            payable.setDisputeReason(disputeReason);
            payable.setDisputedAt(LocalDateTime.now());
        }

        payable = contributorPayableRepository.save(payable);
        return mapPayableToDto(payable);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.PaymentSubmissionDto> getPendingSubmissionsForPartner(Long partnerUserId) {
        PartnerProfile partner = partnerProfileRepository.findByUserId(partnerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found"));
        return submissionRepository.findByManagerPartnerId(partner.getId()).stream()
                .map(this::mapSubmissionToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.RentalFeeRefundDto> getRefundsForCustomer(Long customerUserId) {
        return refundRepository.findByCustomerUserId(customerUserId).stream()
                .map(this::mapRefundToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.RentalFeeRefundDto> getRefundsForPartner(Long partnerUserId) {
        PartnerProfile partner = partnerProfileRepository.findByUserId(partnerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found"));
        return refundRepository.findByManagerPartnerId(partner.getId()).stream()
                .map(this::mapRefundToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.ContributorPayableDto> getPayablesForContributor(Long contributorUserId) {
        return contributorPayableRepository.findByContributorUserId(contributorUserId).stream()
                .map(this::mapPayableToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.ContributorPayableDto> getPayablesForPartner(Long partnerUserId) {
        PartnerProfile partner = partnerProfileRepository.findByUserId(partnerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found"));
        return contributorPayableRepository.findByPartnerId(partner.getId()).stream()
                .map(this::mapPayableToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentDto.PaymentDemandDto getDemandByHoldId(Long holdId) {
        RentalPaymentDemand demand = demandRepository.findByHoldId(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("Demand not found for hold: " + holdId));
        return mapDemandToDto(demand);
    }

    @Transactional(readOnly = true)
    public PaymentDto.PaymentDemandDto getDemandById(Long demandId) {
        RentalPaymentDemand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResourceNotFoundException("Demand not found with id: " + demandId));
        return mapDemandToDto(demand);
    }

    @Transactional
    public PaymentDto.PaymentSubmissionDto rejectSubmission(Long partnerUserId, Long submissionId, String rejectionReason) {
        RentalPaymentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        if (!submission.getManagerPartner().getUser().getId().equals(partnerUserId)) {
            throw new BadRequestException("Only the managing partner can reject this submission");
        }

        User partnerUser = userRepository.findById(partnerUserId).orElseThrow();
        submission.setSubmissionStatus(RentalPaymentSubmission.SubmissionStatus.REJECTED);
        submission.setRejectionReason(rejectionReason);
        submission.setVerifiedAt(LocalDateTime.now());
        submission.setVerifiedByUser(partnerUser);
        return mapSubmissionToDto(submissionRepository.save(submission));
    }

    public PaymentDto.PaymentDemandDto mapDemandToDto(RentalPaymentDemand d) {
        String partnerUpi = null;
        String qrKey = null;
        if (d.getManagerPartner() != null && partnerPaymentProfileRepository != null) {
            PartnerPaymentProfile ppp = partnerPaymentProfileRepository.findByPartnerId(d.getManagerPartner().getId()).orElse(null);
            if (ppp != null) {
                partnerUpi = ppp.getUpiId();
                qrKey = ppp.getUpiQrObjectKey();
            }
        }
        return PaymentDto.PaymentDemandDto.builder()
                .id(d.getId())
                .demandReference(d.getDemandReference())
                .holdId(d.getHold() != null ? d.getHold().getId() : null)
                .managerPartnerId(d.getManagerPartner() != null ? d.getManagerPartner().getId() : null)
                .partnerBusinessName(d.getManagerPartner() != null ? d.getManagerPartner().getBusinessName() : null)
                .partnerUpiId(partnerUpi != null ? partnerUpi : "srirental@okaxis")
                .partnerQrCodeUrl(qrKey)
                .customerUserId(d.getCustomerUser() != null ? d.getCustomerUser().getId() : null)
                .demandType(d.getDemandType() != null ? d.getDemandType().name() : null)
                .amountDemanded(d.getAmountDemanded())
                .totalVerifiedSubmitted(d.getTotalVerifiedSubmitted())
                .amountSatisfied(d.getAmountSatisfied())
                .excessAmount(d.getExcessAmount())
                .cancellationFeeRetained(d.getCancellationFeeRetained())
                .currency(d.getCurrency())
                .status(d.getStatus() != null ? d.getStatus().name() : null)
                .createdAt(d.getCreatedAt())
                .build();
    }

    private PaymentDto.PaymentSubmissionDto mapSubmissionToDto(RentalPaymentSubmission s) {
        return PaymentDto.PaymentSubmissionDto.builder()
                .id(s.getId())
                .submissionReference(s.getSubmissionReference())
                .demandId(s.getDemand() != null ? s.getDemand().getId() : null)
                .managerPartnerId(s.getManagerPartner() != null ? s.getManagerPartner().getId() : null)
                .customerUserId(s.getCustomerUser() != null ? s.getCustomerUser().getId() : null)
                .customerName(s.getCustomerUser() != null ? s.getCustomerUser().getFullName() : null)
                .paymentMethod(s.getPaymentMethod() != null ? s.getPaymentMethod().name() : null)
                .transactionReference(s.getTransactionReference())
                .amountSubmitted(s.getAmountSubmitted())
                .submissionStatus(s.getSubmissionStatus() != null ? s.getSubmissionStatus().name() : null)
                .transactionTimestamp(s.getTransactionTimestamp())
                .verifiedAt(s.getVerifiedAt())
                .rejectionReason(s.getRejectionReason())
                .recoveredForDemandId(s.getRecoveredForDemand() != null ? s.getRecoveredForDemand().getId() : null)
                .reappliedAmount(s.getReappliedAmount())
                .build();
    }

    private PaymentDto.RentalFeeRefundDto mapRefundToDto(RentalFeeRefund r) {
        return PaymentDto.RentalFeeRefundDto.builder()
                .id(r.getId())
                .refundReference(r.getRefundReference())
                .bookingId(r.getBooking() != null ? r.getBooking().getId() : null)
                .demandId(r.getDemand() != null ? r.getDemand().getId() : null)
                .managerPartnerId(r.getManagerPartner() != null ? r.getManagerPartner().getId() : null)
                .customerUserId(r.getCustomerUser() != null ? r.getCustomerUser().getId() : null)
                .refundReason(r.getRefundReason() != null ? r.getRefundReason().name() : null)
                .grossAmountPaid(r.getGrossAmountPaid())
                .cancellationFeeDeducted(r.getCancellationFeeDeducted())
                .refundAmount(r.getRefundAmount())
                .refundStatus(r.getRefundStatus() != null ? r.getRefundStatus().name() : null)
                .refundMethod(r.getRefundMethod() != null ? r.getRefundMethod().name() : null)
                .partnerTransactionReference(r.getPartnerTransactionReference())
                .receiptObjectKey(r.getReceiptObjectKey())
                .disbursedAt(r.getDisbursedAt())
                .acknowledgedAt(r.getAcknowledgedAt())
                .disputeReason(r.getDisputeReason())
                .disputeTicketId(r.getDisputeTicket() != null ? r.getDisputeTicket().getId() : null)
                .build();
    }

    private PaymentDto.SecurityDepositDto mapDepositToDto(SecurityDeposit d) {
        return PaymentDto.SecurityDepositDto.builder()
                .id(d.getId())
                .bookingId(d.getBooking() != null ? d.getBooking().getId() : null)
                .bookingCode(d.getBooking() != null ? d.getBooking().getBookingCode() : null)
                .managerPartnerId(d.getManagerPartner() != null ? d.getManagerPartner().getId() : null)
                .partnerBusinessName(d.getManagerPartner() != null ? d.getManagerPartner().getBusinessName() : null)
                .customerUserId(d.getCustomerUser() != null ? d.getCustomerUser().getId() : null)
                .requiredAmount(d.getRequiredAmount())
                .collectedAmount(d.getCollectedAmount())
                .heldAmount(d.getHeldAmount())
                .deductedAmount(d.getDeductedAmount())
                .refundedAmount(d.getRefundedAmount())
                .status(d.getStatus() != null ? d.getStatus().name() : null)
                .collectedAt(d.getCollectedAt())
                .build();
    }

    private PaymentDto.ContributorPayableDto mapPayableToDto(ContributorPayable p) {
        return PaymentDto.ContributorPayableDto.builder()
                .id(p.getId())
                .bookingId(p.getBooking() != null ? p.getBooking().getId() : null)
                .bookingCode(p.getBooking() != null ? p.getBooking().getBookingCode() : null)
                .agreementId(p.getAgreement() != null ? p.getAgreement().getId() : null)
                .partnerId(p.getPartner() != null ? p.getPartner().getId() : null)
                .partnerBusinessName(p.getPartner() != null ? p.getPartner().getBusinessName() : null)
                .contributorUserId(p.getContributorUser() != null ? p.getContributorUser().getId() : null)
                .agreedCutAmount(p.getAgreedCutAmount())
                .payableStatus(p.getPayableStatus() != null ? p.getPayableStatus().name() : null)
                .partnerPaymentMethod(p.getPartnerPaymentMethod() != null ? p.getPartnerPaymentMethod().name() : null)
                .partnerTransactionReference(p.getPartnerTransactionReference())
                .settledAt(p.getSettledAt())
                .confirmedAt(p.getConfirmedAt())
                .disputeReason(p.getDisputeReason())
                .build();
    }
}
