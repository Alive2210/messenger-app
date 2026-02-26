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
 * JPA entity for storing message reactions (emoji).
 * Tracks when a user reacts to a message with an emoji.
 */
@Entity
@Table(
        name = "message_reactions",
        indexes = {
                @Index(name = "idx_reaction_message_id", columnList = "messageId"),
                @Index(name = "idx_reaction_user_id", columnList = "userId"),
                @Index(name = "idx_reaction_emoji", columnList = "emojiCode"),
                @Index(name = "idx_reaction_timestamp", columnList = "timestamp"),
                @Index(name = "idx_reaction_message_user", columnList = "messageId, userId"),
                @Index(name = "idx_reaction_message_emoji", columnList = "messageId, emojiCode")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReaction {

    /**
     * Primary key identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier of the message being reacted to.
     */
    @Column(name = "message_id", nullable = false)
    private String messageId;

    /**
     * Unique identifier of the user who created the reaction.
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * Unicode emoji code or shortcode (e.g., "👍", "❤️", ":thumbsup:").
     */
    @Column(name = "emoji_code", nullable = false, length = 50)
    private String emojiCode;

    /**
     * Timestamp when the reaction was created.
     */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /**
     * Creates a new MessageReaction.
     *
     * @param messageId the message identifier
     * @param userId    the user who created the reaction
     * @param emojiCode the emoji code
     * @return new MessageReaction instance
     */
    public static MessageReaction create(String messageId, String userId, String emojiCode) {
        return MessageReaction.builder()
                .messageId(messageId)
                .userId(userId)
                .emojiCode(emojiCode)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Updates the emoji code for this reaction.
     *
     * @param emojiCode the new emoji code
     */
    public void updateEmoji(String emojiCode) {
        this.emojiCode = emojiCode;
        this.timestamp = Instant.now();
    }
}
