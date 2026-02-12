package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket DTOs container class.
 * All DTOs are public static inner classes to comply with Java's
 * single public top-level class per file rule.
 */
public class WebSocketDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendMessageRequest {
        private UUID chatId;
        private String encryptedContent;
        private String encryptionIv;
        private String messageType; // TEXT, FILE, VOICE
        private UUID replyToMessageId;
        private FileAttachmentDTO fileAttachment;
        private VoiceMessageDTO voiceMessage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileAttachmentDTO {
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String fileUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoiceMessageDTO {
        private String audioUrl;
        private Integer durationSeconds;
        private Long fileSize;
        private String waveformData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageDTO {
        private UUID id;
        private UUID chatId;
        private String senderId;
        private String senderUsername;
        private String messageType;
        private String encryptedContent;
        private String encryptionIv;
        private LocalDateTime createdAt;
        private Boolean isEdited;
        private UUID replyToMessageId;
        private FileAttachmentDTO fileAttachment;
        private VoiceMessageDTO voiceMessage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageStatusDTO {
        private UUID messageId;
        private String status; // SENT, DELIVERED, READ
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TypingRequest {
        private UUID chatId;
        private boolean typing;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TypingEventDTO {
        private String username;
        private UUID chatId;
        private boolean typing;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReadReceiptRequest {
        private UUID chatId;
        private UUID lastReadMessageId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReadReceiptDTO {
        private String username;
        private UUID lastReadMessageId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorDTO {
        private String error;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserStatusDTO {
        private String username;
        private boolean online;
        private LocalDateTime lastSeen;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WebRTCSignalDTO {
        private String type; // offer, answer, ice-candidate
        private String targetUserId;
        private String senderId;
        private Object payload; // SDP or ICE candidate

        public WebRTCSignalDTO withSender(String sender) {
            this.senderId = sender;
            return this;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JoinConferenceRequest {
        private String conferenceId;
        private boolean videoEnabled;
        private boolean audioEnabled;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LeaveConferenceRequest {
        private String conferenceId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MediaStateRequest {
        private String conferenceId;
        private boolean videoEnabled;
        private boolean audioEnabled;
        private boolean screenSharing;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MediaStateDTO {
        private String username;
        private boolean videoEnabled;
        private boolean audioEnabled;
        private boolean screenSharing;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConferenceEventDTO {
        private String eventType;
        private Object data;
        private String username;
    }
}
