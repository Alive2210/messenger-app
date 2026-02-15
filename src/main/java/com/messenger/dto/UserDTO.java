package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private String id;
    private String username;
    private String email;
    private String publicKey;
    private String avatarUrl;
    private String phoneNumber;
    private String statusMessage;
    private boolean isOnline;
    private String lastSeen;
}
