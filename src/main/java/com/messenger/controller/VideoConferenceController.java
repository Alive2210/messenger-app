package com.messenger.controller;

import com.messenger.dto.*;
import com.messenger.entity.VideoConference;
import com.messenger.service.VideoConferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/conferences")
@RequiredArgsConstructor
public class VideoConferenceController {

    private final VideoConferenceService conferenceService;

    @PostMapping("/chats/{chatId}")
    public ResponseEntity<ConferenceDTO> createConference(
            @PathVariable UUID chatId,
            @RequestParam String type,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Creating {} conference for chat {} by {}", type, chatId, userDetails.getUsername());
        
        VideoConference conference = conferenceService.createConference(
                chatId, 
                userDetails.getUsername(),
                VideoConference.ConferenceType.valueOf(type.toUpperCase())
        );
        
        return ResponseEntity.ok(mapToDTO(conference));
    }

    @GetMapping("/{conferenceId}")
    public ResponseEntity<ConferenceDTO> getConference(
            @PathVariable UUID conferenceId) {
        
        // TODO: implement get by id
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{conferenceId}/participants")
    public ResponseEntity<List<ParticipantDTO>> getParticipants(
            @PathVariable UUID conferenceId) {
        
        var participants = conferenceService.getConferenceParticipants(conferenceId);
        
        List<ParticipantDTO> dtoList = participants.stream()
                .map(p -> ParticipantDTO.builder()
                        .userId(p.getUser().getId().toString())
                        .username(p.getUser().getUsername())
                        .avatarUrl(p.getUser().getAvatarUrl())
                        .isVideoEnabled(p.getIsVideoEnabled())
                        .isAudioEnabled(p.getIsAudioEnabled())
                        .isScreenSharing(p.getIsScreenSharing())
                        .isHost(p.getIsHost())
                        .joinedAt(p.getJoinedAt())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/{conferenceId}/end")
    public ResponseEntity<Void> endConference(
            @PathVariable UUID conferenceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Ending conference {} by {}", conferenceId, userDetails.getUsername());
        conferenceService.endConference(conferenceId, userDetails.getUsername());
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/chats/{chatId}/active")
    public ResponseEntity<ConferenceDTO> getActiveConference(
            @PathVariable UUID chatId) {
        
        // TODO: implement
        return ResponseEntity.ok().build();
    }

    private ConferenceDTO mapToDTO(VideoConference conference) {
        return ConferenceDTO.builder()
                .id(conference.getId())
                .roomId(conference.getRoomId())
                .chatId(conference.getChat().getId())
                .initiatorId(conference.getInitiator().getId().toString())
                .initiatorUsername(conference.getInitiator().getUsername())
                .conferenceType(conference.getConferenceType().name())
                .status(conference.getStatus().name())
                .startedAt(conference.getStartedAt())
                .isRecorded(conference.getIsRecorded())
                .maxParticipants(conference.getMaxParticipants())
                .build();
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ConferenceDTO {
        private UUID id;
        private String roomId;
        private UUID chatId;
        private String initiatorId;
        private String initiatorUsername;
        private String conferenceType;
        private String status;
        private java.time.LocalDateTime startedAt;
        private Boolean isRecorded;
        private Integer maxParticipants;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ParticipantDTO {
        private String userId;
        private String username;
        private String avatarUrl;
        private Boolean isVideoEnabled;
        private Boolean isAudioEnabled;
        private Boolean isScreenSharing;
        private Boolean isHost;
        private java.time.LocalDateTime joinedAt;
    }
}
