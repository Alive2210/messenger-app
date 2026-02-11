package com.messenger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "video_conferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoConference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", unique = true, nullable = false)
    private String roomId; // Unique room identifier for WebRTC

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @Column(name = "conference_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConferenceType conferenceType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConferenceStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "conference", cascade = CascadeType.ALL)
    private Set<ConferenceParticipant> participants = new HashSet<>();

    @Column(name = "recording_url")
    private String recordingUrl; // If conference is recorded

    @Column(name = "is_recorded")
    private Boolean isRecorded = false;

    @Column(name = "max_participants")
    private Integer maxParticipants = 50;

    @Version
    private Long version;

    public enum ConferenceType {
        AUDIO,      // Audio only
        VIDEO,      // Video conference
        SCREEN_SHARE // Screen sharing
    }

    public enum ConferenceStatus {
        SCHEDULED,
        ACTIVE,
        ENDED,
        CANCELLED
    }
}
