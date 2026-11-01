package com.vehiclerental.modules.custody.entity;

import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.location.entity.PartnerHub;
import com.vehiclerental.modules.user.entity.Address;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_custody_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleCustodyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "handover_type", nullable = false, length = 40)
    private HandoverType handoverType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_custodian_user_id")
    private User fromCustodianUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_custodian_user_id")
    private User toCustodianUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_partner_location_id")
    private PartnerHub fromPartnerHub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_partner_location_id")
    private PartnerHub toPartnerHub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handover_address_id", nullable = false)
    private Address handoverAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorized_rep_user_id")
    private User authorizedRepUser;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "odometer_reading_km", nullable = false)
    private Integer odometerReadingKm;

    @Column(name = "fuel_or_battery_level_percent", nullable = false)
    private Integer fuelOrBatteryLevelPercent;

    @Column(name = "custody_notes", length = 500)
    private String custodyNotes;

    @Column(name = "from_party_confirmed", nullable = false)
    private Boolean fromPartyConfirmed;

    @Column(name = "to_party_confirmed", nullable = false)
    private Boolean toPartyConfirmed;

    @PrePersist
    protected void onCreate() {
        if (eventTimestamp == null) eventTimestamp = LocalDateTime.now();
        if (fromPartyConfirmed == null) fromPartyConfirmed = false;
        if (toPartyConfirmed == null) toPartyConfirmed = false;
    }

    public enum HandoverType {
        CONTRIBUTOR_TO_PARTNER,
        PARTNER_TO_CUSTOMER,
        CONTRIBUTOR_TO_CUSTOMER,
        CUSTOMER_TO_PARTNER,
        CUSTOMER_TO_CONTRIBUTOR,
        PARTNER_TO_CONTRIBUTOR,
        PARTNER_HUB_TO_PARTNER_HUB,
        LOGISTICS_AGENT_RELOCATION
    }
}
