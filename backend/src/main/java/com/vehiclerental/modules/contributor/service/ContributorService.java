package com.vehiclerental.modules.contributor.service;

import com.vehiclerental.common.exception.BadRequestException;
import com.vehiclerental.common.exception.ResourceNotFoundException;
import com.vehiclerental.modules.contributor.dto.ContributorDto;
import com.vehiclerental.modules.contributor.entity.ContributionAgreement;
import com.vehiclerental.modules.contributor.entity.ContributionAgreementEvent;
import com.vehiclerental.modules.contributor.entity.ContributorProfile;
import com.vehiclerental.modules.contributor.entity.ContributorVehicleApplication;
import com.vehiclerental.modules.contributor.repository.ContributionAgreementEventRepository;
import com.vehiclerental.modules.contributor.repository.ContributionAgreementRepository;
import com.vehiclerental.modules.contributor.repository.ContributorProfileRepository;
import com.vehiclerental.modules.contributor.repository.ContributorVehicleApplicationRepository;
import com.vehiclerental.modules.location.entity.City;
import com.vehiclerental.modules.location.repository.CityRepository;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.partner.repository.PartnerProfileRepository;
import com.vehiclerental.modules.user.entity.Address;
import com.vehiclerental.modules.user.entity.Role;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.AddressRepository;
import com.vehiclerental.modules.user.repository.RoleRepository;
import com.vehiclerental.modules.user.repository.UserRepository;
import com.vehiclerental.modules.vehicle.entity.Vehicle;
import com.vehiclerental.modules.vehicle.entity.VehiclePricingProfile;
import com.vehiclerental.modules.vehicle.repository.VehiclePricingProfileRepository;
import com.vehiclerental.modules.vehicle.repository.VehicleRepository;
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
public class ContributorService {

    private final ContributorProfileRepository contributorProfileRepository;
    private final ContributorVehicleApplicationRepository applicationRepository;
    private final ContributionAgreementRepository agreementRepository;
    private final ContributionAgreementEventRepository agreementEventRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final VehicleRepository vehicleRepository;
    private final VehiclePricingProfileRepository vehiclePricingProfileRepository;

