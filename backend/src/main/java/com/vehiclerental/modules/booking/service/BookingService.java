package com.vehiclerental.modules.booking.service;

import com.vehiclerental.common.exception.BadRequestException;
import com.vehiclerental.common.exception.ResourceNotFoundException;
import com.vehiclerental.modules.booking.dto.BookingDto;
import com.vehiclerental.modules.booking.entity.*;
import com.vehiclerental.modules.booking.repository.*;
import com.vehiclerental.modules.contributor.entity.ContributionAgreement;
import com.vehiclerental.modules.contributor.repository.ContributionAgreementRepository;
import com.vehiclerental.modules.custody.entity.VehicleCustodyEvent;
import com.vehiclerental.modules.custody.repository.VehicleCustodyEventRepository;
import com.vehiclerental.modules.location.entity.PickupLocation;
import com.vehiclerental.modules.location.repository.PickupLocationRepository;
import com.vehiclerental.modules.partner.entity.PartnerPaymentProfile;
import com.vehiclerental.modules.partner.repository.PartnerPaymentProfileRepository;
import com.vehiclerental.modules.payment.entity.*;
import com.vehiclerental.modules.payment.repository.*;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import com.vehiclerental.modules.vehicle.entity.Vehicle;
import com.vehiclerental.modules.vehicle.entity.VehiclePricingProfile;
import com.vehiclerental.modules.vehicle.repository.VehiclePricingProfileRepository;
import com.vehiclerental.modules.vehicle.repository.VehicleRepository;
import com.vehiclerental.modules.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final VehicleRepository vehicleRepository;
    private final VehiclePricingProfileRepository pricingRepository;
    private final BookingHoldRepository holdRepository;
    private final BookingRepository bookingRepository;
    private final BookingStatusEventRepository statusEventRepository;
    private final BookingExtensionRepository extensionRepository;
    private final InspectionRepository inspectionRepository;
    private final InspectionMediaRepository inspectionMediaRepository;
    private final RentalPaymentDemandRepository demandRepository;
    private final SecurityDepositRepository depositRepository;
    private final RentalFeeRefundRepository refundRepository;
    private final ContributorPayableRepository contributorPayableRepository;
    private final ContributionAgreementRepository agreementRepository;
    private final VehicleCustodyEventRepository custodyEventRepository;
    private final PickupLocationRepository pickupLocationRepository;
    private final PartnerPaymentProfileRepository paymentProfileRepository;
    private final UserRepository userRepository;
    private final VehicleService vehicleService;

    @Value("${app.booking.hold-duration-minutes:15}")
    private int holdDurationMinutes;

    @Transactional
    public BookingDto.HoldResponseDto createBookingHold(Long userId, BookingDto.CreateHoldRequest request) {
        if (request.getEndTime().isBefore(request.getStartTime()) ||
            Duration.between(request.getStartTime(), request.getEndTime()).toHours() < 1) {
            throw new BadRequestException("Rental must be for at least 1 hour and end time must be after start time");
        }

        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // 1. Pessimistic lock on the vehicle row to prevent concurrent hold/booking races
        Vehicle vehicle = vehicleRepository.findByIdWithLock(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + request.getVehicleId()));

        if (vehicle.getListingStatus() != Vehicle.ListingStatus.PUBLISHED) {
            throw new BadRequestException("Vehicle is not available for rental");
        }

        // 2. Validate availability under vehicle lock
        boolean available = vehicleService.isVehicleAvailable(vehicle.getId(), request.getStartTime(), request.getEndTime(), null, null);
        if (!available) {
            throw new BadRequestException("Vehicle is not available for the requested time interval (including turnaround buffers)");
        }

        PickupLocation pickupLoc = pickupLocationRepository.findById(request.getPickupLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Pickup location not found"));
        PickupLocation dropoffLoc = pickupLocationRepository.findById(request.getDropoffLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Dropoff location not found"));

        VehiclePricingProfile pricing = pricingRepository.findByVehicleId(vehicle.getId())
                .orElseThrow(() -> new BadRequestException("Pricing profile not configured for vehicle"));

        // Calculate pricing
        Duration duration = Duration.between(request.getStartTime(), request.getEndTime());
        long hours = duration.toHours();
        if (duration.toMinutes() % 60 != 0) hours++; // round up partial hour

        BigDecimal rentalAmount;
        if (hours >= 24) {
            long days = hours / 24;
            long remHours = hours % 24;
            rentalAmount = pricing.getDailyRate().multiply(BigDecimal.valueOf(days))
                    .add(pricing.getHourlyRate().multiply(BigDecimal.valueOf(remHours)));
        } else {
            rentalAmount = pricing.getHourlyRate().multiply(BigDecimal.valueOf(hours));
        }

        BigDecimal depositAmount = pricing.getSecurityDepositAmount();
        BigDecimal surcharge = (pickupLoc.getDoorstepDeliverySurcharge() != null ? pickupLoc.getDoorstepDeliverySurcharge() : BigDecimal.ZERO)
                .add(dropoffLoc.getDoorstepDeliverySurcharge() != null ? dropoffLoc.getDoorstepDeliverySurcharge() : BigDecimal.ZERO);
        BigDecimal totalHoldAmount = rentalAmount.add(depositAmount).add(surcharge);

        String holdRef = "HLD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BookingHold hold = BookingHold.builder()
                .holdReference(holdRef)
                .vehicle(vehicle)
                .customerUser(customer)
                .pickupLocation(pickupLoc)
                .dropoffLocation(dropoffLoc)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .quotedRentalAmount(rentalAmount)
                .quotedDepositAmount(depositAmount)
                .quotedSurchargeAmount(surcharge)
                .totalHoldAmount(totalHoldAmount)
                .status(BookingHold.HoldStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(holdDurationMinutes))
                .build();
        hold = holdRepository.save(hold);

        // Auto-generate initial RentalPaymentDemand for confirmation
        String demandRef = "DEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        RentalPaymentDemand demand = RentalPaymentDemand.builder()
                .demandReference(demandRef)
                .hold(hold)
                .managerPartner(vehicle.getManagerPartner())
                .customerUser(customer)
                .demandType(RentalPaymentDemand.DemandType.BOOKING_CONFIRMATION)
                .amountDemanded(rentalAmount.add(surcharge))
                .totalVerifiedSubmitted(BigDecimal.ZERO)
                .amountSatisfied(BigDecimal.ZERO)
                .excessAmount(BigDecimal.ZERO)
                .cancellationFeeRetained(BigDecimal.ZERO)
                .currency("INR")
                .status(RentalPaymentDemand.DemandStatus.OPEN)
                .build();
        demand = demandRepository.save(demand);

        PartnerPaymentProfile ppp = paymentProfileRepository.findByPartnerId(vehicle.getManagerPartner().getId()).orElse(null);

        return BookingDto.HoldResponseDto.builder()
                .id(hold.getId())
                .holdReference(hold.getHoldReference())
                .vehicleId(vehicle.getId())
                .vehicleName(vehicle.getMake() + " " + vehicle.getModel())
                .vehicleRegistration(vehicle.getRegistrationNumber())
                .customerUserId(customer.getId())
                .pickupLocationId(pickupLoc.getId())
                .pickupLocationName(pickupLoc.getLocationName())
                .dropoffLocationId(dropoffLoc.getId())
                .dropoffLocationName(dropoffLoc.getLocationName())
                .startTime(hold.getStartTime())
                .endTime(hold.getEndTime())
                .quotedRentalAmount(hold.getQuotedRentalAmount())
                .quotedDepositAmount(hold.getQuotedDepositAmount())
                .quotedSurchargeAmount(hold.getQuotedSurchargeAmount())
                .totalHoldAmount(hold.getTotalHoldAmount())
                .status(hold.getStatus().name())
                .expiresAt(hold.getExpiresAt())
                .initialDemandId(demand.getId())
                .initialDemandReference(demand.getDemandReference())
                .partnerId(vehicle.getManagerPartner().getId())
                .partnerBusinessName(vehicle.getManagerPartner().getBusinessName())
                .partnerUpiId(ppp != null ? ppp.getUpiId() : "")
                .partnerQrKey(ppp != null ? ppp.getUpiQrObjectKey() : null)
                .cashAtPickupEnabled(ppp != null && ppp.isCashAtPickupEnabled())
                .build();
    }

    @Transactional
    public BookingDto.BookingResponseDto confirmBookingFromHold(Long holdId) {
        BookingHold hold = holdRepository.findByIdWithLock(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("Hold not found"));

        if (hold.getStatus() == BookingHold.HoldStatus.CONVERTED) {
            Booking existing = bookingRepository.findByHoldId(hold.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found for converted hold"));
            return mapBookingToDto(existing);
        }

        if (hold.getStatus() == BookingHold.HoldStatus.EXPIRED || hold.getStatus() == BookingHold.HoldStatus.RELEASED) {
            throw new BadRequestException("Hold has expired or was released");
        }

        Vehicle vehicle = vehicleRepository.findByIdWithLock(hold.getVehicle().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        RentalPaymentDemand initialDemand = demandRepository.findByHoldId(hold.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Demand not found for hold"));

        hold.setStatus(BookingHold.HoldStatus.CONVERTED);
        holdRepository.save(hold);

        String bookingCode = "BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .hold(hold)
                .vehicle(vehicle)
                .customerUser(hold.getCustomerUser())
                .managerPartner(vehicle.getManagerPartner())
                .pickupLocation(hold.getPickupLocation())
                .dropoffLocation(hold.getDropoffLocation())
                .startTime(hold.getStartTime())
                .endTime(hold.getEndTime())
                .baseRentalAmount(hold.getQuotedRentalAmount())
                .securityDepositAmount(hold.getQuotedDepositAmount())
                .deliverySurchargeAmount(hold.getQuotedSurchargeAmount())
                .totalAmountPayable(hold.getTotalHoldAmount())
                .initialDemand(initialDemand)
                .bookingStatus(Booking.BookingStatus.CONFIRMED)
                .build();
        booking = bookingRepository.save(booking);

        // Create SecurityDeposit tracking record
        SecurityDeposit deposit = SecurityDeposit.builder()
                .booking(booking)
                .managerPartner(vehicle.getManagerPartner())
                .customerUser(hold.getCustomerUser())
                .requiredAmount(hold.getQuotedDepositAmount())
                .collectedAmount(BigDecimal.ZERO)
                .heldAmount(BigDecimal.ZERO)
                .deductedAmount(BigDecimal.ZERO)
                .refundedAmount(BigDecimal.ZERO)
                .currency("INR")
                .collectionMethod(SecurityDeposit.CollectionMethod.UPI_TRANSFER)
                .status(SecurityDeposit.DepositStatus.PENDING_COLLECTION)
                .build();
        depositRepository.save(deposit);

        // Record status event
        BookingStatusEvent evt = BookingStatusEvent.builder()
                .booking(booking)
                .fromStatus(null)
                .toStatus(Booking.BookingStatus.CONFIRMED)
                .actorUser(hold.getCustomerUser())
                .transitionReason("Booking confirmed via verified payment or partner authorization")
                .build();
        statusEventRepository.save(evt);

        return mapBookingToDto(booking);
    }

    @Transactional
    public BookingDto.BookingResponseDto cancelBooking(Long userId, Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getCustomerUser().getId().equals(userId) && !booking.getManagerPartner().getUser().getId().equals(userId)) {
            throw new BadRequestException("Unauthorized to cancel this booking");
        }

        if (booking.getBookingStatus() != Booking.BookingStatus.CONFIRMED && booking.getBookingStatus() != Booking.BookingStatus.READY_FOR_PICKUP) {
            throw new BadRequestException("Only confirmed or ready for pickup bookings can be cancelled");
        }

        Booking.BookingStatus prev = booking.getBookingStatus();
        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Record event
        User actor = userRepository.findById(userId).orElse(booking.getCustomerUser());
        BookingStatusEvent evt = BookingStatusEvent.builder()
                .booking(booking)
                .fromStatus(prev)
                .toStatus(Booking.BookingStatus.CANCELLED)
                .actorUser(actor)
                .transitionReason(reason != null ? reason : "Customer cancellation")
                .build();
        statusEventRepository.save(evt);

        // Release hold
        BookingHold hold = booking.getHold();
        if (hold != null) {
            hold.setStatus(BookingHold.HoldStatus.RELEASED);
            holdRepository.save(hold);
        }

        // Automatic refund obligation calculation
        RentalPaymentDemand demand = booking.getInitialDemand();
        if (demand != null && demand.getTotalVerifiedSubmitted().compareTo(BigDecimal.ZERO) > 0) {
            // Apply cancellation policy: 10% cancellation fee retained if within 24 hours of start
            BigDecimal cancellationFee = BigDecimal.ZERO;
            if (Duration.between(LocalDateTime.now(), booking.getStartTime()).toHours() < 24) {
                cancellationFee = demand.getTotalVerifiedSubmitted().multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            }
            demand.setCancellationFeeRetained(cancellationFee);
            demandRepository.save(demand);

            BigDecimal refundable = demand.getTotalVerifiedSubmitted().subtract(cancellationFee);
            if (refundable.compareTo(BigDecimal.ZERO) > 0) {
                String refundRef = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                RentalFeeRefund refund = RentalFeeRefund.builder()
                        .refundReference(refundRef)
                        .booking(booking)
                        .demand(demand)
                        .hold(hold)
                        .managerPartner(booking.getManagerPartner())
                        .customerUser(booking.getCustomerUser())
                        .refundReason(RentalFeeRefund.RefundReason.BOOKING_CANCELLATION)
                        .grossAmountPaid(demand.getTotalVerifiedSubmitted())
                        .cancellationFeeDeducted(cancellationFee)
                        .refundAmount(refundable)
                        .currency("INR")
                        .refundStatus(RentalFeeRefund.RefundStatus.PENDING_PARTNER_DISBURSEMENT)
                        .refundMethod(RentalFeeRefund.RefundMethod.UPI_TRANSFER)
                        .recordedByUser(actor)
                        .build();
                refundRepository.save(refund);
            }
        }

        return mapBookingToDto(booking);
    }

    @Transactional
    public BookingDto.InspectionResponseDto conductInspection(Long userId, BookingDto.SubmitInspectionRequest request) {
        User inspector = userRepository.findById(userId).orElseThrow();
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        Vehicle vehicle = booking.getVehicle();

        Inspection inspection = Inspection.builder()
                .booking(booking)
                .vehicle(vehicle)
                .inspectionType(Inspection.InspectionType.valueOf(request.getInspectionType()))
                .odometerReadingKm(request.getOdometerReadingKm())
                .fuelOrBatteryLevelPercent(request.getFuelOrBatteryLevelPercent())
                .exteriorConditionNotes(request.getExteriorConditionNotes())
                .interiorConditionNotes(request.getInteriorConditionNotes())
                .knownScratchesOrDents(request.getKnownScratchesOrDents())
                .inspectorUser(inspector)
                .customerSignatureConfirmed(request.isCustomerSignatureConfirmed())
                .partnerSignatureConfirmed(request.isPartnerSignatureConfirmed())
                .conductedAt(LocalDateTime.now())
                .hub(vehicle.getAssignedHub())
                .build();
        inspection = inspectionRepository.save(inspection);

        if (request.getMedia() != null) {
            for (BookingDto.InspectionMediaItem item : request.getMedia()) {
                InspectionMedia m = InspectionMedia.builder()
                        .inspection(inspection)
                        .viewAngle(InspectionMedia.ViewAngle.valueOf(item.getViewAngle()))
                        .storageObjectKey(item.getStorageObjectKey())
                        .caption(item.getCaption())
                        .build();
                inspectionMediaRepository.save(m);
            }
        }

        // Update vehicle state and booking status based on handover
        if (inspection.getInspectionType() == Inspection.InspectionType.PRE_RENTAL_HANDOVER) {
            booking.setBookingStatus(Booking.BookingStatus.IN_PROGRESS);
            bookingRepository.save(booking);

            vehicle.setCurrentCustodyStatus(Vehicle.CustodyStatus.CUSTOMER_CUSTODY);
            vehicle.setOdometerReadingKm(request.getOdometerReadingKm());
            vehicle.setCurrentFuelOrBatteryLevelPercent(request.getFuelOrBatteryLevelPercent());
            vehicleRepository.save(vehicle);

            // Record Custody Event
            VehicleCustodyEvent ce = VehicleCustodyEvent.builder()
                    .vehicle(vehicle)
                    .booking(booking)
                    .handoverType(VehicleCustodyEvent.HandoverType.PARTNER_TO_CUSTOMER)
                    .fromCustodianUser(booking.getManagerPartner().getUser())
                    .toCustodianUser(booking.getCustomerUser())
                    .fromPartnerHub(vehicle.getAssignedHub())
                    .handoverAddress(booking.getPickupLocation().getAddress())
                    .actorUser(inspector)
                    .odometerReadingKm(request.getOdometerReadingKm())
                    .fuelOrBatteryLevelPercent(request.getFuelOrBatteryLevelPercent())
                    .custodyNotes("Vehicle handed over to customer after pre-rental inspection")
                    .fromPartyConfirmed(true)
                    .toPartyConfirmed(true)
                    .build();
            custodyEventRepository.save(ce);

        } else if (inspection.getInspectionType() == Inspection.InspectionType.POST_RENTAL_RETURN) {
            booking.setBookingStatus(Booking.BookingStatus.COMPLETED);
            booking.setActualReturnTime(LocalDateTime.now());
            bookingRepository.save(booking);

            vehicle.setCurrentCustodyStatus(Vehicle.CustodyStatus.PARTNER_CUSTODY);
            vehicle.setOdometerReadingKm(request.getOdometerReadingKm());
            vehicle.setCurrentFuelOrBatteryLevelPercent(request.getFuelOrBatteryLevelPercent());
            vehicleRepository.save(vehicle);

            // Record Custody Event
            VehicleCustodyEvent ce = VehicleCustodyEvent.builder()
                    .vehicle(vehicle)
                    .booking(booking)
                    .handoverType(VehicleCustodyEvent.HandoverType.CUSTOMER_TO_PARTNER)
                    .fromCustodianUser(booking.getCustomerUser())
                    .toCustodianUser(booking.getManagerPartner().getUser())
                    .toPartnerHub(vehicle.getAssignedHub())
                    .handoverAddress(booking.getDropoffLocation().getAddress())
                    .actorUser(inspector)
                    .odometerReadingKm(request.getOdometerReadingKm())
                    .fuelOrBatteryLevelPercent(request.getFuelOrBatteryLevelPercent())
                    .custodyNotes("Vehicle returned by customer after post-rental inspection")
                    .fromPartyConfirmed(true)
                    .toPartyConfirmed(true)
                    .build();
            custodyEventRepository.save(ce);

            // If vehicle is contributor-owned with an active agreement, generate ContributorPayable
            agreementRepository.findByVehicleId(vehicle.getId()).ifPresent(agreement -> {
                if (agreement.getStatus() == ContributionAgreement.AgreementStatus.ACTIVE) {
                    ContributorPayable payable = ContributorPayable.builder()
                            .booking(booking)
                            .agreement(agreement)
                            .partner(booking.getManagerPartner())
                            .contributorUser(agreement.getContributor().getUser())
                            .agreedCutAmount(agreement.getFixedContributorCutPerBooking())
                            .payableStatus(ContributorPayable.PayableStatus.ELIGIBLE)
                            .build();
                    contributorPayableRepository.save(payable);
                }
            });
        }

        return BookingDto.InspectionResponseDto.builder()
                .id(inspection.getId())
                .bookingId(booking.getId())
                .vehicleId(vehicle.getId())
                .inspectionType(inspection.getInspectionType().name())
                .odometerReadingKm(inspection.getOdometerReadingKm())
                .fuelOrBatteryLevelPercent(inspection.getFuelOrBatteryLevelPercent())
                .exteriorConditionNotes(inspection.getExteriorConditionNotes())
                .interiorConditionNotes(inspection.getInteriorConditionNotes())
                .knownScratchesOrDents(inspection.getKnownScratchesOrDents())
                .customerSignatureConfirmed(inspection.getCustomerSignatureConfirmed())
                .partnerSignatureConfirmed(inspection.getPartnerSignatureConfirmed())
                .conductedAt(inspection.getConductedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<BookingDto.BookingResponseDto> getCustomerBookings(Long userId, Pageable pageable) {
        return bookingRepository.findByCustomerUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapBookingToDto);
    }

    @Transactional(readOnly = true)
    public Page<BookingDto.BookingResponseDto> getPartnerBookings(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow();
        // find partner profile
        var partner = bookingRepository.findAll(pageable); // will filter
        return partner.map(this::mapBookingToDto);
    }

    @Transactional(readOnly = true)
    public BookingDto.BookingResponseDto getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return mapBookingToDto(booking);
    }

    @Transactional(readOnly = true)
    public BookingDto.HoldResponseDto getHoldById(Long holdId) {
        BookingHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("Hold not found"));
        RentalPaymentDemand demand = demandRepository.findByHoldId(hold.getId()).orElse(null);
        PartnerPaymentProfile ppp = paymentProfileRepository.findByPartnerId(hold.getVehicle().getManagerPartner().getId()).orElse(null);

        return BookingDto.HoldResponseDto.builder()
                .id(hold.getId())
                .holdReference(hold.getHoldReference())
                .vehicleId(hold.getVehicle().getId())
                .vehicleName(hold.getVehicle().getMake() + " " + hold.getVehicle().getModel())
                .vehicleRegistration(hold.getVehicle().getRegistrationNumber())
                .customerUserId(hold.getCustomerUser().getId())
                .pickupLocationId(hold.getPickupLocation().getId())
                .pickupLocationName(hold.getPickupLocation().getLocationName())
                .dropoffLocationId(hold.getDropoffLocation().getId())
                .dropoffLocationName(hold.getDropoffLocation().getLocationName())
                .startTime(hold.getStartTime())
                .endTime(hold.getEndTime())
                .quotedRentalAmount(hold.getQuotedRentalAmount())
                .quotedDepositAmount(hold.getQuotedDepositAmount())
                .quotedSurchargeAmount(hold.getQuotedSurchargeAmount())
                .totalHoldAmount(hold.getTotalHoldAmount())
                .status(hold.getStatus().name())
                .expiresAt(hold.getExpiresAt())
                .initialDemandId(demand != null ? demand.getId() : null)
                .initialDemandReference(demand != null ? demand.getDemandReference() : null)
                .partnerId(hold.getVehicle().getManagerPartner().getId())
                .partnerBusinessName(hold.getVehicle().getManagerPartner().getBusinessName())
                .partnerUpiId(ppp != null ? ppp.getUpiId() : "")
                .partnerQrKey(ppp != null ? ppp.getUpiQrObjectKey() : null)
                .cashAtPickupEnabled(ppp != null && ppp.isCashAtPickupEnabled())
                .build();
    }

    private BookingDto.BookingResponseDto mapBookingToDto(Booking b) {
        return BookingDto.BookingResponseDto.builder()
                .id(b.getId())
                .bookingCode(b.getBookingCode())
                .holdId(b.getHold().getId())
                .vehicleId(b.getVehicle().getId())
                .vehicleName(b.getVehicle().getMake() + " " + b.getVehicle().getModel())
                .vehicleRegistration(b.getVehicle().getRegistrationNumber())
                .customerUserId(b.getCustomerUser().getId())
                .customerName(b.getCustomerUser().getFullName())
                .customerPhone(b.getCustomerUser().getPhoneNumber())
                .managerPartnerId(b.getManagerPartner().getId())
                .partnerBusinessName(b.getManagerPartner().getBusinessName())
                .pickupLocationId(b.getPickupLocation().getId())
                .pickupLocationName(b.getPickupLocation().getLocationName())
                .dropoffLocationId(b.getDropoffLocation().getId())
                .dropoffLocationName(b.getDropoffLocation().getLocationName())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .actualReturnTime(b.getActualReturnTime())
                .baseRentalAmount(b.getBaseRentalAmount())
                .securityDepositAmount(b.getSecurityDepositAmount())
                .deliverySurchargeAmount(b.getDeliverySurchargeAmount())
                .totalAmountPayable(b.getTotalAmountPayable())
                .bookingStatus(b.getBookingStatus().name())
                .cancellationReason(b.getCancellationReason())
                .cancelledAt(b.getCancelledAt())
                .createdAt(b.getCreatedAt())
                .initialDemandId(b.getInitialDemand() != null ? b.getInitialDemand().getId() : null)
                .initialDemandReference(b.getInitialDemand() != null ? b.getInitialDemand().getDemandReference() : null)
                .demandStatus(b.getInitialDemand() != null ? b.getInitialDemand().getStatus().name() : null)
                .build();
    }
}
