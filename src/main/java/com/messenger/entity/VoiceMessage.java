package com.messenger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a voice message attachment.
 */
@Entity
@Table(name = "voice_messages", indexes = {
        @Index(name = "idx_voice_message_id", columnList = "message_id"),
        @Index(name = "idx_voice_chat_id", columnList = "chat_id"),
        @Index(name = "idx_voice_sender_id", columnList = "sender_id"),
        @Index(name = "idx_voice_is_played", columnList = "is_played"),
        @Index(name = "idx_voice_duration", columnList = "duration_seconds")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceMessage {

    /**
     * Primary key identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    /**
     * Unique identifier of the associated message.
     */
    @Column(name = "message_id", nullable = false, unique = true)
    private java.util.UUID messageId;

    @Column(name = "chat_id", nullable = false)
    private java.util.UUID chatId;

    /**
     * Unique identifier of the user who sent the voice message.
     */
    @Column(name = "sender_id", nullable = false)
    private java.util.UUID senderId;

    /**
     * Duration of the voice message in seconds.
     */
    @Column(name = "duration_seconds", nullable = false)
    private Integer duration;

    /**
     * URL to the audio file stored in MinIO.
     */
    @Column(name = "audio_url", nullable = false, length = 1024)
    private String audioUrl;

    /**
     * JSON string representing the simplified waveform of the audio.
     */
    @Column(name = "waveform_data", columnDefinition = "TEXT")
    private String waveform;

    /**
     * Size of the audio file in bytes.
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * MIME type of the audio file (e.g., audio/ogg, audio/mp4).
     */
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    /**
     * Timestamp when the voice message was created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Flag indicating whether the voice message has been played by the recipient.
     */
    @Column(name = "is_played", nullable = false)
    @Builder.Default
    private boolean isPlayed = false;

    /**
     * Transcribed text content of the voice message (optional).
     */
    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    /**
     * Creates a new VoiceMessage.
     *
     * @param messageId the unique message identifier
     * @param chatId    the chat identifier
     * @param senderId  the sender identifier
     * @param audioUrl  the URL to the audio file
     * @param duration  the duration in seconds
     * @param waveform  the waveform data as JSON string
     * @param fileSize  the file size in bytes
     * @param mimeType  the MIME type of the audio
     * @return new VoiceMessage instance
     */
    public static VoiceMessage create(java.util.UUID messageId, java.util.UUID chatId, java.util.UUID senderId,
            String audioUrl, Integer duration, String waveform,
            Long fileSize, String mimeType) {
        return VoiceMessage.builder()
                .messageId(messageId)
                .chatId(chatId)
                .senderId(senderId)
                .audioUrl(audioUrl)
                .duration(duration)
                .waveform(waveform)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .createdAt(Instant.now())
                .isPlayed(false)
                .build();
    }

    /**
     * Marks the voice message as played.
     */
    public void markAsPlayed() {
        this.isPlayed = true;
    }

    /**
     * Updates the transcription content.
     *
     * @param transcript the transcribed text
     */
    public void updateTranscript(String transcript) {
        this.transcript = transcript;
    }
}
