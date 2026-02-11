package com.messenger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_chat_created", columnList = "chat_id, created_at"),
    @Index(name = "idx_sender", columnList = "sender_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "message_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Column(name = "encrypted_content", columnDefinition = "TEXT")
    private String encryptedContent; // Encrypted message content

    @Column(name = "encryption_iv")
    private String encryptionIv; // Initialization vector for decryption

    @OneToOne(mappedBy = "message", cascade = CascadeType.ALL)
    private FileAttachment fileAttachment;

    @OneToOne(mappedBy = "message", cascade = CascadeType.ALL)
    private VoiceMessage voiceMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "is_edited")
    private Boolean isEdited = false;

    @Column(name = "location_lat")
    private Double locationLat;

    @Column(name = "location_lng")
    private Double locationLng;

    @Column(name = "location_label")
    private String locationLabel;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private Set<MessageStatus> messageStatuses = new HashSet<>();

    @Version
    private Long version;

    public enum MessageType {
        TEXT,
        FILE,
        VOICE,
        IMAGE,
        VIDEO,
        SYSTEM,
        LOCATION
    }
}
