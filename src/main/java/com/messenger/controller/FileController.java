package com.messenger.controller;

import com.messenger.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Uploading file: {} by user: {}", file.getOriginalFilename(), userDetails.getUsername());

        String fileName = fileStorageService.uploadFile(file, userDetails.getUsername());
        String fileUrl = fileStorageService.getFileUrl(fileName);

        Map<String, String> response = new HashMap<>();
        response.put("fileName", fileName);
        response.put("fileUrl", fileUrl);
        response.put("originalName", file.getOriginalFilename());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/voice")
    public ResponseEntity<Map<String, String>> uploadVoiceMessage(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam("duration") Integer durationSeconds,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Uploading voice message: {} seconds by user: {}",
                durationSeconds, userDetails.getUsername());

        try {
            String format = audioFile.getContentType().contains("ogg") ? "ogg" : "mp3";
            String fileName = fileStorageService.uploadVoiceMessage(
                    audioFile.getBytes(),
                    userDetails.getUsername(),
                    format);
            String fileUrl = fileStorageService.getFileUrl(fileName);

            Map<String, String> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("fileUrl", fileUrl);
            response.put("durationSeconds", durationSeconds.toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading voice message", e);
            throw new RuntimeException("Failed to upload voice message", e);
        }
    }

    @GetMapping("/url/{fileName}")
    public ResponseEntity<Map<String, String>> getFileUrl(@PathVariable String fileName) {
        String url = fileStorageService.getFileUrl(fileName);

        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", url);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/presigned/{fileName}")
    public ResponseEntity<Map<String, String>> getPresignedUrl(
            @PathVariable String fileName,
            @RequestParam(value = "download", defaultValue = "false") boolean download) {
        try {
            String url = fileStorageService.getFileUrl(fileName);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", url);
            response.put("download", String.valueOf(download));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating presigned URL for: {}", fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Downloading file: {} by user: {}", fileName, userDetails.getUsername());

        try {
            byte[] fileData = fileStorageService.downloadFile(fileName);
            String contentType = determineContentType(fileName);

            // Cache headers for better performance
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                    .header(HttpHeaders.ETAG, "\"" + fileName.hashCode() + "\"")
                    .body(new ByteArrayResource(fileData));
        } catch (Exception e) {
            log.error("Error downloading file: {}", fileName, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Stream file with proper HTTP streaming for large files
     * Supports range requests for video/audio seeking
     */
    @GetMapping("/stream/{fileName}")
    public ResponseEntity<Resource> streamFile(
            @PathVariable String fileName,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Streaming file: {} by user: {}", fileName, userDetails.getUsername());

        try {
            byte[] fileData = fileStorageService.downloadFile(fileName);
            String contentType = determineContentType(fileName);
            Resource resource = new ByteArrayResource(fileData);

            // For now, return full file - can be enhanced for true range support
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileData.length))
                    .body(resource);
        } catch (Exception e) {
            log.error("Error streaming file: {}", fileName, e);
            return ResponseEntity.notFound().build();
        }
    }

    private String determineContentType(String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".webp")) return "image/webp";
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".doc")) return "application/msword";
        if (fileName.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) return "application/vnd.ms-excel";
        if (fileName.endsWith(".txt")) return "text/plain";
        if (fileName.endsWith(".mp3")) return "audio/mpeg";
        if (fileName.endsWith(".wav")) return "audio/wav";
        if (fileName.endsWith(".ogg")) return "audio/ogg";
        if (fileName.endsWith(".mp4")) return "video/mp4";
        return "application/octet-stream";
    }

    @DeleteMapping("/{fileName}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileName,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Deleting file: {} by user: {}", fileName, userDetails.getUsername());
        fileStorageService.deleteFile(fileName);

        return ResponseEntity.ok().build();
    }

    // --- Resumable Upload Endpoints ---

    @PostMapping("/upload/init")
    public ResponseEntity<Map<String, String>> initResumableUpload(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String fileName = (String) request.get("fileName");
        Integer totalChunks = (Integer) request.get("totalChunks");

        log.info("Initializing resumable upload: {} with {} chunks", fileName, totalChunks);
        String sessionId = fileStorageService.initResumableUpload(fileName, userDetails.getUsername(), totalChunks);

        Map<String, String> response = new HashMap<>();
        response.put("sessionId", sessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload/chunk")
    public ResponseEntity<Map<String, Object>> uploadChunk(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("file") MultipartFile file) {

        log.trace("Uploading chunk {} for session {}", chunkIndex, sessionId);
        boolean success = fileStorageService.uploadChunk(sessionId, chunkIndex, file);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("chunkIndex", chunkIndex);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/upload/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getUploadStatus(@PathVariable String sessionId) {
        List<Integer> missingChunks = fileStorageService.getMissingChunks(sessionId);
        boolean isComplete = missingChunks.isEmpty();

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("missingChunks", missingChunks);
        response.put("isComplete", isComplete);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload/complete/{sessionId}")
    public ResponseEntity<Map<String, String>> completeUpload(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Completing upload for session {}", sessionId);
        String fileName = fileStorageService.completeUpload(sessionId, userDetails.getUsername());
        String fileUrl = fileStorageService.getFileUrl(fileName);

        Map<String, String> response = new HashMap<>();
        response.put("fileName", fileName);
        response.put("fileUrl", fileUrl);
        return ResponseEntity.ok(response);
    }
}
