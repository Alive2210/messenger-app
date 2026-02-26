package com.messenger.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * DTO for typing indicator events.
 * Used for transferring typing status between client and server via WebSocket.
 */
@Data
@Builder
public class TypingEventDTO {

    /**
     * Unique identifier of the chat/conversation.
     */
    private String chatId;

    /**
     * Unique identifier of the user who is typing.
     */
    private String userId;

    /**
     * Username of the user who is typing (for display purposes).
     */
    private String username;

    /**
     * Whether the user is currently typing.
     * true = started typing, false = stopped typing.
     */
    private Boolean isTyping;

    /**
     * Timestamp when the typing event occurred.
     */
    private Instant timestamp;
}