    @Transactional
    public ContributorDto.ContributorProfileDto createOrUpdateProfile(Long userId, ContributorDto.ContributorProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ContributorProfile profile = contributorProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ContributorProfile cp = new ContributorProfile();
                    cp.setUser(user);
                    return cp;
                });

        City city = request.getCityId() != null ? cityRepository.findById(request.getCityId()).orElse(null) : null;
        Address addr = profile.getResidentialAddress() != null ? profile.getResidentialAddress() : new Address();
        addr.setUser(user);
        addr.setCity(city != null ? city.getName() : "Vijayawada");
        addr.setAddressLine1(request.getAddressLine1());
        addr.setAddressLine2(request.getAddressLine2());
        addr.setPostalCode(request.getPostalCode());
        addr = addressRepository.save(addr);

        profile.setPanNumber(request.getPanNumber());
        profile.setBankAccountNumber(request.getBankAccountNumber());
        profile.setBankIfscCode(request.getBankIfscCode());
        profile.setBankName(request.getBankName());
        profile.setResidentialAddress(addr);
        profile.setActive(true);
        profile = contributorProfileRepository.save(profile);

        // Assign ROLE_VEHICLE_CONTRIBUTOR
        Role contributorRole = roleRepository.findByName("ROLE_VEHICLE_CONTRIBUTOR")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_VEHICLE_CONTRIBUTOR", "Contributor role")));
        user.getRoles().add(contributorRole);
        userRepository.save(user);

        return mapProfileToDto(profile);
    }

    @Transactional(readOnly = true)
    public ContributorDto.ContributorProfileDto getProfile(Long userId) {
        ContributorProfile profile = contributorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Contributor profile not found"));
        return mapProfileToDto(profile);
    }

    @Transactional
    public ContributorDto.ContributorApplicationDto submitApplication(Long userId, ContributorDto.VehicleApplicationRequest request) {
        ContributorProfile profile = contributorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Please complete your contributor profile before submitting a vehicle"));

        PartnerProfile partner = partnerProfileRepository.findById(request.getTargetPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Target partner not found with id: " + request.getTargetPartnerId()));

        City city = request.getPickupCityId() != null ? cityRepository.findById(request.getPickupCityId()).orElse(null) : null;
        Address pickupAddr = new Address();
        pickupAddr.setUser(profile.getUser());
        pickupAddr.setCity(city != null ? city.getName() : "Vijayawada");
        pickupAddr.setAddressLine1(request.getPickupAddressLine1());
        pickupAddr.setAddressLine2(request.getPickupAddressLine2());
        pickupAddr.setPostalCode(request.getPickupPostalCode());
        pickupAddr = addressRepository.save(pickupAddr);

        ContributorVehicleApplication app = new ContributorVehicleApplication();
        app.setContributor(profile);
        app.setTargetPartner(partner);
        app.setMake(request.getMake());
        app.setModel(request.getModel());
        app.setYearOfManufacture(request.getYearOfManufacture());
        app.setCategory(ContributorVehicleApplication.VehicleCategory.valueOf(request.getCategory()));
        app.setTransmission(request.getTransmission() != null ? ContributorVehicleApplication.TransmissionType.valueOf(request.getTransmission()) : ContributorVehicleApplication.TransmissionType.MANUAL);
        app.setFuelType(request.getFuelType() != null ? ContributorVehicleApplication.FuelType.valueOf(request.getFuelType()) : ContributorVehicleApplication.FuelType.PETROL);
        app.setRegistrationNumber(request.getRegistrationNumber());
        app.setVinChassisNumber(request.getVinChassisNumber());
        app.setEngineNumber(request.getEngineNumber());
        app.setCurrentOdometerKm(request.getCurrentOdometerKm() != null ? request.getCurrentOdometerKm() : 0);
        app.setVehiclePickupAddress(pickupAddr);
        app.setRcDocumentStorageKey(request.getRcDocumentStorageKey());
        app.setInsuranceDocumentStorageKey(request.getInsuranceDocumentStorageKey());
        app.setStatus(ContributorVehicleApplication.ApplicationStatus.SUBMITTED);
        app = applicationRepository.save(app);

        return mapApplicationToDto(app);
    }

    @Transactional(readOnly = true)
    public List<ContributorDto.ContributorApplicationDto> getContributorApplications(Long userId) {
        ContributorProfile profile = contributorProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return List.of();
        return applicationRepository.findByContributorId(profile.getId()).stream()
                .map(this::mapApplicationToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContributorDto.ContributorApplicationDto> getPartnerIncomingApplications(Long userId) {
        PartnerProfile partner = partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found"));
        return applicationRepository.findByTargetPartnerId(partner.getId()).stream()
                .map(this::mapApplicationToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContributorDto.AgreementDto proposeAgreement(Long userId, ContributorDto.ProposeAgreementRequest request) {
        PartnerProfile partner = partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found"));

        ContributorVehicleApplication app = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!app.getTargetPartner().getId().equals(partner.getId())) {
            throw new BadRequestException("Application is not targeted to this partner");
        }

        app.setStatus(ContributorVehicleApplication.ApplicationStatus.ACCEPTED);
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewedByUser(partner.getUser());
        applicationRepository.save(app);

        String code = "AGR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ContributionAgreement agreement = ContributionAgreement.builder()
                .application(app)
                .partner(partner)
                .contributor(app.getContributor())
                .agreementCode(code)
                .fixedContributorCutPerBooking(request.getFixedContributorCutPerBooking())
                .effectiveStartDate(request.getEffectiveStartDate())
                .effectiveEndDate(request.getEffectiveEndDate())
                .noticePeriodDays(request.getNoticePeriodDays() != null ? request.getNoticePeriodDays() : 7)
                .termsAndConditionsText(request.getTermsAndConditionsText())
                .status(ContributionAgreement.AgreementStatus.PENDING_CONTRIBUTOR_SIGNATURE)
                .partnerSignedAt(LocalDateTime.now())
                .build();
        agreement = agreementRepository.save(agreement);

        recordAgreementEvent(agreement, ContributionAgreementEvent.AgreementEventType.PROPOSED, partner.getUser(), "Agreement proposed with partner signature");

        return mapAgreementToDto(agreement);
    }

    @Transactional
    public ContributorDto.AgreementDto signAgreementByContributor(Long userId, Long agreementId) {
        User user = userRepository.findById(userId).orElseThrow();
        ContributionAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement not found"));

        if (!agreement.getContributor().getUser().getId().equals(userId)) {
            throw new BadRequestException("Only the vehicle contributor can sign this agreement");
        }

        agreement.setContributorSignedAt(LocalDateTime.now());
        agreement.setStatus(ContributionAgreement.AgreementStatus.ACTIVE);

        // When agreement becomes ACTIVE, list the vehicle in the system for the partner
        ContributorVehicleApplication app = agreement.getApplication();
        Vehicle vehicle = Vehicle.builder()
                .registrationNumber(app.getRegistrationNumber())
                .vinChassisNumber(app.getVinChassisNumber())
                .engineNumber(app.getEngineNumber())
                .make(app.getMake())
                .model(app.getModel())
                .yearOfManufacture(app.getYearOfManufacture())
                .category(Vehicle.VehicleCategory.valueOf(app.getCategory().name()))
                .transmission(Vehicle.TransmissionType.valueOf(app.getTransmission().name()))
                .fuelType(Vehicle.FuelType.valueOf(app.getFuelType().name()))
                .seatingCapacity(app.getCategory() == ContributorVehicleApplication.VehicleCategory.CAR ? 5 : 2)
                .odometerReadingKm(app.getCurrentOdometerKm())
                .currentFuelOrBatteryLevelPercent(100)
                .ownershipType(Vehicle.OwnershipType.INDIVIDUAL_OWNER)
                .legalOwnerUser(agreement.getContributor().getUser())
                .managerPartner(agreement.getPartner())
                .currentCustodyStatus(Vehicle.CustodyStatus.PARTNER_CUSTODY)
                .listingStatus(Vehicle.ListingStatus.PUBLISHED)
                .turnaroundBufferMinutes(60)
                .build();
        vehicle = vehicleRepository.save(vehicle);

        // Default pricing profile for contributor vehicle
        BigDecimal dailyRate = app.getCategory() == ContributorVehicleApplication.VehicleCategory.CAR ? new BigDecimal("2500.00") : new BigDecimal("700.00");
        BigDecimal hourlyRate = dailyRate.divide(new BigDecimal("10"), 2, java.math.RoundingMode.HALF_UP);
        VehiclePricingProfile pricing = VehiclePricingProfile.builder()
                .vehicle(vehicle)
                .hourlyRate(hourlyRate)
                .dailyRate(dailyRate)
                .weeklyRate(dailyRate.multiply(new BigDecimal("6.5")))
                .securityDepositAmount(app.getCategory() == ContributorVehicleApplication.VehicleCategory.CAR ? new BigDecimal("3000.00") : new BigDecimal("1000.00"))
                .freeKmPerDay(300)
                .extraKmCharge(new BigDecimal("12.00"))
                .lateReturnHourlyPenalty(new BigDecimal("200.00"))
                .isActive(true)
                .build();
        vehiclePricingProfileRepository.save(pricing);

        agreement.setVehicle(vehicle);
        agreement = agreementRepository.save(agreement);

        recordAgreementEvent(agreement, ContributionAgreementEvent.AgreementEventType.SIGNED_BY_CONTRIBUTOR, user, "Agreement signed by contributor and vehicle activated");

        return mapAgreementToDto(agreement);
    }

    @Transactional(readOnly = true)
    public List<ContributorDto.AgreementDto> getContributorAgreements(Long userId) {
        ContributorProfile profile = contributorProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return List.of();
        return agreementRepository.findByContributorId(profile.getId()).stream()
                .map(this::mapAgreementToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContributorDto.AgreementDto> getPartnerAgreements(Long userId) {
        PartnerProfile partner = partnerProfileRepository.findByUserId(userId).orElse(null);
        if (partner == null) return List.of();
        return agreementRepository.findByPartnerId(partner.getId()).stream()
                .map(this::mapAgreementToDto)
                .collect(Collectors.toList());
    }

    private void recordAgreementEvent(ContributionAgreement agreement, ContributionAgreementEvent.AgreementEventType type, User actor, String notes) {
        ContributionAgreementEvent evt = ContributionAgreementEvent.builder()
                .agreement(agreement)
                .eventType(type)
                .actor(actor)
                .eventNotes(notes)
                .eventTimestamp(LocalDateTime.now())
                .build();
        agreementEventRepository.save(evt);
    }

    private ContributorDto.ContributorProfileDto mapProfileToDto(ContributorProfile p) {
        return ContributorDto.ContributorProfileDto.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .panNumber(p.getPanNumber())
                .bankAccountNumber(p.getBankAccountNumber())
                .bankIfscCode(p.getBankIfscCode())
                .bankName(p.getBankName())
                .active(p.isActive())
                .address(p.getResidentialAddress() != null ? p.getResidentialAddress().getAddressLine1() : null)
                .build();
    }

    private ContributorDto.ContributorApplicationDto mapApplicationToDto(ContributorVehicleApplication a) {
        return ContributorDto.ContributorApplicationDto.builder()
                .id(a.getId())
                .contributorId(a.getContributor().getId())
                .targetPartnerId(a.getTargetPartner().getId())
                .targetPartnerBusinessName(a.getTargetPartner().getBusinessName())
                .make(a.getMake())
                .model(a.getModel())
                .yearOfManufacture(a.getYearOfManufacture())
                .category(a.getCategory().name())
                .transmission(a.getTransmission().name())
                .fuelType(a.getFuelType().name())
                .registrationNumber(a.getRegistrationNumber())
                .status(a.getStatus().name())
                .rejectionReason(a.getRejectionReason())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private ContributorDto.AgreementDto mapAgreementToDto(ContributionAgreement ag) {
        return ContributorDto.AgreementDto.builder()
                .id(ag.getId())
                .applicationId(ag.getApplication().getId())
                .partnerId(ag.getPartner().getId())
                .partnerBusinessName(ag.getPartner().getBusinessName())
                .contributorId(ag.getContributor().getId())
                .vehicleId(ag.getVehicle() != null ? ag.getVehicle().getId() : null)
                .agreementCode(ag.getAgreementCode())
                .fixedContributorCutPerBooking(ag.getFixedContributorCutPerBooking())
                .effectiveStartDate(ag.getEffectiveStartDate())
                .effectiveEndDate(ag.getEffectiveEndDate())
                .noticePeriodDays(ag.getNoticePeriodDays())
                .termsAndConditionsText(ag.getTermsAndConditionsText())
                .status(ag.getStatus().name())
                .partnerSignedAt(ag.getPartnerSignedAt())
                .contributorSignedAt(ag.getContributorSignedAt())
                .createdAt(ag.getCreatedAt())
                .build();
    }
}
