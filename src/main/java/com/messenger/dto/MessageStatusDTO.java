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
public class MessageStatusDTO {
    private UUID messageId;
    private String status;
    private String userId;
    private String username;
    
    public MessageStatusDTO(UUID messageId, String status) {
        this.messageId = messageId;
        this.status = status;
    }
}
