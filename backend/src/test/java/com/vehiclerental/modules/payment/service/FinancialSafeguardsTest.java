package com.vehiclerental.modules.payment.service;

import com.vehiclerental.common.exception.BadRequestException;
import com.vehiclerental.modules.booking.entity.BookingHold;
import com.vehiclerental.modules.booking.repository.BookingHoldRepository;
import com.vehiclerental.modules.booking.service.BookingService;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.payment.dto.PaymentDto;
import com.vehiclerental.modules.payment.entity.RentalPaymentDemand;
import com.vehiclerental.modules.payment.entity.RentalPaymentSubmission;
import com.vehiclerental.modules.payment.repository.*;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialSafeguardsTest {

    @Mock
    private RentalPaymentDemandRepository demandRepository;
    @Mock
    private RentalPaymentSubmissionRepository submissionRepository;
    @Mock
    private RentalFeeRefundRepository refundRepository;
    @Mock
    private BookingHoldRepository holdRepository;
    @Mock
    private BookingService bookingService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private com.vehiclerental.modules.partner.service.PartnerService partnerService;
    @Mock
    private com.vehiclerental.modules.vehicle.service.VehicleService vehicleService;
    @Mock
    private SecurityDepositRepository depositRepository;
    @Mock
    private DepositDeductionRepository deductionRepository;
    @Mock
    private SecurityDepositRefundRepository depositRefundRepository;
    @Mock
    private ContributorPayableRepository contributorPayableRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private com.vehiclerental.modules.partner.repository.PartnerProfileRepository partnerProfileRepository;
    @Mock
    private com.vehiclerental.modules.partner.repository.PartnerPaymentProfileRepository partnerPaymentProfileRepository;
    @Mock
    private com.vehiclerental.modules.operations.repository.SupportTicketRepository supportTicketRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                demandRepository,
                submissionRepository,
                refundRepository,
                depositRepository,
                deductionRepository,
                depositRefundRepository,
                contributorPayableRepository,
                holdRepository,
                bookingRepository,
                partnerProfileRepository,
                partnerPaymentProfileRepository,
                userRepository,
                supportTicketRepository,
                bookingService,
                partnerService,
                vehicleService
        );
    }

    @Test
    @DisplayName("Safeguard B: Calculate refundable balance correctly accounting for verified amount, cancellation fee, and pending refunds")
    void testCalculateAvailableRefundableBalance() {
        Long demandId = 100L;
        RentalPaymentDemand demand = RentalPaymentDemand.builder()
                .id(demandId)
                .amountDemanded(new BigDecimal("3000.00"))
                .totalVerifiedSubmitted(new BigDecimal("3000.00"))
                .cancellationFeeRetained(new BigDecimal("300.00")) // 10% fee retained
                .build();

        when(demandRepository.findById(demandId)).thenReturn(Optional.of(demand));
        // There is an existing pending refund of 1000.00
        when(refundRepository.getCumulativeUnvoidedRefundAmountForDemand(demandId))
                .thenReturn(new BigDecimal("1000.00"));

        BigDecimal available = paymentService.calculateAvailableRefundableBalance(demandId);

        // Expected: 3000 - 300 - 1000 = 1700
        assertEquals(new BigDecimal("1700.00"), available);
    }

    @Test
    @DisplayName("Safeguard A: Successfully reapply verified payment from expired demand to valid new hold")
    void testReapplyPaymentSuccess() {
        Long customerId = 1L;
        Long subId = 50L;
        Long newHoldId = 200L;

        User customer = new User();
        customer.setId(customerId);

        RentalPaymentDemand oldDemand = RentalPaymentDemand.builder()
                .id(10L)
                .status(RentalPaymentDemand.DemandStatus.EXPIRED)
                .build();

        RentalPaymentSubmission submission = RentalPaymentSubmission.builder()
                .id(subId)
                .customerUser(customer)
                .submissionStatus(RentalPaymentSubmission.SubmissionStatus.VERIFIED)
                .amountSubmitted(new BigDecimal("2500.00"))
                .reappliedAmount(BigDecimal.ZERO)
                .demand(oldDemand)
                .build();

        BookingHold newHold = BookingHold.builder()
                .id(newHoldId)
                .status(BookingHold.HoldStatus.ACTIVE)
                .build();

        RentalPaymentDemand newDemand = RentalPaymentDemand.builder()
                .id(20L)
                .hold(newHold)
                .amountDemanded(new BigDecimal("2500.00"))
                .totalVerifiedSubmitted(BigDecimal.ZERO)
                .amountSatisfied(BigDecimal.ZERO)
                .excessAmount(BigDecimal.ZERO)
                .status(RentalPaymentDemand.DemandStatus.OPEN)
                .build();

        when(submissionRepository.findById(subId)).thenReturn(Optional.of(submission));
        when(holdRepository.findByIdWithLock(newHoldId)).thenReturn(Optional.of(newHold));
        when(demandRepository.findByHoldId(newHoldId)).thenReturn(Optional.of(newDemand));

        PaymentDto.ReapplyPaymentRequest req = PaymentDto.ReapplyPaymentRequest.builder()
                .originalSubmissionId(subId)
                .newHoldId(newHoldId)
                .build();

        PaymentDto.PaymentSubmissionDto result = paymentService.reapplyPaymentToNewHold(customerId, req);

        assertNotNull(result);
        assertEquals(new BigDecimal("2500.00"), submission.getReappliedAmount());
        assertEquals(newDemand, submission.getRecoveredForDemand());
        assertEquals(new BigDecimal("2500.00"), newDemand.getAmountSatisfied());
        assertEquals(RentalPaymentDemand.DemandStatus.SATISFIED, newDemand.getStatus());
        verify(submissionRepository, times(1)).save(submission);
        verify(demandRepository, times(1)).save(newDemand);
    }

    @Test
    @DisplayName("Safeguard A: Prevent reuse of same verified payment more than once")
    void testPreventDoubleReapplication() {
        Long customerId = 1L;
        Long subId = 50L;
        Long newHoldId = 200L;

        User customer = new User();
        customer.setId(customerId);

        // Already reapplied previously
        RentalPaymentSubmission submission = RentalPaymentSubmission.builder()
                .id(subId)
                .customerUser(customer)
                .submissionStatus(RentalPaymentSubmission.SubmissionStatus.VERIFIED)
                .amountSubmitted(new BigDecimal("2500.00"))
                .reappliedAmount(new BigDecimal("2500.00")) // already used!
                .build();

        when(submissionRepository.findById(subId)).thenReturn(Optional.of(submission));

        PaymentDto.ReapplyPaymentRequest req = PaymentDto.ReapplyPaymentRequest.builder()
                .originalSubmissionId(subId)
                .newHoldId(newHoldId)
                .build();

        assertThrows(BadRequestException.class, () -> paymentService.reapplyPaymentToNewHold(customerId, req));
    }
}
