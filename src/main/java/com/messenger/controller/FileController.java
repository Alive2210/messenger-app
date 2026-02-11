package com.messenger.controller;

import com.messenger.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
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
                    format
            );
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

    @DeleteMapping("/{fileName}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileName,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Deleting file: {} by user: {}", fileName, userDetails.getUsername());
        fileStorageService.deleteFile(fileName);
        
        return ResponseEntity.ok().build();
    }
}
