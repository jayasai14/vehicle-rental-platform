package com.vehiclerental.modules.operations.service;

import com.vehiclerental.common.exception.ResourceNotFoundException;
import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.booking.repository.BookingRepository;
import com.vehiclerental.modules.operations.dto.OperationsDto;
import com.vehiclerental.modules.operations.entity.SupportTicket;
import com.vehiclerental.modules.operations.repository.SupportTicketRepository;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.partner.repository.PartnerProfileRepository;
import com.vehiclerental.modules.payment.entity.DepositDeduction;
import com.vehiclerental.modules.payment.entity.RentalFeeRefund;
import com.vehiclerental.modules.payment.repository.DepositDeductionRepository;
import com.vehiclerental.modules.payment.repository.RentalFeeRefundRepository;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import com.vehiclerental.modules.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final SupportTicketRepository ticketRepository;
    private final RentalFeeRefundRepository refundRepository;
    private final DepositDeductionRepository deductionRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public OperationsDto.AdminPlatformStatsDto getPlatformStats() {
        long totalUsers = userRepository.count();
        long totalPartners = partnerProfileRepository.count();
        long totalVehicles = vehicleRepository.count();
        long totalBookings = bookingRepository.count();
        long pendingPartners = partnerProfileRepository.findByVerificationStatus(PartnerProfile.PartnerVerificationStatus.SUBMITTED).size();
        long openTickets = ticketRepository.findAll().stream().filter(t -> t.getStatus() == SupportTicket.TicketStatus.OPEN).count();

        BigDecimal totalVolume = bookingRepository.findAll().stream()
                .filter(b -> b.getBookingStatus() != Booking.BookingStatus.CANCELLED)
                .map(Booking::getTotalAmountPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OperationsDto.AdminPlatformStatsDto.builder()
                .totalUsers(totalUsers)
                .totalPartners(totalPartners)
                .totalVehicles(totalVehicles)
                .totalBookings(totalBookings)
                .pendingPartnerApplications(pendingPartners)
                .openSupportTickets(openTickets)
                .totalRentalVolume(totalVolume)
                .build();
    }

    @Transactional
    public void arbitrateRefundDispute(Long adminUserId, Long refundId, OperationsDto.ResolveDisputeRequest request) {
        User admin = userRepository.findById(adminUserId).orElseThrow();
        RentalFeeRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));

        refund.setRefundStatus(RentalFeeRefund.RefundStatus.ADMIN_RESOLVED);
        refund.setResolvedByAdminUser(admin);
        refund.setResolutionNotes(request.getResolutionNotes());
        refund.setResolvedAt(LocalDateTime.now());
        if (request.getApprovedRefundOrDeductionAmount() != null) {
            refund.setRefundAmount(request.getApprovedRefundOrDeductionAmount());
        }
        refundRepository.save(refund);

        if (refund.getDisputeTicket() != null) {
            refund.getDisputeTicket().setStatus(SupportTicket.TicketStatus.RESOLVED);
            ticketRepository.save(refund.getDisputeTicket());
        }

        auditLogService.recordAction(adminUserId, "ARBITRATE_REFUND", "rental_fee_refunds", String.valueOf(refundId), request.getResolutionNotes());
    }

    @Transactional
    public void arbitrateDepositDeduction(Long adminUserId, Long deductionId, OperationsDto.ResolveDisputeRequest request) {
        User admin = userRepository.findById(adminUserId).orElseThrow();
        DepositDeduction deduction = deductionRepository.findById(deductionId)
                .orElseThrow(() -> new ResourceNotFoundException("Deduction not found"));

        deduction.setStatus(DepositDeduction.DeductionStatus.APPROVED_BY_ADMIN);
        deduction.setResolvedByAdminUser(admin);
        deduction.setResolutionNotes(request.getResolutionNotes());
        deduction.setResolvedAt(LocalDateTime.now());
        if (request.getApprovedRefundOrDeductionAmount() != null) {
            deduction.setApprovedDeductionAmount(request.getApprovedRefundOrDeductionAmount());
        } else {
            deduction.setApprovedDeductionAmount(deduction.getProposedDeductionAmount());
        }
        deductionRepository.save(deduction);

        if (deduction.getDisputeTicket() != null) {
            deduction.getDisputeTicket().setStatus(SupportTicket.TicketStatus.RESOLVED);
            ticketRepository.save(deduction.getDisputeTicket());
        }

        auditLogService.recordAction(adminUserId, "ARBITRATE_DEPOSIT_DEDUCTION", "deposit_deductions", String.valueOf(deductionId), request.getResolutionNotes());
    }

    @Transactional(readOnly = true)
    public List<com.vehiclerental.modules.payment.dto.PaymentDto.RentalFeeRefundDto> getDisputedRefunds() {
        return refundRepository.findByRefundStatus(RentalFeeRefund.RefundStatus.DISPUTED_BY_CUSTOMER).stream()
                .map(r -> com.vehiclerental.modules.payment.dto.PaymentDto.RentalFeeRefundDto.builder()
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
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OperationsDto.DisputedDepositDto> getDisputedDeposits() {
        List<DepositDeduction> deductions = deductionRepository.findByStatus(DepositDeduction.DeductionStatus.PROPOSED);
        return deductions.stream()
                .map(d -> OperationsDto.DisputedDepositDto.builder()
                        .id(d.getId())
                        .depositId(d.getDeposit().getId())
                        .bookingId(d.getDeposit().getBooking() != null ? d.getDeposit().getBooking().getId() : null)
                        .bookingCode(d.getDeposit().getBooking() != null ? d.getDeposit().getBooking().getBookingCode() : null)
                        .deductionReason(d.getDeductionReason().name())
                        .proposedDeductionAmount(d.getProposedDeductionAmount())
                        .status(d.getStatus().name())
                        .evidenceDocumentKey(d.getEvidenceDocumentKey())
                        .build())
                .collect(Collectors.toList());
    }
}
