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
public class SendMessageRequest {
    private UUID chatId;
    private String encryptedContent;
    private String encryptionIv;
    private String messageType;
    private UUID replyToMessageId;
    private FileAttachmentDTO fileAttachment;
    private VoiceMessageDTO voiceMessage;
}
