package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorDTO {
    private String error;
    private String message;
    private String timestamp;
    private String path;
    
    public ErrorDTO(String error) {
        this.error = error;
        this.message = error;
        this.timestamp = LocalDateTime.now().toString();
    }
}
