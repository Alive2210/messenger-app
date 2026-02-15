package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAttachmentDTO {
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private String thumbnailUrl;
}
