package com.messenger.controller;

import com.messenger.entity.VideoConference;
import com.messenger.service.VideoConferenceService;
import com.messenger.service.VideoStreamBuffer;
import com.messenger.service.VideoReconnectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("VideoConferenceController Tests")
@ExtendWith(MockitoExtension.class)
class VideoConferenceControllerTest {

    @Mock
    private VideoConferenceService conferenceService;

    @Mock
    private VideoStreamBuffer videoStreamBuffer;

    @Mock
    private VideoReconnectService videoReconnectService;

    private UserDetails userDetails;

    @InjectMocks
    private VideoConferenceController videoConferenceController;

    @BeforeEach
    void setUp() {
        userDetails = new User("test-user", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("Should create audio conference")
    void shouldCreateAudioConference() {
        // Given
        UUID chatId = UUID.randomUUID();
        VideoConference conference = createConference(VideoConference.ConferenceType.AUDIO);

        when(conferenceService.createConference(eq(chatId), eq("test-user"), eq(VideoConference.ConferenceType.AUDIO)))
                .thenReturn(conference);

        // When
        ResponseEntity<VideoConferenceController.ConferenceDTO> response = 
                videoConferenceController.createConference(chatId, "audio", userDetails);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("AUDIO", response.getBody().getConferenceType());
    }

    @Test
    @DisplayName("Should create video conference")
    void shouldCreateVideoConference() {
        // Given
        UUID chatId = UUID.randomUUID();
        VideoConference conference = createConference(VideoConference.ConferenceType.VIDEO);

        when(conferenceService.createConference(eq(chatId), eq("test-user"), eq(VideoConference.ConferenceType.VIDEO)))
                .thenReturn(conference);

        // When
        ResponseEntity<VideoConferenceController.ConferenceDTO> response = 
                videoConferenceController.createConference(chatId, "video", userDetails);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("VIDEO", response.getBody().getConferenceType());
    }

    @Test
    @DisplayName("Should create screen share conference")
    void shouldCreateScreenShareConference() {
        // Given
        UUID chatId = UUID.randomUUID();
        VideoConference conference = createConference(VideoConference.ConferenceType.SCREEN_SHARE);

        when(conferenceService.createConference(eq(chatId), eq("test-user"), eq(VideoConference.ConferenceType.SCREEN_SHARE)))
                .thenReturn(conference);

        // When
        ResponseEntity<VideoConferenceController.ConferenceDTO> response = 
                videoConferenceController.createConference(chatId, "screen_share", userDetails);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("SCREEN_SHARE", response.getBody().getConferenceType());
    }

    @Test
    @DisplayName("Should get conference participants")
    void shouldGetConferenceParticipants() {
        // Given
        UUID conferenceId = UUID.randomUUID();
        
        // When
        ResponseEntity<List<VideoConferenceController.ParticipantDTO>> response = 
                videoConferenceController.getParticipants(conferenceId);

        // Then
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    @DisplayName("Should end conference and clear buffers")
    void shouldEndConferenceAndClearBuffers() {
        // Given
        UUID conferenceId = UUID.randomUUID();

        // When
        ResponseEntity<Void> response = videoConferenceController.endConference(conferenceId, userDetails);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        verify(conferenceService).endConference(conferenceId, "test-user");
        verify(videoStreamBuffer).clearConferenceBuffers(conferenceId.toString());
        verify(videoReconnectService).removeConferenceSessions(conferenceId.toString());
    }

    @Test
    @DisplayName("Should handle different conference types case insensitive")
    void shouldHandleConferenceTypesCaseInsensitive() {
        // Given
        UUID chatId = UUID.randomUUID();
        VideoConference conference = createConference(VideoConference.ConferenceType.AUDIO);

        when(conferenceService.createConference(any(), any(), any())).thenReturn(conference);

        // When - test uppercase
        ResponseEntity<VideoConferenceController.ConferenceDTO> response1 = 
                videoConferenceController.createConference(chatId, "AUDIO", userDetails);

        // Then
        assertEquals(200, response1.getStatusCodeValue());

        // When - test mixed case
        ResponseEntity<VideoConferenceController.ConferenceDTO> response2 = 
                videoConferenceController.createConference(chatId, "Video", userDetails);

        // Then
        assertEquals(200, response2.getStatusCodeValue());
    }

    @Test
    @DisplayName("Should clear video buffers on conference end")
    void shouldClearVideoBuffersOnConferenceEnd() {
        // Given
        UUID conferenceId = UUID.randomUUID();
        String conferenceIdStr = conferenceId.toString();

        // When
        videoConferenceController.endConference(conferenceId, userDetails);

        // Then
        verify(videoStreamBuffer).clearConferenceBuffers(conferenceIdStr);
        verify(videoReconnectService).removeConferenceSessions(conferenceIdStr);
    }

    private VideoConference createConference(VideoConference.ConferenceType type) {
        // Используем конструктор без @Builder из-за проблем с lombok
        VideoConference conference = new VideoConference();
        try {
            java.lang.reflect.Field idField = VideoConference.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(conference, UUID.randomUUID());
            
            java.lang.reflect.Field roomIdField = VideoConference.class.getDeclaredField("roomId");
            roomIdField.setAccessible(true);
            roomIdField.set(conference, "room-" + System.currentTimeMillis());
            
            java.lang.reflect.Field typeField = VideoConference.class.getDeclaredField("conferenceType");
            typeField.setAccessible(true);
            typeField.set(conference, type);
            
            java.lang.reflect.Field statusField = VideoConference.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(conference, VideoConference.ConferenceStatus.ACTIVE);
            
            // Set Chat to avoid NPE in mapToDTO
            com.messenger.entity.Chat chat = new com.messenger.entity.Chat();
            java.lang.reflect.Field chatIdField = com.messenger.entity.Chat.class.getDeclaredField("id");
            chatIdField.setAccessible(true);
            chatIdField.set(chat, UUID.randomUUID());
            
            java.lang.reflect.Field conferenceChatField = VideoConference.class.getDeclaredField("chat");
            conferenceChatField.setAccessible(true);
            conferenceChatField.set(conference, chat);
            
            // Set Initiator to avoid NPE in mapToDTO
            com.messenger.entity.User initiator = new com.messenger.entity.User();
            java.lang.reflect.Field userIdField = com.messenger.entity.User.class.getDeclaredField("id");
            userIdField.setAccessible(true);
            userIdField.set(initiator, UUID.randomUUID());
            
            java.lang.reflect.Field usernameField = com.messenger.entity.User.class.getDeclaredField("username");
            usernameField.setAccessible(true);
            usernameField.set(initiator, "test-user");
            
            java.lang.reflect.Field initiatorField = VideoConference.class.getDeclaredField("initiator");
            initiatorField.setAccessible(true);
            initiatorField.set(conference, initiator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return conference;
    }
}
