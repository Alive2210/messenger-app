package com.messenger.controller;

import com.messenger.dto.ChatDTOs.*;
import com.messenger.dto.FileAttachmentDTO;
import com.messenger.dto.MessageDTO;
import com.messenger.dto.PaginatedResponse;
import com.messenger.dto.SendMessageRequest;
import com.messenger.dto.VoiceMessageDTO;
import com.messenger.service.ChatService;
import com.messenger.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    @Value("${app.server.url:https://localhost}")
    private String appServerUrl;

    @Value("${NETWORK_EXTERNAL_IP:}")
    private String networkExternalIp;

    private final ChatService chatService;
    private final MessageService messageService;
    private final com.messenger.service.QRCodeService qrCodeService;

    @PostMapping
    public ResponseEntity<ChatDTO> createChat(
            @Valid @RequestBody CreateChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Creating chat by user: {}", userDetails.getUsername());
        ChatDTO chat = chatService.createChat(request, userDetails.getUsername());
        return ResponseEntity.ok(chat);
    }

    @PostMapping("/{chatId}/join")
    public ResponseEntity<ChatDTO> joinChat(
            @PathVariable UUID chatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} joining chat {} via QR/link", userDetails.getUsername(), chatId);
        ChatDTO chat = chatService.joinChat(chatId, userDetails.getUsername());
        return ResponseEntity.ok(chat);
    }

    @GetMapping("/{chatId}/qr-invite")
    public ResponseEntity<java.util.Map<String, String>> getChatQRInvite(
            @PathVariable UUID chatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Generating QR invite for chat {} by user {}", chatId, userDetails.getUsername());
        // Verify user is in chat before allowing them to generate invite
        chatService.getChatById(chatId, userDetails.getUsername());

        String inviteContent = resolveInviteBaseUrl() + "/?joinChat=" + chatId;
        String qrCode = qrCodeService.generateQRCodeBase64(inviteContent);

        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("qrCode", qrCode);
        response.put("inviteLink", inviteContent);

        return ResponseEntity.ok(response);
    }

    private String resolveInviteBaseUrl() {
        String normalizedBaseUrl = normalizeBaseUrl(appServerUrl);

        try {
            java.net.URI uri = java.net.URI.create(normalizedBaseUrl);
            String host = uri.getHost();
            if (host != null && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host))
                    && networkExternalIp != null && !networkExternalIp.isBlank()) {
                return "https://" + networkExternalIp.trim();
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to parse app.server.url for QR invite, using normalized value: {}", normalizedBaseUrl);
        }

        return normalizedBaseUrl;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://localhost";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @GetMapping
    public ResponseEntity<List<ChatDTO>> getMyChats(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ChatDTO> chats = chatService.getUserChats(userDetails.getUsername());
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<ContactDTO>> getContacts(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting contacts for user {}", userDetails.getUsername());
        List<ContactDTO> contacts = chatService.getContacts(userDetails.getUsername());
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDTO> getChatById(
            @PathVariable UUID chatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ChatDTO chat = chatService.getChatById(chatId, userDetails.getUsername());
        return ResponseEntity.ok(chat);
    }

    @PostMapping("/{chatId}/participants")
    public ResponseEntity<Void> addParticipant(
            @PathVariable UUID chatId,
            @RequestBody AddParticipantRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        chatService.addParticipant(chatId, request.getUsername(), userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{chatId}/participants/{username}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable UUID chatId,
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        chatService.removeParticipant(chatId, username, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/personal/{username}")
    public ResponseEntity<ChatDTO> getOrCreatePersonalChat(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        ChatDTO chat = chatService.getOrCreatePersonalChat(userDetails.getUsername(), username);
        return ResponseEntity.ok(chat);
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageDTO>> getChatMessages(
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting messages for chat {} by user {}", chatId, userDetails.getUsername());
        List<MessageDTO> messages = messageService.getChatMessages(chatId, userDetails.getUsername(), page, size);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{chatId}/messages/paginated")
    public ResponseEntity<PaginatedResponse<MessageDTO>> getChatMessagesPaginated(
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting paginated messages for chat {} by user {}", chatId, userDetails.getUsername());
        PaginatedResponse<MessageDTO> response = messageService.getChatMessagesPaginated(
                chatId, userDetails.getUsername(), page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable UUID chatId,
            @RequestBody SendMessageRestRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SendMessageRequest serviceRequest = SendMessageRequest.builder()
                .chatId(chatId)
                .encryptedContent(firstNonBlank(request.getEncryptedContent(), request.getContent()))
                .encryptionIv(request.getEncryptionIv())
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .replyToMessageId(request.getReplyToMessageId() != null ? request.getReplyToMessageId() : request.getReplyToId())
                .fileAttachment(request.getFileAttachment())
                .voiceMessage(request.getVoiceMessage())
                .clientMessageId(request.getClientMessageId())
                .build();

        MessageDTO message = messageService.sendMessage(serviceRequest, userDetails.getUsername());
        return ResponseEntity.ok(message);
    }

    @PostMapping("/{chatId}/read")
    public ResponseEntity<Void> markChatAsRead(
            @PathVariable UUID chatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        messageService.markMessagesAsRead(chatId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(
            @PathVariable UUID chatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Deleting chat {} by user {}", chatId, userDetails.getUsername());
        chatService.deleteChat(chatId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return "";
    }

    @Data
    public static class SendMessageRestRequest {
        private String content;
        private String encryptedContent;
        private String encryptionIv;
        private String messageType;
        private UUID replyToId;
        private UUID replyToMessageId;
        private FileAttachmentDTO fileAttachment;
        private VoiceMessageDTO voiceMessage;
        private String clientMessageId;
    }
}
