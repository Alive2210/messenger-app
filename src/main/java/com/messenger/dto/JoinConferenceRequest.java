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
public class JoinConferenceRequest {
    private String conferenceId;
    private boolean videoEnabled;
    private boolean audioEnabled;
    private String deviceId;
}
