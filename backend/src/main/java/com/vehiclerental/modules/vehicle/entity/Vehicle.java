package com.vehiclerental.modules.vehicle.entity;

import com.vehiclerental.modules.location.entity.PartnerHub;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", nullable = false, unique = true, length = 30)
    private String registrationNumber;

    @Column(name = "vin_chassis_number", nullable = false, unique = true, length = 50)
    private String vinChassisNumber;

    @Column(name = "engine_number", nullable = false, unique = true, length = 50)
    private String engineNumber;

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

    @Column(name = "seating_capacity", nullable = false)
    private Integer seatingCapacity = 4;

    @Column(name = "odometer_reading_km", nullable = false)
    private Integer odometerReadingKm = 0;

    @Column(name = "current_fuel_or_battery_level_percent", nullable = false)
    private Integer currentFuelOrBatteryLevelPercent = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_type", nullable = false)
    private OwnershipType ownershipType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_owner_user_id")
    private User legalOwnerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_owner_partner_id")
    private PartnerProfile legalOwnerPartner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_partner_id", nullable = false)
    private PartnerProfile managerPartner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_hub_id")
    private PartnerHub assignedHub;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_custody_status", nullable = false)
    private CustodyStatus currentCustodyStatus = CustodyStatus.PARTNER_CUSTODY;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_status", nullable = false)
    private ListingStatus listingStatus = ListingStatus.DRAFT;

    @Column(name = "turnaround_buffer_minutes", nullable = false)
    private Integer turnaroundBufferMinutes = 60;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum VehicleCategory { CAR, BIKE, SCOOTER }
    public enum TransmissionType { MANUAL, AUTOMATIC }
    public enum FuelType { PETROL, DIESEL, ELECTRIC, HYBRID, CNG }
    public enum OwnershipType { INDIVIDUAL_OWNER, PARTNER_BUSINESS_OWNER }
    public enum CustodyStatus { CONTRIBUTOR_CUSTODY, PARTNER_CUSTODY, CUSTOMER_CUSTODY, IN_MAINTENANCE, IN_TRANSIT }
    public enum ListingStatus { DRAFT, PENDING_APPROVAL, PUBLISHED, UNLISTED, SUSPENDED }

    public Vehicle() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getVinChassisNumber() { return vinChassisNumber; }
    public void setVinChassisNumber(String vinChassisNumber) { this.vinChassisNumber = vinChassisNumber; }
    public String getEngineNumber() { return engineNumber; }
    public void setEngineNumber(String engineNumber) { this.engineNumber = engineNumber; }
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
    public Integer getSeatingCapacity() { return seatingCapacity; }
    public void setSeatingCapacity(Integer seatingCapacity) { this.seatingCapacity = seatingCapacity; }
    public Integer getOdometerReadingKm() { return odometerReadingKm; }
    public void setOdometerReadingKm(Integer odometerReadingKm) { this.odometerReadingKm = odometerReadingKm; }
    public Integer getCurrentFuelOrBatteryLevelPercent() { return currentFuelOrBatteryLevelPercent; }
    public void setCurrentFuelOrBatteryLevelPercent(Integer currentFuelOrBatteryLevelPercent) { this.currentFuelOrBatteryLevelPercent = currentFuelOrBatteryLevelPercent; }
    public OwnershipType getOwnershipType() { return ownershipType; }
    public void setOwnershipType(OwnershipType ownershipType) { this.ownershipType = ownershipType; }
    public User getLegalOwnerUser() { return legalOwnerUser; }
    public void setLegalOwnerUser(User legalOwnerUser) { this.legalOwnerUser = legalOwnerUser; }
    public PartnerProfile getLegalOwnerPartner() { return legalOwnerPartner; }
    public void setLegalOwnerPartner(PartnerProfile legalOwnerPartner) { this.legalOwnerPartner = legalOwnerPartner; }
    public PartnerProfile getManagerPartner() { return managerPartner; }
    public void setManagerPartner(PartnerProfile managerPartner) { this.managerPartner = managerPartner; }
    public PartnerHub getAssignedHub() { return assignedHub; }
    public void setAssignedHub(PartnerHub assignedHub) { this.assignedHub = assignedHub; }
    public CustodyStatus getCurrentCustodyStatus() { return currentCustodyStatus; }
    public void setCurrentCustodyStatus(CustodyStatus currentCustodyStatus) { this.currentCustodyStatus = currentCustodyStatus; }
    public ListingStatus getListingStatus() { return listingStatus; }
    public void setListingStatus(ListingStatus listingStatus) { this.listingStatus = listingStatus; }
    public Integer getTurnaroundBufferMinutes() { return turnaroundBufferMinutes; }
    public void setTurnaroundBufferMinutes(Integer turnaroundBufferMinutes) { this.turnaroundBufferMinutes = turnaroundBufferMinutes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
