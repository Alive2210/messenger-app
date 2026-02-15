package com.messenger.controller;

import com.messenger.dto.*;
import com.messenger.service.MessageService;
import com.messenger.service.ReactionService;
import com.messenger.service.VideoConferenceService;
import com.messenger.service.VideoStreamBuffer;
import com.messenger.service.VideoReconnectService;
import com.messenger.service.WebRtcConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final VideoConferenceService videoConferenceService;
    private final WebRtcConfigurationService webRtcConfigurationService;
    private final ReactionService reactionService;
    private final VideoStreamBuffer videoStreamBuffer;
    private final VideoReconnectService videoReconnectService;

    /**
     * Handle text messages
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        try {
            String username = principal.getName();
            log.info("Received message from {} to chat {}", username, request.getChatId());

            MessageDTO message = messageService.sendMessage(request, username);

            // Send to all participants in the chat
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + request.getChatId(),
                    message
            );

            // Send delivery receipt to sender
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/message-status",
                    new MessageStatusDTO(message.getId(), "SENT")
            );

        } catch (Exception e) {
            log.error("Error processing message", e);
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    new ErrorDTO("Failed to send message: " + e.getMessage())
            );
        }
    }

    /**
     * Handle typing indicator
     */
    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload TypingRequest request, Principal principal) {
        TypingEventDTO event = new TypingEventDTO(
                principal.getName(),
                request.getChatId(),
                request.isTyping()
        );

        messagingTemplate.convertAndSend(
                "/topic/chat/" + request.getChatId() + "/typing",
                event
        );
    }

    /**
     * Handle read receipts
     */
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload ReadReceiptRequest request, Principal principal) {
        messageService.markMessagesAsRead(
                request.getChatId(),
                principal.getName()
        );

        // Notify other participants
        messagingTemplate.convertAndSend(
                "/topic/chat/" + request.getChatId() + "/read",
                new ReadReceiptDTO(principal.getName(), request.getLastReadMessageId())
        );
    }

    /**
     * Handle WebRTC signaling for video calls
     */
    @MessageMapping("/webrtc.offer")
    public void handleOffer(@Payload WebRTCSignalDTO signal, Principal principal) {
        log.info("WebRTC offer from {} to {}", principal.getName(), signal.getTargetUserId());
        
        messagingTemplate.convertAndSendToUser(
                signal.getTargetUserId(),
                "/queue/webrtc",
                signal.withSender(principal.getName())
        );
    }

    @MessageMapping("/webrtc.answer")
    public void handleAnswer(@Payload WebRTCSignalDTO signal, Principal principal) {
        log.info("WebRTC answer from {} to {}", principal.getName(), signal.getTargetUserId());
        
        messagingTemplate.convertAndSendToUser(
                signal.getTargetUserId(),
                "/queue/webrtc",
                signal.withSender(principal.getName())
        );
    }

    @MessageMapping("/webrtc.ice-candidate")
    public void handleIceCandidate(@Payload WebRTCSignalDTO signal, Principal principal) {
        messagingTemplate.convertAndSendToUser(
                signal.getTargetUserId(),
                "/queue/webrtc",
                signal.withSender(principal.getName())
        );
    }

    /**
     * Получить конфигурацию WebRTC (ICE серверы, настройки видео/аудио)
     */
    @MessageMapping("/webrtc.config")
    public void getWebRtcConfig(Principal principal) {
        log.info("Sending WebRTC configuration to {}", principal.getName());
        
        Map<String, Object> config = webRtcConfigurationService.getFullConfiguration();
        
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/webrtc-config",
                config
        );
    }

    /**
     * Handle video conference joining
     */
    @MessageMapping("/conference.join")
    public void joinConference(@Payload JoinConferenceRequest request, 
                               Principal principal,
                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            UUID conferenceId = UUID.fromString(request.getConferenceId());
            String sessionId = headerAccessor.getSessionId();
            
            var participant = videoConferenceService.joinConference(
                    conferenceId, 
                    principal.getName(),
                    request.isVideoEnabled(),
                    request.isAudioEnabled()
            );

            // Register video session for reconnection support
            videoReconnectService.registerVideoSession(
                    sessionId,
                    request.getConferenceId(),
                    principal.getName(),
                    request.getDeviceId() != null ? request.getDeviceId() : "default"
            );
            
            log.info("🎥 Video session registered for {} in conference {}", 
                    principal.getName(), request.getConferenceId());

            // Notify all conference participants
            messagingTemplate.convertAndSend(
                    "/topic/conference/" + request.getConferenceId(),
                    new ConferenceEventDTO(
                            "PARTICIPANT_JOINED",
                            participant,
                            principal.getName()
                    )
            );

            // Store conference ID in session
            headerAccessor.getSessionAttributes().put("conferenceId", request.getConferenceId());

        } catch (Exception e) {
            log.error("Error joining conference", e);
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    new ErrorDTO("Failed to join conference: " + e.getMessage())
            );
        }
    }

    /**
     * Handle leaving conference
     */
    @MessageMapping("/conference.leave")
    public void leaveConference(@Payload LeaveConferenceRequest request, 
                                Principal principal,
                                SimpMessageHeaderAccessor headerAccessor) {
        try {
            UUID conferenceId = UUID.fromString(request.getConferenceId());
            String sessionId = headerAccessor.getSessionId();
            
            videoConferenceService.leaveConference(conferenceId, principal.getName());

            // Handle video disconnection with grace period
            videoReconnectService.handleVideoDisconnection(
                    sessionId,
                    request.getConferenceId(),
                    principal.getName(),
                    "user_left"
            );
            
            log.info("🔌 Video session disconnected for {} in conference {}. Grace period started.",
                    principal.getName(), request.getConferenceId());

            messagingTemplate.convertAndSend(
                    "/topic/conference/" + request.getConferenceId(),
                    new ConferenceEventDTO(
                            "PARTICIPANT_LEFT",
                            null,
                            principal.getName()
                    )
            );

        } catch (Exception e) {
            log.error("Error leaving conference", e);
        }
    }
    
    /**
     * Handle video session reconnection
     */
    @MessageMapping("/video.reconnect")
    public void handleVideoReconnection(@Payload VideoReconnectionRequest request,
                                        Principal principal,
                                        SimpMessageHeaderAccessor headerAccessor) {
        try {
            String newSessionId = headerAccessor.getSessionId();
            
            boolean success = videoReconnectService.confirmVideoReconnection(
                    request.getOldSessionId(),
                    newSessionId,
                    request.getConferenceId(),
                    principal.getName()
            );
            
            if (success) {
                // Notify about successful reconnection
                messagingTemplate.convertAndSend(
                        "/topic/conference/" + request.getConferenceId(),
                        new ConferenceEventDTO(
                                "PARTICIPANT_RECONNECTED",
                                null,
                                principal.getName()
                        )
                );
                
                // Send video recovery response automatically
                handleAutomaticVideoRecovery(request.getConferenceId(), principal);
                
                log.info("✅ Video session {} reconnected as {} for {}",
                        request.getOldSessionId(), newSessionId, principal.getName());
            } else {
                // Grace period expired, need to rejoin
                messagingTemplate.convertAndSendToUser(
                        principal.getName(),
                        "/queue/video-reconnect-failed",
                        new ErrorDTO("Grace period expired. Please rejoin conference.")
                );
            }
            
        } catch (Exception e) {
            log.error("Error handling video reconnection", e);
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    new ErrorDTO("Reconnection failed: " + e.getMessage())
            );
        }
    }
    
    private void handleAutomaticVideoRecovery(String conferenceId, Principal principal) {
        // Automatically request video recovery for reconnected session
        byte[][] frames = videoStreamBuffer.getLastFrames(conferenceId, principal.getName(), 30);
        
        if (frames.length > 0) {
            String[] base64Frames = new String[frames.length];
            for (int i = 0; i < frames.length; i++) {
                base64Frames[i] = Base64.getEncoder().encodeToString(frames[i]);
            }
            
            ReconnectionDTOs.VideoRecoveryResponse response = 
                    ReconnectionDTOs.VideoRecoveryResponse.builder()
                            .success(true)
                            .conferenceId(conferenceId)
                            .participantId(principal.getName())
                            .frames(base64Frames)
                            .totalFrames(frames.length)
                            .lastSequence(System.currentTimeMillis())
                            .build();
            
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/video-recovery",
                    response
            );
        }
    }
    
    /**
     * DTO for video reconnection request
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class VideoReconnectionRequest {
        private String oldSessionId;
        private String conferenceId;
        private String deviceId;
    }

    /**
     * Handle media state changes (mute/unmute, video on/off)
     */
    @MessageMapping("/conference.media-state")
    public void updateMediaState(@Payload MediaStateRequest request, Principal principal) {
        try {
            UUID conferenceId = UUID.fromString(request.getConferenceId());
            videoConferenceService.updateMediaState(
                    conferenceId,
                    principal.getName(),
                    request.isVideoEnabled(),
                    request.isAudioEnabled(),
                    request.isScreenSharing()
            );

            messagingTemplate.convertAndSend(
                    "/topic/conference/" + request.getConferenceId() + "/media",
                    new MediaStateDTO(
                            principal.getName(),
                            request.isVideoEnabled(),
                            request.isAudioEnabled(),
                            request.isScreenSharing()
                    )
            );

        } catch (Exception e) {
            log.error("Error updating media state", e);
        }
    }

    /**
     * Handle user connecting
     */
    @MessageMapping("/user.connect")
    public void userConnected(Principal principal) {
        log.info("User connected: {}", principal.getName());
        // Update user online status
        messageService.updateUserOnlineStatus(principal.getName(), true);
        
        // Notify contacts
        messagingTemplate.convertAndSend(
                "/topic/user/" + principal.getName() + "/status",
                new UserStatusDTO(principal.getName(), true, null)
        );
    }

    /**
     * Handle user disconnect
     */
    @MessageMapping("/user.disconnect")
    public void userDisconnected(Principal principal) {
        log.info("User disconnected: {}", principal.getName());
        messageService.updateUserOnlineStatus(principal.getName(), false);
        
        messagingTemplate.convertAndSend(
                "/topic/user/" + principal.getName() + "/status",
                new UserStatusDTO(principal.getName(), false, java.time.LocalDateTime.now())
        );
    }

    // Location sharing via WebSocket was removed from this version to keep stability

    /**
     * Handle reaction add via WebSocket
     */
    @MessageMapping("/reaction.add")
    public void addReaction(@Payload ReactionDTOs.AddReactionRequest request, Principal principal) {
        try {
            String username = principal.getName();
            log.info("WebSocket: User {} adding reaction {} to message {}",
                    username, request.getEmojiCode(), request.getMessageId());

            ReactionDTOs.ReactionDTO reaction = reactionService.addReaction(request, username);
            
            // Get updated summary
            ReactionDTOs.MessageReactionsDTO summary = reactionService
                    .getMessageReactionsSummary(request.getMessageId(), username);

            // Create event
            ReactionDTOs.ReactionEventDTO event = ReactionDTOs.ReactionEventDTO.builder()
                    .eventType("REACTION_ADDED")
                    .reaction(reaction)
                    .summary(summary)
                    .chatId(request.getMessageId().toString())
                    .build();

            // Broadcast to all participants in the chat
            messagingTemplate.convertAndSend(
                    "/topic/message/" + request.getMessageId() + "/reactions",
                    event
            );

        } catch (Exception e) {
            log.error("Error adding reaction via WebSocket", e);
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    new ErrorDTO("Failed to add reaction: " + e.getMessage())
            );
        }
    }

    /**
     * Handle reaction remove via WebSocket
     */
    @MessageMapping("/reaction.remove")
    public void removeReaction(@Payload ReactionDTOs.RemoveReactionRequest request, Principal principal) {
        try {
            String username = principal.getName();
            log.info("WebSocket: User {} removing reaction {} from message {}",
                    username, request.getEmojiCode(), request.getMessageId());

            reactionService.removeReaction(request, username);
            
            // Get updated summary
            ReactionDTOs.MessageReactionsDTO summary = reactionService
                    .getMessageReactionsSummary(request.getMessageId(), username);

            // Create event
            ReactionDTOs.ReactionEventDTO event = ReactionDTOs.ReactionEventDTO.builder()
                    .eventType("REACTION_REMOVED")
                    .reaction(null)
                    .summary(summary)
                    .chatId(request.getMessageId().toString())
                    .build();

            // Broadcast to all participants
            messagingTemplate.convertAndSend(
                    "/topic/message/" + request.getMessageId() + "/reactions",
                    event
            );

        } catch (Exception e) {
            log.error("Error removing reaction via WebSocket", e);
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    new ErrorDTO("Failed to remove reaction: " + e.getMessage())
            );
        }
    }

    /**
     * Handle reaction toggle via WebSocket
     */
    @MessageMapping("/reaction.toggle")
    public void toggleReaction(@Payload ReactionDTOs.AddReactionRequest request, Principal principal) {
        try {
            String username = principal.getName();
            log.info("WebSocket: User {} toggling reaction {} on message {}",
                    username, request.getEmojiCode(), request.getMessageId());

            reactionService.toggleReaction(request, username);
            
            // Get updated summary
            ReactionDTOs.MessageReactionsDTO summary = reactionService
                    .getMessageReactionsSummary(request.getMessageId(), username);

            // Determine event type
            String eventType = summary.isUserHasReacted() ? "REACTION_ADDED" : "REACTION_REMOVED";

            // Create event
            ReactionDTOs.ReactionEventDTO event = ReactionDTOs.ReactionEventDTO.builder()
                    .eventType(eventType)
                    .summary(summary)
                    .chatId(request.getMessageId().toString())
                    .build();

            // Broadcast to all participants
            messagingTemplate.convertAndSend(
                    "/topic/message/" + request.getMessageId() + "/reactions",
                    event
            );

        } catch (Exception e) {
            log.error("Error toggling reaction via WebSocket", e);
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    new ErrorDTO("Failed to toggle reaction: " + e.getMessage())
            );
        }
    }

    /**
     * Handle incoming video frame for buffering
     */
    @MessageMapping("/video.frame")
    public void handleVideoFrame(@Payload VideoFrameDTO frame, Principal principal) {
        try {
            log.trace("Received video frame from {} for conference {}", 
                    principal.getName(), frame.getConferenceId());
            
            // Decode base64 frame data
            byte[] frameData = Base64.getDecoder().decode(frame.getFrameData());
            
            // Add to buffer
            videoStreamBuffer.addFrame(
                    frame.getConferenceId(),
                    principal.getName(),
                    frameData,
                    frame.getTimestamp()
            );
            
            // Forward to target participant if specified
            if (frame.getTargetUserId() != null) {
                messagingTemplate.convertAndSendToUser(
                        frame.getTargetUserId(),
                        "/queue/video",
                        frame
                );
            }
            
        } catch (Exception e) {
            log.error("Error handling video frame", e);
        }
    }

    /**
     * Request video stream recovery after connection interruption
     */
    @MessageMapping("/video.recover")
    public void recoverVideoStream(@Payload ReconnectionDTOs.VideoRecoveryRequest request, 
                                   Principal principal) {
        try {
            log.info("Video recovery request from {} for conference {}", 
                    principal.getName(), request.getConferenceId());
            
            // Get buffered frames from the requested sequence
            byte[][] frames = videoStreamBuffer.getFrames(
                    request.getConferenceId(),
                    request.getParticipantId(),
                    request.getFromSequence()
            );
            
            if (frames.length == 0) {
                // Try to get last frames if no frames from requested sequence
                frames = videoStreamBuffer.getLastFrames(
                        request.getConferenceId(),
                        request.getParticipantId(),
                        30 // Last 30 frames (~1 second)
                );
            }
            
            // Convert frames to base64
            String[] base64Frames = new String[frames.length];
            for (int i = 0; i < frames.length; i++) {
                base64Frames[i] = Base64.getEncoder().encodeToString(frames[i]);
            }
            
            // Get buffer status
            VideoStreamBuffer.BufferStatus status = videoStreamBuffer.getStatus(
                    request.getConferenceId(),
                    request.getParticipantId()
            );
            
            ReconnectionDTOs.VideoRecoveryResponse response = 
                    ReconnectionDTOs.VideoRecoveryResponse.builder()
                            .success(frames.length > 0)
                            .conferenceId(request.getConferenceId())
                            .participantId(request.getParticipantId())
                            .frames(base64Frames)
                            .totalFrames(frames.length)
                            .lastSequence(status != null ? status.getLastSequenceNumber() : 0)
                            .build();
            
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/video-recovery",
                    response
            );
            
            log.info("Sent {} frames for recovery to {}", frames.length, principal.getName());
            
        } catch (Exception e) {
            log.error("Error recovering video stream", e);
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    new ErrorDTO("Failed to recover video: " + e.getMessage())
            );
        }
    }

    /**
     * Get buffer status for debugging
     */
    @MessageMapping("/video.buffer-status")
    public void getBufferStatus(@Payload VideoBufferStatusRequest request, Principal principal) {
        try {
            VideoStreamBuffer.BufferStatus status = videoStreamBuffer.getStatus(
                    request.getConferenceId(),
                    request.getParticipantId()
            );
            
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/buffer-status",
                    status != null ? status : new ErrorDTO("Buffer not found")
            );
            
        } catch (Exception e) {
            log.error("Error getting buffer status", e);
        }
    }

    /**
     * Handle connection interruption - client signals network issues
     */
    @MessageMapping("/connection.interrupted")
    public void handleConnectionInterrupted(@Payload ReconnectionDTOs.ConnectionInterrupted event,
                                           Principal principal) {
        log.warn("Connection interrupted for {}: {}", principal.getName(), event.getReason());
        
        // Store state for potential recovery
        // Buffer remains active for reconnection window
    }

    /**
     * DTO for video frame
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class VideoFrameDTO {
        private String conferenceId;
        private String targetUserId;
        private String frameData; // Base64 encoded
        private long timestamp;
        private long sequenceNumber;
        private String codec;
    }

    /**
     * DTO for buffer status request
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VideoBufferStatusRequest {
        private String conferenceId;
        private String participantId;
    }
}
