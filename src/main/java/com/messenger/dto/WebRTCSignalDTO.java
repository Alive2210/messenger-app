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
    private String senderId;
    private Object payload;

    public WebRTCSignalDTO withSender(String sender) {
        this.senderId = sender;
        return this;
    }
}
