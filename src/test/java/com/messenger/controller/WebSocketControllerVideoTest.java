package com.messenger.controller;

import com.messenger.dto.*;
import com.messenger.entity.ConferenceParticipant;
import com.messenger.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@DisplayName("WebSocketController Video Tests")
@ExtendWith(MockitoExtension.class)
class WebSocketControllerVideoTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private VideoStreamBuffer videoStreamBuffer;

    @Mock
    private VideoReconnectService videoReconnectService;

    @Mock
    private MessageService messageService;

    @Mock
    private VideoConferenceService videoConferenceService;

    @Mock
    private WebRtcConfigurationService webRtcConfigurationService;

    @Mock
    private ReactionService reactionService;

    @Mock
    private Principal principal;

    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    @InjectMocks
    private WebSocketController webSocketController;

    @BeforeEach
    void setUp() {
        lenient().when(principal.getName()).thenReturn("test-user");
        lenient().when(headerAccessor.getSessionId()).thenReturn("test-session-id");
        Map<String, Object> sessionAttributes = new HashMap<>();
        lenient().when(headerAccessor.getSessionAttributes()).thenReturn(sessionAttributes);
    }

    @Test
    @DisplayName("Should handle video frame and forward to buffer")
    void shouldHandleVideoFrameAndForwardToBuffer() {
        // Given
        WebSocketController.VideoFrameDTO frame = WebSocketController.VideoFrameDTO.builder()
                .conferenceId("conf-1")
                .targetUserId("target-user")
                .frameData(Base64.getEncoder().encodeToString(new byte[]{0x01, 0x02, 0x03}))
                .timestamp(System.currentTimeMillis())
                .sequenceNumber(1)
                .codec("VP8")
                .build();

        // When
        webSocketController.handleVideoFrame(frame, principal);

        // Then
        verify(videoStreamBuffer).addFrame(
                eq("conf-1"),
                eq("test-user"),
                any(byte[].class),
                anyLong()
        );
        verify(messagingTemplate).convertAndSendToUser(
                eq("target-user"),
                eq("/queue/video"),
                eq(frame)
        );
    }

    @Test
    @DisplayName("Should register video session on conference join")
    void shouldRegisterVideoSessionOnConferenceJoin() {
        // Given
        String conferenceId = UUID.randomUUID().toString();
        JoinConferenceRequest request = new JoinConferenceRequest();
        request.setConferenceId(conferenceId);
        request.setVideoEnabled(true);
        request.setAudioEnabled(true);
        request.setDeviceId("device-1");
        
        // Mock the conference service to return a participant
        ConferenceParticipant participant = new ConferenceParticipant();
        when(videoConferenceService.joinConference(any(), eq("test-user"), eq(true), eq(true)))
                .thenReturn(participant);

        // When
        webSocketController.joinConference(request, principal, headerAccessor);

        // Then
        verify(videoReconnectService).registerVideoSession(
                eq("test-session-id"),
                eq(conferenceId),
                eq("test-user"),
                eq("device-1")
        );
    }

    @Test
    @DisplayName("Should start grace period on conference leave")
    void shouldStartGracePeriodOnConferenceLeave() {
        // Given
        String conferenceId = UUID.randomUUID().toString();
        LeaveConferenceRequest request = new LeaveConferenceRequest();
        request.setConferenceId(conferenceId);
        
        // Mock the conference service
        doNothing().when(videoConferenceService).leaveConference(any(), eq("test-user"));

        // When
        webSocketController.leaveConference(request, principal, headerAccessor);

        // Then
        verify(videoReconnectService).handleVideoDisconnection(
                eq("test-session-id"),
                eq(conferenceId),
                eq("test-user"),
                eq("user_left")
        );
    }

    @Test
    @DisplayName("Should recover video stream on request")
    void shouldRecoverVideoStreamOnRequest() {
        // Given
        ReconnectionDTOs.VideoRecoveryRequest request = ReconnectionDTOs.VideoRecoveryRequest.builder()
                .conferenceId("conf-1")
                .participantId("participant-1")
                .fromSequence(100)
                .build();

        byte[][] mockFrames = new byte[][]{
                new byte[]{0x01},
                new byte[]{0x02},
                new byte[]{0x03}
        };

        when(videoStreamBuffer.getFrames("conf-1", "participant-1", 100))
                .thenReturn(mockFrames);

        VideoStreamBuffer.BufferStatus mockStatus = new VideoStreamBuffer.BufferStatus();
        mockStatus.setLastSequenceNumber(103);
        when(videoStreamBuffer.getStatus("conf-1", "participant-1"))
                .thenReturn(mockStatus);

        // When
        webSocketController.recoverVideoStream(request, principal);

        // Then
        ArgumentCaptor<ReconnectionDTOs.VideoRecoveryResponse> responseCaptor = 
                ArgumentCaptor.forClass(ReconnectionDTOs.VideoRecoveryResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("test-user"),
                eq("/queue/video-recovery"),
                responseCaptor.capture()
        );

        ReconnectionDTOs.VideoRecoveryResponse response = responseCaptor.getValue();
        assertTrue(response.isSuccess());
        assertEquals(3, response.getTotalFrames());
        assertEquals(103, response.getLastSequence());
    }

    @Test
    @DisplayName("Should fallback to last frames if requested not available")
    void shouldFallbackToLastFramesIfRequestedNotAvailable() {
        // Given
        ReconnectionDTOs.VideoRecoveryRequest request = ReconnectionDTOs.VideoRecoveryRequest.builder()
                .conferenceId("conf-1")
                .participantId("participant-1")
                .fromSequence(1000)
                .build();

        // Empty result for specific sequence
        when(videoStreamBuffer.getFrames("conf-1", "participant-1", 1000))
                .thenReturn(new byte[0][]);

        // But have last frames available
        byte[][] lastFrames = new byte[][]{
                new byte[]{0x01},
                new byte[]{0x02}
        };
        when(videoStreamBuffer.getLastFrames("conf-1", "participant-1", 30))
                .thenReturn(lastFrames);

        // When
        webSocketController.recoverVideoStream(request, principal);

        // Then
        verify(videoStreamBuffer).getLastFrames("conf-1", "participant-1", 30);
    }

    @Test
    @DisplayName("Should handle successful video reconnection")
    void shouldHandleSuccessfulVideoReconnection() {
        // Given
        WebSocketController.VideoReconnectionRequest request = 
                WebSocketController.VideoReconnectionRequest.builder()
                        .oldSessionId("old-session")
                        .conferenceId("conf-1")
                        .deviceId("device-1")
                        .build();

        when(videoReconnectService.confirmVideoReconnection(
                "old-session", "test-session-id", "conf-1", "test-user"))
                .thenReturn(true);

        byte[][] mockFrames = new byte[][]{new byte[]{0x01}};
        when(videoStreamBuffer.getLastFrames("conf-1", "test-user", 30))
                .thenReturn(mockFrames);

        // When
        webSocketController.handleVideoReconnection(request, principal, headerAccessor);

        // Then
        verify(videoReconnectService).confirmVideoReconnection(
                "old-session", "test-session-id", "conf-1", "test-user");
        
        // Should notify about reconnection
        verify(messagingTemplate).convertAndSend(
                eq("/topic/conference/conf-1"),
                any(ConferenceEventDTO.class)
        );
    }

    @Test
    @DisplayName("Should handle failed video reconnection")
    void shouldHandleFailedVideoReconnection() {
        // Given
        WebSocketController.VideoReconnectionRequest request = 
                WebSocketController.VideoReconnectionRequest.builder()
                        .oldSessionId("old-session")
                        .conferenceId("conf-1")
                        .deviceId("device-1")
                        .build();

        when(videoReconnectService.confirmVideoReconnection(
                "old-session", "test-session-id", "conf-1", "test-user"))
                .thenReturn(false);

        // When
        webSocketController.handleVideoReconnection(request, principal, headerAccessor);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                eq("test-user"),
                eq("/queue/video-reconnect-failed"),
                any(ErrorDTO.class)
        );
    }

    @Test
    @DisplayName("Should handle connection interruption")
    void shouldHandleConnectionInterruption() {
        // Given
        ReconnectionDTOs.ConnectionInterrupted event = 
                ReconnectionDTOs.ConnectionInterrupted.builder()
                        .sessionId("session-1")
                        .reason("network_loss")
                        .timestamp(System.currentTimeMillis())
                        .willReconnect(true)
                        .build();

        // When
        webSocketController.handleConnectionInterrupted(event, principal);

        // Then - should log the interruption (no exception thrown)
        assertDoesNotThrow(() -> 
                webSocketController.handleConnectionInterrupted(event, principal));
    }

    @Test
    @DisplayName("Should return buffer status")
    void shouldReturnBufferStatus() {
        // Given
        WebSocketController.VideoBufferStatusRequest request = 
                new WebSocketController.VideoBufferStatusRequest();
        request.setConferenceId("conf-1");
        request.setParticipantId("participant-1");

        VideoStreamBuffer.BufferStatus mockStatus = new VideoStreamBuffer.BufferStatus();
        mockStatus.setFrameCount(50);
        mockStatus.setTotalSizeBytes(5 * 1024 * 1024);
        
        when(videoStreamBuffer.getStatus("conf-1", "participant-1"))
                .thenReturn(mockStatus);

        // When
        webSocketController.getBufferStatus(request, principal);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                eq("test-user"),
                eq("/queue/buffer-status"),
                eq(mockStatus)
        );
    }

    @Test
    @DisplayName("Should handle error when video frame processing fails")
    void shouldHandleErrorWhenVideoFrameProcessingFails() {
        // Given
        WebSocketController.VideoFrameDTO frame = WebSocketController.VideoFrameDTO.builder()
                .conferenceId("conf-1")
                .frameData("invalid-base64!!!") // Invalid base64
                .timestamp(System.currentTimeMillis())
                .build();

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> webSocketController.handleVideoFrame(frame, principal));
    }
}
