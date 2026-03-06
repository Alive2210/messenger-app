package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for read receipt information.
 * Used for transferring read receipt status between client and server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptDTO {

    /**
     * Unique identifier of the message.
     */
    private String messageId;

    /**
     * Unique identifier of the user who read the message.
     */
    private String userId;

    /**
     * Status of the receipt (DELIVERED or READ).
     */
    private ReceiptStatus status;

    /**
     * Timestamp when the status was updated.
     */
    private Instant timestamp;

    /**
     * Enum representing the receipt status.
     */
    public enum ReceiptStatus {
        /**
         * Message has been delivered to the recipient's device.
         */
        DELIVERED,

        /**
         * Message has been read by the recipient.
         */
        READ
    }
}
