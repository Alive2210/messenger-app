package com.messenger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ChatDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateChatRequest {
        @NotBlank(message = "Chat name is required")
        private String chatName;
        
        @NotNull(message = "Chat type is required")
        private String chatType; // PERSONAL, GROUP, CHANNEL
        
        private List<String> participantUsernames;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddParticipantRequest {
        @NotBlank(message = "Username is required")
        private String username;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatDTO {
        private UUID id;
        private String chatName;
        private String chatType;
        private String chatAvatar;
        private String createdBy;
        private LocalDateTime createdAt;
        private List<ChatParticipantDTO> participants;
        private MessageDTO lastMessage;
        private Long unreadCount;
        private Boolean isEncrypted;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatParticipantDTO {
        private String userId;
        private String username;
        private String avatarUrl;
        private Boolean isAdmin;
        private LocalDateTime joinedAt;
        private Boolean isOnline;
    }
}
