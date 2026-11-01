package com.vehiclerental.modules.operations.service;

import com.vehiclerental.common.exception.ForbiddenException;
import com.vehiclerental.common.exception.ResourceNotFoundException;
import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.booking.repository.BookingRepository;
import com.vehiclerental.modules.operations.dto.OperationsDto;
import com.vehiclerental.modules.operations.entity.SupportTicket;
import com.vehiclerental.modules.operations.entity.SupportTicketMessage;
import com.vehiclerental.modules.operations.repository.SupportTicketMessageRepository;
import com.vehiclerental.modules.operations.repository.SupportTicketRepository;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public OperationsDto.TicketDto createTicket(Long userId, OperationsDto.CreateTicketRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        Booking booking = request.getBookingId() != null ? bookingRepository.findById(request.getBookingId()).orElse(null) : null;

        String ref = "TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        SupportTicket ticket = SupportTicket.builder()
                .ticketReference(ref)
                .user(user)
                .booking(booking)
                .subject(request.getSubject())
                .category(SupportTicket.TicketCategory.valueOf(request.getCategory()))
                .status(SupportTicket.TicketStatus.OPEN)
                .priority(request.getPriority() != null ? SupportTicket.TicketPriority.valueOf(request.getPriority()) : SupportTicket.TicketPriority.MEDIUM)
                .build();
        ticket = ticketRepository.save(ticket);

        SupportTicketMessage msg = SupportTicketMessage.builder()
                .ticket(ticket)
                .senderUser(user)
                .messageText(request.getInitialMessage())
                .build();
        messageRepository.save(msg);

        return mapTicketToDto(ticket);
    }

    @Transactional
    public OperationsDto.TicketMessageDto sendMessage(Long userId, Long ticketId, OperationsDto.SendMessageRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        boolean isOwner = ticket.getUser().getId().equals(userId);
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Unauthorized to send messages on this ticket");
        }

        SupportTicketMessage msg = SupportTicketMessage.builder()
                .ticket(ticket)
                .senderUser(user)
                .messageText(request.getMessageText())
                .attachmentObjectKey(request.getAttachmentObjectKey())
                .build();
        msg = messageRepository.save(msg);

        return OperationsDto.TicketMessageDto.builder()
                .id(msg.getId())
                .senderUserId(user.getId())
                .senderName(user.getFullName())
                .messageText(msg.getMessageText())
                .attachmentObjectKey(msg.getAttachmentObjectKey())
                .createdAt(msg.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<OperationsDto.TicketDto> getUserTickets(Long userId, Pageable pageable) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::mapTicketToDto);
    }

    @Transactional(readOnly = true)
    public Page<OperationsDto.TicketDto> getAllTickets(Pageable pageable) {
        return ticketRepository.findAll(pageable).map(this::mapTicketToDto);
    }

    @Transactional(readOnly = true)
    public OperationsDto.TicketDto getTicketById(Long userId, Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        if (userId != null) {
            User user = userRepository.findById(userId).orElseThrow();
            boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
            boolean isOwner = ticket.getUser().getId().equals(userId);
            if (!isAdmin && !isOwner) {
                throw new ForbiddenException("Unauthorized access to this ticket");
            }
        }
        return mapTicketToDto(ticket);
    }

    @Transactional(readOnly = true)
    public OperationsDto.TicketDto getTicketById(Long ticketId) {
        return getTicketById(null, ticketId);
    }

    @Transactional(readOnly = true)
    public List<OperationsDto.TicketMessageDto> getTicketMessages(Long userId, Long ticketId) {
        OperationsDto.TicketDto ticket = getTicketById(userId, ticketId);
        return ticket.getMessages();
    }

    @Transactional
    public OperationsDto.TicketDto updateStatus(Long ticketId, SupportTicket.TicketStatus status) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        ticket.setStatus(status);
        ticket = ticketRepository.save(ticket);
        return mapTicketToDto(ticket);
    }

    private OperationsDto.TicketDto mapTicketToDto(SupportTicket t) {
        List<SupportTicketMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(t.getId());
        return OperationsDto.TicketDto.builder()
                .id(t.getId())
                .ticketReference(t.getTicketReference())
                .userId(t.getUser().getId())
                .userName(t.getUser().getFullName())
                .bookingId(t.getBooking() != null ? t.getBooking().getId() : null)
                .subject(t.getSubject())
                .category(t.getCategory().name())
                .status(t.getStatus().name())
                .priority(t.getPriority().name())
                .createdAt(t.getCreatedAt())
                .messages(messages.stream().map(m -> OperationsDto.TicketMessageDto.builder()
                        .id(m.getId())
                        .senderUserId(m.getSenderUser().getId())
                        .senderName(m.getSenderUser().getFullName())
                        .messageText(m.getMessageText())
                        .attachmentObjectKey(m.getAttachmentObjectKey())
                        .createdAt(m.getCreatedAt())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
