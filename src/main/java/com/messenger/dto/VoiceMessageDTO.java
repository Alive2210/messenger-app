package com.messenger.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * DTO for voice message information.
 * Used for transferring voice message data between client and server.
 */
@Data
@Builder
public class VoiceMessageDTO {

    /**
     * Unique identifier of the voice message.
     */
    private String messageId;

    /**
     * Unique identifier of the chat/conversation.
     */
    private String chatId;

    /**
     * Unique identifier of the user who sent the voice message.
     */
    private String senderId;

    /**
     * Username of the sender.
     */
    private String senderUsername;

    /**
     * Duration of the voice message in seconds.
     */
    private Integer duration;

    /**
     * URL to the audio file stored in MinIO.
     */
    private String audioUrl;

    /**
     * Waveform data as JSON array representing audio amplitude visualization.
     * Example: [0.2, 0.5, 0.8, 0.3, 0.1]
     */
    private String waveform;

    /**
     * File size in bytes.
     */
    private Long fileSize;

    /**
     * MIME type of the audio file (e.g., audio/ogg, audio/mp4).
     */
    private String mimeType;

    /**
     * Whether the voice message has been played.
     */
    private Boolean isPlayed;

    /**
     * Text transcript of the voice message (from speech-to-text).
     */
    private String transcript;

    /**
     * Timestamp when the voice message was created.
     */
    private Instant createdAt;

    /**
     * DTO for creating a new voice message.
     */
    @Data
    @Builder
    public static class CreateVoiceMessageRequest {

        /**
         * Unique identifier of the chat.
         */
        private String chatId;

        /**
         * Unique identifier of the sender.
         */
        private String senderId;

        /**
         * URL to the audio file in MinIO.
         */
        private String audioUrl;

        /**
         * Duration in seconds.
         */
        private Integer duration;

        /**
         * Waveform data as JSON string.
         */
        private String waveform;

        /**
         * File size in bytes.
         */
        private Long fileSize;

        /**
         * MIME type of the audio.
         */
        private String mimeType;
    }

    /**
     * DTO for marking a voice message as played.
     */
    @Data
    @Builder
    public static class MarkAsPlayedRequest {

        /**
         * Unique identifier of the message.
         */
        private String messageId;

        /**
         * Unique identifier of the user marking as played.
         */
        private String userId;
    }

    /**
     * DTO for voice message list response.
     */
    @Data
    @Builder
    public static class VoiceMessageListResponse {

        /**
         * List of voice messages.
         */
        private List<VoiceMessageDTO> messages;

        /**
         * Total count of voice messages.
         */
        private long totalCount;

        /**
         * Count of unplayed voice messages.
         */
        private long unplayedCount;
    }

    /**
     * DTO for voice message upload response.
     */
    @Data
    @Builder
    public static class UploadResponse {

        /**
         * URL to the uploaded audio file.
         */
        private String audioUrl;

        /**
         * Generated waveform data.
         */
        private String waveform;

        /**
         * Duration of the audio in seconds.
         */
        private Integer duration;

        /**
         * File size in bytes.
         */
        private Long fileSize;

        /**
         * MIME type of the audio.
         */
        private String mimeType;
    }
}
