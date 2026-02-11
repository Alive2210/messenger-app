package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationDTOs {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationShareRequest {
        private UUID chatId;
        private double latitude;
        private double longitude;
        private String label;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationShareDTO {
        private UUID chatId;
        private double latitude;
        private double longitude;
        private String label;
        private String senderUsername;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationShareEventDTO {
        private String eventType;
        private LocationShareDTO location;
    }
}
