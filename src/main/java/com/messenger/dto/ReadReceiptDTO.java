package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadReceiptDTO {
    private String username;
    private UUID lastReadMessageId;
    private UUID chatId;
    
    public ReadReceiptDTO(String username, UUID lastReadMessageId) {
        this.username = username;
        this.lastReadMessageId = lastReadMessageId;
    }
}
