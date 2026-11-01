package com.vehiclerental.common.seed;

import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.booking.entity.BookingHold;
import com.vehiclerental.modules.booking.entity.Inspection;
import com.vehiclerental.modules.booking.repository.BookingHoldRepository;
import com.vehiclerental.modules.booking.repository.BookingRepository;
import com.vehiclerental.modules.booking.repository.InspectionRepository;
import com.vehiclerental.modules.contributor.entity.ContributionAgreement;
import com.vehiclerental.modules.contributor.entity.ContributorProfile;
import com.vehiclerental.modules.contributor.entity.ContributorVehicleApplication;
import com.vehiclerental.modules.contributor.repository.ContributionAgreementRepository;
import com.vehiclerental.modules.contributor.repository.ContributorProfileRepository;
import com.vehiclerental.modules.contributor.repository.ContributorVehicleApplicationRepository;
import com.vehiclerental.modules.location.entity.City;
import com.vehiclerental.modules.location.entity.PartnerHub;
import com.vehiclerental.modules.location.entity.PickupLocation;
import com.vehiclerental.modules.location.repository.CityRepository;
import com.vehiclerental.modules.location.repository.PartnerHubRepository;
import com.vehiclerental.modules.location.repository.PickupLocationRepository;
import com.vehiclerental.modules.operations.entity.Review;
import com.vehiclerental.modules.operations.repository.ReviewRepository;
import com.vehiclerental.modules.partner.entity.PartnerPaymentProfile;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.partner.repository.PartnerPaymentProfileRepository;
import com.vehiclerental.modules.partner.repository.PartnerProfileRepository;
import com.vehiclerental.modules.payment.entity.*;
import com.vehiclerental.modules.payment.repository.*;
import com.vehiclerental.modules.user.entity.*;
import com.vehiclerental.modules.user.repository.*;
import com.vehiclerental.modules.vehicle.entity.*;
import com.vehiclerental.modules.vehicle.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final DrivingLicenceRepository drivingLicenceRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final PartnerPaymentProfileRepository paymentProfileRepository;
    private final ContributorProfileRepository contributorProfileRepository;
    private final ContributorVehicleApplicationRepository applicationRepository;
    private final ContributionAgreementRepository agreementRepository;
    private final CityRepository cityRepository;
    private final PartnerHubRepository hubRepository;
    private final PickupLocationRepository pickupRepository;
    private final AddressRepository addressRepository;
    private final VehicleRepository vehicleRepository;
    private final VehiclePricingProfileRepository pricingRepository;
    private final VehicleImageRepository imageRepository;
    private final VehicleFeatureRepository featureRepository;
    private final BookingHoldRepository holdRepository;
    private final BookingRepository bookingRepository;
    private final RentalPaymentDemandRepository demandRepository;
    private final RentalPaymentSubmissionRepository submissionRepository;
    private final SecurityDepositRepository depositRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail("admin@vehiclerental.com").isPresent()) {
            log.info("Demo data already seeded.");
            return;
        }

        log.info("Seeding realistic development & demo data...");

        // Ensure roles
        Role roleCustomer = getOrCreateRole("ROLE_CUSTOMER", "Customer renter");
        Role rolePartner = getOrCreateRole("ROLE_RENTAL_PARTNER", "Rental partner fleet manager");
        Role roleContributor = getOrCreateRole("ROLE_VEHICLE_CONTRIBUTOR", "Vehicle owner contributor");
        Role roleAdmin = getOrCreateRole("ROLE_ADMIN", "Platform Administrator");

        // Cities
        City vijayawada = cityRepository.findByNameIgnoreCase("Vijayawada")
                .orElseGet(() -> cityRepository.save(new City("Vijayawada", "Andhra Pradesh", "India")));
        City guntur = cityRepository.findByNameIgnoreCase("Guntur")
                .orElseGet(() -> cityRepository.save(new City("Guntur", "Andhra Pradesh", "India")));

        // 1. Admin User
        User admin = new User();
        admin.setFullName("Platform Administrator");
        admin.setEmail("admin@vehiclerental.com");
        admin.setPhoneNumber("+919876543210");
        admin.setPasswordHash(passwordEncoder.encode("Password@123"));
        admin.setActive(true);
        admin.setEmailVerified(true);
        admin.setRoles(Set.of(roleAdmin, roleCustomer));
        admin = userRepository.save(admin);

        // 2. Partner User
        User partnerUser = new User();
        partnerUser.setFullName("Kalyan Rentals Fleet Manager");
        partnerUser.setEmail("partner@vijayawada.com");
        partnerUser.setPhoneNumber("+919848012345");
        partnerUser.setPasswordHash(passwordEncoder.encode("Password@123"));
        partnerUser.setActive(true);
        partnerUser.setEmailVerified(true);
        partnerUser.setRoles(Set.of(rolePartner, roleCustomer));
        partnerUser = userRepository.save(partnerUser);

        Address partnerAddr = new Address();
        partnerAddr.setUser(partnerUser);
        partnerAddr.setCity(vijayawada.getName());
        partnerAddr.setAddressLine1("Shop 14, MG Road, Benz Circle");
        partnerAddr.setPostalCode("520010");
        partnerAddr = addressRepository.save(partnerAddr);

        PartnerProfile partner = new PartnerProfile();
        partner.setUser(partnerUser);
        partner.setBusinessName("Kalyan Self-Drive Rentals LLP");
        partner.setPanNumber("ABCDE1234F");
        partner.setGstin("37ABCDE1234F1Z5");
        partner.setTradeLicenceNumber("TL-VJA-2026-9081");
        partner.setCommercialRegistrationNumber("CR-AP-09827");
        partner.setRegisteredBusinessAddress(partnerAddr);
        partner.setVerificationStatus(PartnerProfile.PartnerVerificationStatus.VERIFIED);
        partner.setVerifiedAt(LocalDateTime.now().minusDays(30));
        partner.setVerifiedByUser(admin);
        partner.setActive(true);
        partner = partnerProfileRepository.save(partner);

        // Partner Payment Profile (Partner-Direct UPI)
        PartnerPaymentProfile ppp = new PartnerPaymentProfile();
        ppp.setPartner(partner);
        ppp.setPayeeDisplayName("Kalyan Rentals");
        ppp.setUpiId("kalyanrentals@oksbi");
        ppp.setExternalPaymentUrl("https://upi.example.com/pay?pa=kalyanrentals@oksbi");
        ppp.setAcceptedPaymentMethods("[\"UPI_QR\", \"UPI_ID\", \"CASH_AT_PICKUP\"]");
        ppp.setCashAtPickupEnabled(true);
        ppp.setActive(true);
        paymentProfileRepository.save(ppp);

        // Partner Hubs & Pickup Locations
        PartnerHub benzCircleHub = new PartnerHub();
        benzCircleHub.setPartner(partner);
        benzCircleHub.setCity(vijayawada);
        benzCircleHub.setAddress(partnerAddr);
        benzCircleHub.setName("Benz Circle Central Hub");
        benzCircleHub.setContactPhone("+919848012345");
        benzCircleHub.setActive(true);
        benzCircleHub = hubRepository.save(benzCircleHub);

        Address rlyStationAddr = new Address();
        rlyStationAddr.setUser(partnerUser);
        rlyStationAddr.setCity(vijayawada.getName());
        rlyStationAddr.setAddressLine1("Opposite Railway Station Main Gate, Station Road");
        rlyStationAddr.setPostalCode("520001");
        rlyStationAddr = addressRepository.save(rlyStationAddr);

        PartnerHub rlyHub = new PartnerHub();
        rlyHub.setPartner(partner);
        rlyHub.setCity(vijayawada);
        rlyHub.setAddress(rlyStationAddr);
        rlyHub.setName("Vijayawada Junction Hub");
        rlyHub.setContactPhone("+919848012346");
        rlyHub.setActive(true);
        rlyHub = hubRepository.save(rlyHub);

        PickupLocation locBenz = new PickupLocation();
        locBenz.setPartner(partner);
        locBenz.setPartnerHub(benzCircleHub);
        locBenz.setAddress(partnerAddr);
        locBenz.setLocationName("Benz Circle Pickup Desk");
        locBenz.setLocationType(PickupLocation.PickupLocationType.HUB);
        locBenz.setDoorstepDeliverySurcharge(BigDecimal.ZERO);
        locBenz.setActive(true);
        pickupRepository.save(locBenz);

        PickupLocation locRly = new PickupLocation();
        locRly.setPartner(partner);
        locRly.setPartnerHub(rlyHub);
        locRly.setAddress(rlyStationAddr);
        locRly.setLocationName("Railway Station Platform 1 Counter");
        locRly.setLocationType(PickupLocation.PickupLocationType.RAILWAY_STATION);
        locRly.setDoorstepDeliverySurcharge(new BigDecimal("100.00"));
        locRly.setActive(true);
        pickupRepository.save(locRly);

        PickupLocation locAirport = new PickupLocation();
        locAirport.setPartner(partner);
        locAirport.setPartnerHub(benzCircleHub);
        locAirport.setAddress(partnerAddr);
        locAirport.setLocationName("Gannavaram Airport Arrival Dropoff Zone");
        locAirport.setLocationType(PickupLocation.PickupLocationType.AIRPORT);
        locAirport.setDoorstepDeliverySurcharge(new BigDecimal("350.00"));
        locAirport.setActive(true);
        pickupRepository.save(locAirport);

        // 3. Contributor User
        User contributorUser = new User();
        contributorUser.setFullName("Suresh Varma");
        contributorUser.setEmail("contributor@owner.com");
        contributorUser.setPhoneNumber("+919440156789");
        contributorUser.setPasswordHash(passwordEncoder.encode("Password@123"));
        contributorUser.setActive(true);
        contributorUser.setEmailVerified(true);
        contributorUser.setRoles(Set.of(roleContributor, roleCustomer));
        contributorUser = userRepository.save(contributorUser);

        ContributorProfile contributor = new ContributorProfile();
        contributor.setUser(contributorUser);
        contributor.setPanNumber("ABCDE9876Z");
        contributor.setBankAccountNumber("91002003004050");
        contributor.setBankIfscCode("HDFC0001234");
        contributor.setBankName("HDFC Bank Benz Circle");
        contributor.setResidentialAddress(partnerAddr);
        contributor.setActive(true);
        contributor = contributorProfileRepository.save(contributor);

        // 4. Customer User
        User customerUser = new User();
        customerUser.setFullName("Ravi Teja");
        customerUser.setEmail("customer@rider.com");
        customerUser.setPhoneNumber("+919123456780");
        customerUser.setPasswordHash(passwordEncoder.encode("Password@123"));
        customerUser.setActive(true);
        customerUser.setEmailVerified(true);
        customerUser.setRoles(Set.of(roleCustomer));
        customerUser = userRepository.save(customerUser);

        CustomerProfile customerProfile = new CustomerProfile();
        customerProfile.setUser(customerUser);
        customerProfile.setPreferredLanguage("English");
        customerProfile.setTotalCompletedTrips(3);
        customerProfileRepository.save(customerProfile);

        DrivingLicence dl = new DrivingLicence();
        dl.setUser(customerUser);
        dl.setLicenceNumber("AP-16-2020-0099881");
        dl.setIssuingRto("RTO Vijayawada");
        dl.setValidFrom(LocalDate.of(2020, 1, 15));
        dl.setValidUntil(LocalDate.of(2040, 1, 14));
        dl.setVehicleClassesAuthorized("LMV, MCWG");
        dl.setStorageObjectKey("demo_dl.pdf");
        dl.setVerificationStatus(DrivingLicence.VerificationStatus.VERIFIED);
        dl.setVerifiedAt(LocalDateTime.now().minusDays(20));
        drivingLicenceRepository.save(dl);

        // 5. Seed Vehicles (Cars, Bikes, Scooters)
        // Vehicle 1: Hyundai Creta
        Vehicle creta = createVehicle(
                "AP16BZ4567", "VINHYUNDAI2023001", "ENGCRETA0981",
                "Hyundai", "Creta SX (O)", 2023, Vehicle.VehicleCategory.CAR,
                Vehicle.TransmissionType.AUTOMATIC, Vehicle.FuelType.DIESEL, 5, 24000,
                partner, benzCircleHub, new BigDecimal("250.00"), new BigDecimal("3200.00"), new BigDecimal("4000.00"),
                "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80",
                List.of("Panoramic Sunroof", "Ventilated Seats", "Bose Premium Audio", "Automatic Cruise Control", "Wireless Charger")
        );

        // Vehicle 2: Maruti Swift
        Vehicle swift = createVehicle(
                "AP16CD8912", "VINMARUTI2024002", "ENGSWIFT7612",
                "Maruti Suzuki", "Swift ZXi", 2024, Vehicle.VehicleCategory.CAR,
                Vehicle.TransmissionType.MANUAL, Vehicle.FuelType.PETROL, 5, 12000,
                partner, rlyHub, new BigDecimal("120.00"), new BigDecimal("1600.00"), new BigDecimal("2000.00"),
                "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?auto=format&fit=crop&w=800&q=80",
                List.of("Touchscreen Display", "Android Auto & Apple CarPlay", "Keyless Entry", "Rear Parking Camera")
        );

        // Vehicle 3: Royal Enfield Classic 350
        Vehicle bullet = createVehicle(
                "AP16EF3419", "VINROYAL2023003", "ENGBULLET3501",
                "Royal Enfield", "Classic 350 Stealth Black", 2023, Vehicle.VehicleCategory.BIKE,
                Vehicle.TransmissionType.MANUAL, Vehicle.FuelType.PETROL, 2, 8500,
                partner, benzCircleHub, new BigDecimal("90.00"), new BigDecimal("950.00"), new BigDecimal("1500.00"),
                "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?auto=format&fit=crop&w=800&q=80",
                List.of("Dual Channel ABS", "Tripper Navigation", "Alloy Wheels", "USB Charging Port")
        );

        // Vehicle 4: Honda Activa 6G
        Vehicle activa = createVehicle(
                "AP16GH9021", "VINHONDA2024004", "ENGACTIVA6001",
                "Honda", "Activa 6G Premium", 2024, Vehicle.VehicleCategory.SCOOTER,
                Vehicle.TransmissionType.AUTOMATIC, Vehicle.FuelType.PETROL, 2, 4200,
                partner, rlyHub, new BigDecimal("45.00"), new BigDecimal("450.00"), new BigDecimal("800.00"),
                "https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?auto=format&fit=crop&w=800&q=80",
                List.of("Telescopic Suspension", "External Fuel Fill", "Silent Start ACG", "LED Headlamp")
        );

        // Vehicle 5: Tata Nexon EV (Contributor Owned with Active Agreement)
        Vehicle nexon = createVehicle(
                "AP16JK5500", "VINTATANEX2024005", "ENGNEXONEV001",
                "Tata", "Nexon EV Empowered", 2024, Vehicle.VehicleCategory.CAR,
                Vehicle.TransmissionType.AUTOMATIC, Vehicle.FuelType.ELECTRIC, 5, 15000,
                partner, benzCircleHub, new BigDecimal("220.00"), new BigDecimal("2800.00"), new BigDecimal("3500.00"),
                "https://images.unsplash.com/photo-1617814076367-b759c7d7e738?auto=format&fit=crop&w=800&q=80",
                List.of("360 Degree Camera", "465 km ARAI Range", "Fast DC Charging Support", "Sunroof", "Air Purifier")
        );
        nexon.setOwnershipType(Vehicle.OwnershipType.INDIVIDUAL_OWNER);
        nexon.setLegalOwnerUser(contributorUser);
        vehicleRepository.save(nexon);

        // Contributor application & agreement for Nexon EV
        ContributorVehicleApplication app = new ContributorVehicleApplication();
        app.setContributor(contributor);
        app.setTargetPartner(partner);
        app.setMake("Tata");
        app.setModel("Nexon EV Empowered");
        app.setYearOfManufacture(2024);
        app.setCategory(ContributorVehicleApplication.VehicleCategory.CAR);
        app.setTransmission(ContributorVehicleApplication.TransmissionType.AUTOMATIC);
        app.setFuelType(ContributorVehicleApplication.FuelType.ELECTRIC);
        app.setRegistrationNumber("AP16JK5500");
        app.setVinChassisNumber("VINTATANEX2024005");
        app.setEngineNumber("ENGNEXONEV001");
        app.setCurrentOdometerKm(15000);
        app.setVehiclePickupAddress(partnerAddr);
        app.setRcDocumentStorageKey("demo_nexon_rc.pdf");
        app.setInsuranceDocumentStorageKey("demo_nexon_ins.pdf");
        app.setStatus(ContributorVehicleApplication.ApplicationStatus.ACCEPTED);
        app.setReviewedAt(LocalDateTime.now().minusDays(10));
        app.setReviewedByUser(partnerUser);
        app = applicationRepository.save(app);

        ContributionAgreement agr = ContributionAgreement.builder()
                .application(app)
                .partner(partner)
                .contributor(contributor)
                .vehicle(nexon)
                .agreementCode("AGR-NEXON-001")
                .fixedContributorCutPerBooking(new BigDecimal("1200.00"))
                .effectiveStartDate(LocalDate.now().minusDays(10))
                .effectiveEndDate(LocalDate.now().plusYears(1))
                .noticePeriodDays(7)
                .termsAndConditionsText("Standard Vehicle Rental Contribution Agreement terms. Fixed compensation of Rs 1,200 per completed booking.")
                .status(ContributionAgreement.AgreementStatus.ACTIVE)
                .partnerSignedAt(LocalDateTime.now().minusDays(10))
                .contributorSignedAt(LocalDateTime.now().minusDays(9))
                .build();
        agreementRepository.save(agr);

        // Seed a completed past booking with Review for Creta
        seedPastBookingWithReview(customerUser, partner, creta, locBenz, new BigDecimal("3200.00"), new BigDecimal("4000.00"));

        log.info("Demo data seeding completed successfully! Demo accounts ready:");
        log.info("  Admin: admin@vehiclerental.com / Password@123");
        log.info("  Partner: partner@vijayawada.com / Password@123");
        log.info("  Contributor: contributor@owner.com / Password@123");
        log.info("  Customer: customer@rider.com / Password@123");
    }

    private Role getOrCreateRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name, description)));
    }

    private Vehicle createVehicle(
            String reg, String vin, String eng,
            String make, String model, int year, Vehicle.VehicleCategory cat,
            Vehicle.TransmissionType trans, Vehicle.FuelType fuel, int seats, int odo,
            PartnerProfile partner, PartnerHub hub,
            BigDecimal hourly, BigDecimal daily, BigDecimal deposit,
            String imgUrl, List<String> features
    ) {
        Vehicle v = Vehicle.builder()
                .registrationNumber(reg)
                .vinChassisNumber(vin)
                .engineNumber(eng)
                .make(make)
                .model(model)
                .yearOfManufacture(year)
                .category(cat)
                .transmission(trans)
                .fuelType(fuel)
                .seatingCapacity(seats)
                .odometerReadingKm(odo)
                .currentFuelOrBatteryLevelPercent(100)
                .ownershipType(Vehicle.OwnershipType.PARTNER_BUSINESS_OWNER)
                .legalOwnerPartner(partner)
                .managerPartner(partner)
                .assignedHub(hub)
                .currentCustodyStatus(Vehicle.CustodyStatus.PARTNER_CUSTODY)
                .listingStatus(Vehicle.ListingStatus.PUBLISHED)
                .turnaroundBufferMinutes(60)
                .build();
        v = vehicleRepository.save(v);

        VehiclePricingProfile p = VehiclePricingProfile.builder()
                .vehicle(v)
                .hourlyRate(hourly)
                .dailyRate(daily)
                .weeklyRate(daily.multiply(new BigDecimal("6.5")))
                .securityDepositAmount(deposit)
                .freeKmPerDay(300)
                .extraKmCharge(new BigDecimal("12.00"))
                .lateReturnHourlyPenalty(new BigDecimal("200.00"))
                .isActive(true)
                .build();
        pricingRepository.save(p);

        VehicleImage img = VehicleImage.builder()
                .vehicle(v)
                .storageObjectKey(imgUrl)
                .isPrimary(true)
                .displayOrder(0)
                .build();
        imageRepository.save(img);

        for (String feat : features) {
            VehicleFeature vf = VehicleFeature.builder()
                    .vehicle(v)
                    .featureName(feat)
                    .featureCategory(VehicleFeature.FeatureCategory.CONVENIENCE)
                    .build();
            featureRepository.save(vf);
        }

        return v;
    }

    private void seedPastBookingWithReview(User customer, PartnerProfile partner, Vehicle vehicle, PickupLocation loc, BigDecimal rental, BigDecimal deposit) {
        LocalDateTime start = LocalDateTime.now().minusDays(5);
        LocalDateTime end = LocalDateTime.now().minusDays(3);

        BookingHold hold = BookingHold.builder()
                .holdReference("HLD-PAST-001")
                .vehicle(vehicle)
                .customerUser(customer)
                .pickupLocation(loc)
                .dropoffLocation(loc)
                .startTime(start)
                .endTime(end)
                .quotedRentalAmount(rental)
                .quotedDepositAmount(deposit)
                .quotedSurchargeAmount(BigDecimal.ZERO)
                .totalHoldAmount(rental.add(deposit))
                .status(BookingHold.HoldStatus.CONVERTED)
                .expiresAt(start.plusMinutes(15))
                .build();
        hold = holdRepository.save(hold);

        RentalPaymentDemand demand = RentalPaymentDemand.builder()
                .demandReference("DEM-PAST-001")
                .hold(hold)
                .managerPartner(partner)
                .customerUser(customer)
                .demandType(RentalPaymentDemand.DemandType.BOOKING_CONFIRMATION)
                .amountDemanded(rental)
                .totalVerifiedSubmitted(rental)
                .amountSatisfied(rental)
                .excessAmount(BigDecimal.ZERO)
                .cancellationFeeRetained(BigDecimal.ZERO)
                .currency("INR")
                .status(RentalPaymentDemand.DemandStatus.SATISFIED)
                .build();
        demand = demandRepository.save(demand);

        RentalPaymentSubmission sub = RentalPaymentSubmission.builder()
                .submissionReference("SUB-PAST-001")
                .demand(demand)
                .managerPartner(partner)
                .customerUser(customer)
                .paymentMethod(RentalPaymentSubmission.PaymentMethod.UPI_QR)
                .transactionReference("UPI778899001122")
                .amountSubmitted(rental)
                .currency("INR")
                .receiptObjectKey("demo_receipt.jpg")
                .transactionTimestamp(start.minusMinutes(10))
                .submissionStatus(RentalPaymentSubmission.SubmissionStatus.VERIFIED)
                .verifiedAt(start.minusMinutes(5))
                .verifiedByUser(partner.getUser())
                .reappliedAmount(BigDecimal.ZERO)
                .build();
        submissionRepository.save(sub);

        Booking booking = Booking.builder()
                .bookingCode("BKG-PAST-001")
                .hold(hold)
                .vehicle(vehicle)
                .customerUser(customer)
                .managerPartner(partner)
                .pickupLocation(loc)
                .dropoffLocation(loc)
                .startTime(start)
                .endTime(end)
                .actualReturnTime(end)
                .baseRentalAmount(rental)
                .securityDepositAmount(deposit)
                .deliverySurchargeAmount(BigDecimal.ZERO)
                .totalAmountPayable(rental.add(deposit))
                .initialDemand(demand)
                .bookingStatus(Booking.BookingStatus.COMPLETED)
                .build();
        booking = bookingRepository.save(booking);

        SecurityDeposit secDep = SecurityDeposit.builder()
                .booking(booking)
                .managerPartner(partner)
                .customerUser(customer)
                .requiredAmount(deposit)
                .collectedAmount(deposit)
                .heldAmount(deposit)
                .deductedAmount(BigDecimal.ZERO)
                .refundedAmount(deposit)
                .currency("INR")
                .collectionMethod(SecurityDeposit.CollectionMethod.UPI_TRANSFER)
                .collectionTransactionReference("DEP9988776655")
                .status(SecurityDeposit.DepositStatus.FULLY_REFUNDED)
                .collectedAt(start)
                .build();
        depositRepository.save(secDep);

        Review review = Review.builder()
                .booking(booking)
                .vehicle(vehicle)
                .customerUser(customer)
                .ratingCleanliness(5)
                .ratingVehicleCondition(5)
                .ratingPartnerService(5)
                .overallRating(new BigDecimal("5.00"))
                .reviewTitle("Outstanding experience with Creta!")
                .reviewText("The car was spotless, drove smoothly on the highway to Amaravati. Kalyan Rentals team was extremely courteous.")
                .partnerReplyText("Thank you Ravi Teja! It was a pleasure hosting your journey. Looking forward to your next trip.")
                .partnerRepliedAt(LocalDateTime.now().minusDays(2))
                .isPublished(true)
                .build();
        reviewRepository.save(review);
    }
}
