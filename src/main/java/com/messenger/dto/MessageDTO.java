package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {
    private UUID id;
    private UUID chatId;
    private String senderId;
    private String senderUsername;
    private String senderAvatarUrl;
    private String messageType;
    private String encryptedContent;
    private String encryptionIv;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isEdited;
    private Boolean isDeleted;
    private UUID replyToMessageId;
    private FileAttachmentDTO fileAttachment;
    private VoiceMessageDTO voiceMessage;
    private Double locationLat;
    private Double locationLng;
    private String locationLabel;
}
