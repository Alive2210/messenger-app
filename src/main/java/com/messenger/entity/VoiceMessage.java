package com.messenger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity for storing voice messages.
 * Represents an audio message sent by a user in a chat.
 */
@Entity
@Table(
        name = "voice_messages",
        indexes = {
                @Index(name = "idx_voice_message_id", columnList = "messageId"),
                @Index(name = "idx_voice_chat_id", columnList = "chatId"),
                @Index(name = "idx_voice_sender_id", columnList = "senderId"),
                @Index(name = "idx_voice_created_at", columnList = "createdAt"),
                @Index(name = "idx_voice_is_played", columnList = "isPlayed"),
                @Index(name = "idx_voice_duration", columnList = "duration")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceMessage {

    /**
     * Primary key identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier of the voice message.
     */
    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId;

    /**
     * Unique identifier of the chat/conversation.
     */
    @Column(name = "chat_id", nullable = false)
    private String chatId;

    /**
     * Unique identifier of the user who sent the voice message.
     */
    @Column(name = "sender_id", nullable = false)
    private String senderId;

    /**
     * Duration of the voice message in seconds.
     */
    @Column(name = "duration", nullable = false)
    private Integer duration;

    /**
     * URL to the audio file stored in MinIO.
     */
    @Column(name = "audio_url", nullable = false, length = 1024)
    private String audioUrl;

    /**
     * Waveform data as JSON array representing audio amplitude visualization.
     * Example: [0.2, 0.5, 0.8, 0.3, 0.1]
     */
    @Column(name = "waveform", columnDefinition = "TEXT")
    private String waveform;

    /**
     * File size in bytes.
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
     * Whether the voice message has been played by the recipient.
     */
    @Column(name = "is_played", nullable = false)
    @Builder.Default
    private Boolean isPlayed = false;

    /**
     * Text transcript of the voice message (from speech-to-text).
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
    public static VoiceMessage create(String messageId, String chatId, String senderId,
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
     * Updates the transcript of the voice message.
     *
     * @param transcript the text transcript
     */
    public void updateTranscript(String transcript) {
        this.transcript = transcript;
    }

    /**
     * Checks if the voice message has been played.
     *
     * @return true if played
     */
    public boolean isPlayed() {
        return this.isPlayed != null && this.isPlayed;
    }
}
