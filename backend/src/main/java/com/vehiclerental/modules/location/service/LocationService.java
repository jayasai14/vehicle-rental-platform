package com.vehiclerental.modules.location.service;

import com.vehiclerental.common.exception.ResourceNotFoundException;
import com.vehiclerental.modules.location.dto.LocationDto;
import com.vehiclerental.modules.location.entity.City;
import com.vehiclerental.modules.location.entity.PartnerHub;
import com.vehiclerental.modules.location.entity.PickupLocation;
import com.vehiclerental.modules.location.repository.CityRepository;
import com.vehiclerental.modules.location.repository.PartnerHubRepository;
import com.vehiclerental.modules.location.repository.PickupLocationRepository;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.partner.repository.PartnerProfileRepository;
import com.vehiclerental.modules.user.entity.Address;
import com.vehiclerental.modules.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final CityRepository cityRepository;
    private final PartnerHubRepository partnerHubRepository;
    private final PickupLocationRepository pickupLocationRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<LocationDto.CityDto> getAllCities() {
        return cityRepository.findByActiveTrue().stream()
                .map(c -> LocationDto.CityDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .state(c.getState())
                        .country(c.getCountry())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocationDto.PartnerHubDto> getHubs(Long cityId, Long partnerId) {
        List<PartnerHub> hubs;
        if (partnerId != null) {
            hubs = partnerHubRepository.findByPartnerId(partnerId);
        } else if (cityId != null) {
            hubs = partnerHubRepository.findByCityIdAndActiveTrue(cityId);
        } else {
            hubs = partnerHubRepository.findByActiveTrue();
        }

        return hubs.stream().map(h -> LocationDto.PartnerHubDto.builder()
                .id(h.getId())
                .partnerId(h.getPartner().getId())
                .name(h.getName())
                .cityId(h.getCity().getId())
                .cityName(h.getCity().getName())
                .contactPhone(h.getContactPhone())
                .fullAddress(formatAddress(h.getAddress()))
                .build()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocationDto.PickupLocationDto> getPickupLocations(Long partnerId, Long hubId) {
        List<PickupLocation> locations;
        if (partnerId != null) {
            locations = pickupLocationRepository.findByPartnerId(partnerId);
        } else if (hubId != null) {
            locations = pickupLocationRepository.findByPartnerHubId(hubId);
        } else {
            locations = pickupLocationRepository.findByActiveTrue();
        }

        return locations.stream().map(l -> LocationDto.PickupLocationDto.builder()
                .id(l.getId())
                .partnerId(l.getPartner().getId())
                .partnerHubId(l.getPartnerHub() != null ? l.getPartnerHub().getId() : null)
                .locationName(l.getLocationName())
                .locationType(l.getLocationType().name())
                .doorstepDeliverySurcharge(l.getDoorstepDeliverySurcharge())
                .fullAddress(formatAddress(l.getAddress()))
                .build()).collect(Collectors.toList());
    }

    @Transactional
    public LocationDto.PartnerHubDto createHub(Long userId, LocationDto.CreateHubRequest request) {
        PartnerProfile partner = partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found for user"));

        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + request.getCityId()));

        Address address = new Address();
        address.setCity(city != null ? city.getName() : "Vijayawada");
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setPostalCode(request.getPostalCode());
        address = addressRepository.save(address);

        PartnerHub hub = new PartnerHub();
        hub.setPartner(partner);
        hub.setCity(city);
        hub.setAddress(address);
        hub.setName(request.getName());
        hub.setContactPhone(request.getContactPhone());
        hub.setActive(true);
        hub = partnerHubRepository.save(hub);

        return LocationDto.PartnerHubDto.builder()
                .id(hub.getId())
                .partnerId(partner.getId())
                .name(hub.getName())
                .cityId(city.getId())
                .cityName(city.getName())
                .contactPhone(hub.getContactPhone())
                .fullAddress(formatAddress(address))
                .build();
    }

    @Transactional
    public LocationDto.PickupLocationDto createPickupLocation(Long userId, LocationDto.CreatePickupLocationRequest request) {
        PartnerProfile partner = partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found for user"));

        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));

        Address address = new Address();
        address.setCity(city != null ? city.getName() : "Vijayawada");
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setPostalCode(request.getPostalCode());
        address = addressRepository.save(address);

        PartnerHub hub = null;
        if (request.getPartnerHubId() != null) {
            hub = partnerHubRepository.findById(request.getPartnerHubId()).orElse(null);
        }

        PickupLocation loc = new PickupLocation();
        loc.setPartner(partner);
        loc.setPartnerHub(hub);
        loc.setAddress(address);
        loc.setLocationName(request.getLocationName());
        loc.setLocationType(PickupLocation.PickupLocationType.valueOf(request.getLocationType()));
        loc.setDoorstepDeliverySurcharge(request.getDoorstepDeliverySurcharge() != null ? request.getDoorstepDeliverySurcharge() : BigDecimal.ZERO);
        loc.setActive(true);
        loc = pickupLocationRepository.save(loc);

        return LocationDto.PickupLocationDto.builder()
                .id(loc.getId())
                .partnerId(partner.getId())
                .partnerHubId(hub != null ? hub.getId() : null)
                .locationName(loc.getLocationName())
                .locationType(loc.getLocationType().name())
                .doorstepDeliverySurcharge(loc.getDoorstepDeliverySurcharge())
                .fullAddress(formatAddress(address))
                .build();
    }

    private String formatAddress(Address a) {
        if (a == null) return "";
        StringBuilder sb = new StringBuilder();
        if (a.getAddressLine1() != null) sb.append(a.getAddressLine1());
        if (a.getAddressLine2() != null && !a.getAddressLine2().isBlank()) sb.append(", ").append(a.getAddressLine2());
        if (a.getCity() != null) sb.append(", ").append(a.getCity());
        if (a.getPostalCode() != null) sb.append(" - ").append(a.getPostalCode());
        return sb.toString();
    }
}
