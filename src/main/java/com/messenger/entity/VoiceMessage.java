package com.messenger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "voice_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "audio_url", nullable = false)
    private String audioUrl; // MinIO/S3 URL

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "waveform_data", columnDefinition = "TEXT")
    private String waveformData; // JSON array of amplitude values for visualization

    @Column(name = "is_listened")
    private Boolean isListened = false;

    @Column(name = "is_encrypted")
    private Boolean isEncrypted = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
