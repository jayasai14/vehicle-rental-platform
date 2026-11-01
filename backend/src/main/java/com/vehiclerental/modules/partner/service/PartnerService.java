package com.vehiclerental.modules.partner.service;

import com.vehiclerental.common.exception.BadRequestException;
import com.vehiclerental.common.exception.ResourceNotFoundException;
import com.vehiclerental.modules.location.entity.City;
import com.vehiclerental.modules.location.repository.CityRepository;
import com.vehiclerental.modules.partner.dto.PartnerDto;
import com.vehiclerental.modules.partner.entity.PartnerDocument;
import com.vehiclerental.modules.partner.entity.PartnerPaymentProfile;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.partner.repository.PartnerDocumentRepository;
import com.vehiclerental.modules.partner.repository.PartnerPaymentProfileRepository;
import com.vehiclerental.modules.partner.repository.PartnerProfileRepository;
import com.vehiclerental.modules.user.entity.Address;
import com.vehiclerental.modules.user.entity.Role;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.AddressRepository;
import com.vehiclerental.modules.user.repository.RoleRepository;
import com.vehiclerental.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerProfileRepository partnerProfileRepository;
    private final PartnerDocumentRepository partnerDocumentRepository;
    private final PartnerPaymentProfileRepository partnerPaymentProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CityRepository cityRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public PartnerDto.PartnerProfileDto applyForPartner(Long userId, PartnerDto.PartnerApplicationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (partnerProfileRepository.findByUserId(userId).isPresent()) {
            throw new BadRequestException("Partner application already exists for this user");
        }

        City city = request.getCityId() != null ? cityRepository.findById(request.getCityId()).orElse(null) : null;

        Address address = new Address();
        address.setUser(user);
        address.setCity(city != null ? city.getName() : "Vijayawada");
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setPostalCode(request.getPostalCode());
        address = addressRepository.save(address);

        PartnerProfile partner = new PartnerProfile();
        partner.setUser(user);
        partner.setBusinessName(request.getBusinessName());
        partner.setPanNumber(request.getPanNumber());
        partner.setGstin(request.getGstin());
        partner.setTradeLicenceNumber(request.getTradeLicenceNumber());
        partner.setCommercialRegistrationNumber(request.getCommercialRegistrationNumber());
        partner.setRegisteredBusinessAddress(address);
        partner.setVerificationStatus(PartnerProfile.PartnerVerificationStatus.SUBMITTED);
        partner.setActive(true);
        partner = partnerProfileRepository.save(partner);

        return mapToDto(partner);
    }

    @Transactional
    public PartnerDto.PartnerDocumentDto uploadDocument(Long userId, PartnerDto.UploadDocumentRequest request) {
        PartnerProfile partner = partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found"));

        PartnerDocument doc = new PartnerDocument();
        doc.setPartner(partner);
        doc.setDocumentType(PartnerDocument.PartnerDocumentType.valueOf(request.getDocumentType()));
        doc.setDocumentNumber(request.getDocumentNumber());
        doc.setStorageObjectKey(request.getStorageObjectKey());
        doc.setVerificationStatus(PartnerDocument.VerificationStatus.PENDING);
        doc = partnerDocumentRepository.save(doc);

        return PartnerDto.PartnerDocumentDto.builder()
                .id(doc.getId())
                .documentType(doc.getDocumentType().name())
                .documentNumber(doc.getDocumentNumber())
                .storageObjectKey(doc.getStorageObjectKey())
                .verificationStatus(doc.getVerificationStatus().name())
                .build();
    }

    @Transactional(readOnly = true)
    public PartnerDto.PartnerProfileDto getPartnerProfile(Long userId) {
        PartnerProfile partner = partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found"));
        return mapToDto(partner);
    }

    @Transactional(readOnly = true)
    public PartnerDto.PartnerProfileDto getPartnerProfileById(Long partnerId) {
        PartnerProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found with id: " + partnerId));
        return mapToDto(partner);
    }

    @Transactional
    public PartnerDto.PartnerPaymentProfileDto configurePaymentProfile(
            Long userId,
            PartnerDto.ConfigurePaymentProfileRequest request
    ) {
        PartnerProfile partner = partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found"));

        PartnerPaymentProfile ppp = partnerPaymentProfileRepository.findByPartnerId(partner.getId())
                .orElseGet(() -> {
                    PartnerPaymentProfile p = new PartnerPaymentProfile();
                    p.setPartner(partner);
                    return p;
                });

        ppp.setPayeeDisplayName(request.getPayeeDisplayName());
        ppp.setUpiId(request.getUpiId());
        ppp.setUpiQrObjectKey(request.getUpiQrObjectKey());
        ppp.setExternalPaymentUrl(request.getExternalPaymentUrl());
        ppp.setAcceptedPaymentMethods(request.getAcceptedPaymentMethods() != null ? request.getAcceptedPaymentMethods() : "[\"UPI_QR\", \"UPI_ID\", \"CASH_AT_PICKUP\"]");
        ppp.setCashAtPickupEnabled(request.isCashAtPickupEnabled());
        ppp.setActive(true);
        ppp = partnerPaymentProfileRepository.save(ppp);

        return PartnerDto.PartnerPaymentProfileDto.builder()
                .id(ppp.getId())
                .partnerId(partner.getId())
                .payeeDisplayName(ppp.getPayeeDisplayName())
                .upiId(ppp.getUpiId())
                .upiQrObjectKey(ppp.getUpiQrObjectKey())
                .externalPaymentUrl(ppp.getExternalPaymentUrl())
                .acceptedPaymentMethods(ppp.getAcceptedPaymentMethods())
                .cashAtPickupEnabled(ppp.isCashAtPickupEnabled())
                .active(ppp.isActive())
                .build();
    }

    @Transactional
    public PartnerDto.PartnerProfileDto reviewApplication(Long partnerId, Long adminUserId, PartnerDto.ReviewApplicationRequest request) {
        PartnerProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found"));
        User admin = userRepository.findById(adminUserId).orElse(null);

        if (request.isApproved()) {
            partner.setVerificationStatus(PartnerProfile.PartnerVerificationStatus.VERIFIED);
            partner.setVerifiedAt(LocalDateTime.now());
            partner.setVerifiedByUser(admin);
            partner.setRejectionReason(null);

            // Add ROLE_RENTAL_PARTNER to user
            User partnerUser = partner.getUser();
            Role partnerRole = roleRepository.findByName("ROLE_RENTAL_PARTNER")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_RENTAL_PARTNER", "Partner role")));
            partnerUser.getRoles().add(partnerRole);
            userRepository.save(partnerUser);
        } else {
            partner.setVerificationStatus(PartnerProfile.PartnerVerificationStatus.REJECTED);
            partner.setRejectionReason(request.getRejectionReason());
        }

        partner = partnerProfileRepository.save(partner);
        return mapToDto(partner);
    }

    @Transactional(readOnly = true)
    public List<PartnerDto.PartnerProfileDto> listAllPartners(PartnerProfile.PartnerVerificationStatus status) {
        List<PartnerProfile> partners = (status != null)
                ? partnerProfileRepository.findByVerificationStatus(status)
                : partnerProfileRepository.findAll();
        return partners.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private PartnerDto.PartnerProfileDto mapToDto(PartnerProfile p) {
        List<PartnerDocument> docs = partnerDocumentRepository.findByPartnerId(p.getId());
        PartnerPaymentProfile ppp = partnerPaymentProfileRepository.findByPartnerId(p.getId()).orElse(null);

        return PartnerDto.PartnerProfileDto.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .businessName(p.getBusinessName())
                .panNumber(p.getPanNumber())
                .gstin(p.getGstin())
                .tradeLicenceNumber(p.getTradeLicenceNumber())
                .commercialRegistrationNumber(p.getCommercialRegistrationNumber())
                .verificationStatus(p.getVerificationStatus().name())
                .verifiedAt(p.getVerifiedAt())
                .rejectionReason(p.getRejectionReason())
                .active(p.isActive())
                .registeredAddress(p.getRegisteredBusinessAddress() != null ? p.getRegisteredBusinessAddress().getAddressLine1() : null)
                .documents(docs.stream().map(d -> PartnerDto.PartnerDocumentDto.builder()
                        .id(d.getId())
                        .documentType(d.getDocumentType().name())
                        .documentNumber(d.getDocumentNumber())
                        .storageObjectKey(d.getStorageObjectKey())
                        .verificationStatus(d.getVerificationStatus().name())
                        .build()).collect(Collectors.toList()))
                .paymentProfile(ppp != null ? PartnerDto.PartnerPaymentProfileDto.builder()
                        .id(ppp.getId())
                        .partnerId(p.getId())
                        .payeeDisplayName(ppp.getPayeeDisplayName())
                        .upiId(ppp.getUpiId())
                        .upiQrObjectKey(ppp.getUpiQrObjectKey())
                        .externalPaymentUrl(ppp.getExternalPaymentUrl())
                        .acceptedPaymentMethods(ppp.getAcceptedPaymentMethods())
                        .cashAtPickupEnabled(ppp.isCashAtPickupEnabled())
                        .active(ppp.isActive())
                        .build() : null)
                .build();
    }
}
