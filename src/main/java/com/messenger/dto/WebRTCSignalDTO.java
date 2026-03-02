package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebRTCSignalDTO {
    private String type;
    private String targetUserId;
    // Use "sender" to match the client-side field name
    private String sender;
    // Use "data" to match the client-side field name
    private Object data;

    public WebRTCSignalDTO withSender(String senderName) {
        this.sender = senderName;
        return this;
    }
}
