package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceMessageDTO {
    private String audioUrl;
    private Integer durationSeconds;
    private Long fileSize;
    private String mimeType;
    private String waveformData;
}
