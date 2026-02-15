package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaStateDTO {
    private String username;
    private boolean videoEnabled;
    private boolean audioEnabled;
    private boolean screenSharing;
}
