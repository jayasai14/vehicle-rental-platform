package com.vehiclerental.modules.contributor.entity;

import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.user.entity.Address;
import com.vehiclerental.modules.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contributor_vehicle_applications")
public class ContributorVehicleApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contributor_id", nullable = false)
    private ContributorProfile contributor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_partner_id", nullable = false)
    private PartnerProfile targetPartner;

    @Column(nullable = false, length = 50)
    private String make;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "year_of_manufacture", nullable = false)
    private Integer yearOfManufacture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransmissionType transmission = TransmissionType.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType = FuelType.PETROL;

    @Column(name = "registration_number", nullable = false, unique = true, length = 30)
    private String registrationNumber;

    @Column(name = "vin_chassis_number", nullable = false, length = 50)
    private String vinChassisNumber;

    @Column(name = "engine_number", nullable = false, length = 50)
    private String engineNumber;

    @Column(name = "current_odometer_km", nullable = false)
    private Integer currentOdometerKm = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_pickup_address_id", nullable = false)
    private Address vehiclePickupAddress;

    @Column(name = "rc_document_storage_key", nullable = false)
    private String rcDocumentStorageKey;

    @Column(name = "insurance_document_storage_key", nullable = false)
    private String insuranceDocumentStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedByUser;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum VehicleCategory { CAR, BIKE, SCOOTER }
    public enum TransmissionType { MANUAL, AUTOMATIC }
    public enum FuelType { PETROL, DIESEL, ELECTRIC, HYBRID, CNG }
    public enum ApplicationStatus { SUBMITTED, IN_REVIEW, ACCEPTED, REJECTED, WITHDRAWN }

    public ContributorVehicleApplication() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ContributorProfile getContributor() { return contributor; }
    public void setContributor(ContributorProfile contributor) { this.contributor = contributor; }
    public PartnerProfile getTargetPartner() { return targetPartner; }
    public void setTargetPartner(PartnerProfile targetPartner) { this.targetPartner = targetPartner; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getYearOfManufacture() { return yearOfManufacture; }
    public void setYearOfManufacture(Integer yearOfManufacture) { this.yearOfManufacture = yearOfManufacture; }
    public VehicleCategory getCategory() { return category; }
    public void setCategory(VehicleCategory category) { this.category = category; }
    public TransmissionType getTransmission() { return transmission; }
    public void setTransmission(TransmissionType transmission) { this.transmission = transmission; }
    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getVinChassisNumber() { return vinChassisNumber; }
    public void setVinChassisNumber(String vinChassisNumber) { this.vinChassisNumber = vinChassisNumber; }
    public String getEngineNumber() { return engineNumber; }
    public void setEngineNumber(String engineNumber) { this.engineNumber = engineNumber; }
    public Integer getCurrentOdometerKm() { return currentOdometerKm; }
    public void setCurrentOdometerKm(Integer currentOdometerKm) { this.currentOdometerKm = currentOdometerKm; }
    public Address getVehiclePickupAddress() { return vehiclePickupAddress; }
    public void setVehiclePickupAddress(Address vehiclePickupAddress) { this.vehiclePickupAddress = vehiclePickupAddress; }
    public String getRcDocumentStorageKey() { return rcDocumentStorageKey; }
    public void setRcDocumentStorageKey(String rcDocumentStorageKey) { this.rcDocumentStorageKey = rcDocumentStorageKey; }
    public String getInsuranceDocumentStorageKey() { return insuranceDocumentStorageKey; }
    public void setInsuranceDocumentStorageKey(String insuranceDocumentStorageKey) { this.insuranceDocumentStorageKey = insuranceDocumentStorageKey; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public User getReviewedByUser() { return reviewedByUser; }
    public void setReviewedByUser(User reviewedByUser) { this.reviewedByUser = reviewedByUser; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
