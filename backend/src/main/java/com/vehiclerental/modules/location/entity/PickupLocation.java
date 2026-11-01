package com.vehiclerental.modules.location.entity;

import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.user.entity.Address;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pickup_locations")
public class PickupLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerProfile partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_hub_id")
    private PartnerHub partnerHub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Column(name = "location_name", nullable = false, length = 150)
    private String locationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false)
    private PickupLocationType locationType = PickupLocationType.HUB;

    @Column(name = "doorstep_delivery_surcharge", nullable = false, precision = 10, scale = 2)
    private BigDecimal doorstepDeliverySurcharge = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum PickupLocationType {
        HUB, AIRPORT, RAILWAY_STATION, BUS_TERMINAL, HOTEL_DROPOFF, DOORSTEP_DELIVERY_ZONE
    }

    public PickupLocation() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PartnerProfile getPartner() { return partner; }
    public void setPartner(PartnerProfile partner) { this.partner = partner; }
    public PartnerHub getPartnerHub() { return partnerHub; }
    public void setPartnerHub(PartnerHub partnerHub) { this.partnerHub = partnerHub; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public PickupLocationType getLocationType() { return locationType; }
    public void setLocationType(PickupLocationType locationType) { this.locationType = locationType; }
    public BigDecimal getDoorstepDeliverySurcharge() { return doorstepDeliverySurcharge; }
    public void setDoorstepDeliverySurcharge(BigDecimal doorstepDeliverySurcharge) { this.doorstepDeliverySurcharge = doorstepDeliverySurcharge; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
