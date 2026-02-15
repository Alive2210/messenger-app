package com.messenger.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VideoStreamBuffer Tests")
class VideoStreamBufferTest {

    private VideoStreamBuffer videoStreamBuffer;

    @BeforeEach
    void setUp() {
        videoStreamBuffer = new VideoStreamBuffer();
    }

    @Test
    @DisplayName("Should add video frame to buffer")
    void shouldAddFrameToBuffer() {
        // Given
        String conferenceId = "conf-1";
        String participantId = "user-1";
        byte[] frameData = new byte[]{0x01, 0x02, 0x03, 0x04};
        long timestamp = System.currentTimeMillis();

        // When
        videoStreamBuffer.addFrame(conferenceId, participantId, frameData, timestamp);

        // Then
        VideoStreamBuffer.BufferStatus status = videoStreamBuffer.getStatus(conferenceId, participantId);
        assertNotNull(status);
        assertEquals(1, status.getFrameCount());
        assertEquals(4, status.getTotalSizeBytes());
    }

    @Test
    @DisplayName("Should buffer up to 60 frames")
    void shouldBufferUpTo60Frames() {
        // Given
        String conferenceId = "conf-1";
        String participantId = "user-1";
        byte[] frameData = new byte[1000]; // 1KB frame

        // When - add 70 frames
        for (int i = 0; i < 70; i++) {
            videoStreamBuffer.addFrame(conferenceId, participantId, frameData, System.currentTimeMillis());
        }

        // Then - should keep only 60 frames
        VideoStreamBuffer.BufferStatus status = videoStreamBuffer.getStatus(conferenceId, participantId);
        assertNotNull(status);
        assertEquals(60, status.getFrameCount());
    }

    @Test
    @DisplayName("Should respect max buffer size of 10MB")
    void shouldRespectMaxBufferSize() {
        // Given
        String conferenceId = "conf-1";
        String participantId = "user-1";
        byte[] largeFrame = new byte[2 * 1024 * 1024]; // 2MB frame

        // When - try to add 6 frames (12MB total)
        for (int i = 0; i < 6; i++) {
            videoStreamBuffer.addFrame(conferenceId, participantId, largeFrame, System.currentTimeMillis());
        }

        // Then - buffer should not exceed 10MB
        VideoStreamBuffer.BufferStatus status = videoStreamBuffer.getStatus(conferenceId, participantId);
        assertNotNull(status);
        assertTrue(status.getTotalSizeBytes() <= 10 * 1024 * 1024, 
                "Buffer should not exceed 10MB, but was " + status.getTotalSizeBytes());
    }

    @Test
    @DisplayName("Should get frames from specific sequence")
    void shouldGetFramesFromSpecificSequence() {
        // Given
        String conferenceId = "conf-1";
        String participantId = "user-1";
        
        // Add 10 frames (sequence numbers will be 1-10, data values 0-9)
        for (int i = 0; i < 10; i++) {
            byte[] frameData = new byte[]{(byte) i};
            videoStreamBuffer.addFrame(conferenceId, participantId, frameData, System.currentTimeMillis());
        }

        // When - get frames from sequence 5
        byte[][] frames = videoStreamBuffer.getFrames(conferenceId, participantId, 5);

        // Then - should get frames with sequence 5-10 (6 frames)
        // Data values: sequence 5 -> data[0]=4, sequence 6 -> data[0]=5, etc.
        assertEquals(6, frames.length);
        assertEquals(4, frames[0][0]); // First frame (sequence 5) should have data value 4
    }

    @Test
    @DisplayName("Should get last N frames")
    void shouldGetLastNFrames() {
        // Given
        String conferenceId = "conf-1";
        String participantId = "user-1";
        
        // Add 20 frames
        for (int i = 0; i < 20; i++) {
            byte[] frameData = new byte[]{(byte) i};
            videoStreamBuffer.addFrame(conferenceId, participantId, frameData, System.currentTimeMillis());
        }

        // When - get last 5 frames
        byte[][] frames = videoStreamBuffer.getLastFrames(conferenceId, participantId, 5);

        // Then - should get last 5 frames
        assertEquals(5, frames.length);
        assertEquals(15, frames[0][0]); // First frame should be 15
        assertEquals(19, frames[4][0]); // Last frame should be 19
    }

    @Test
    @DisplayName("Should replay buffer from beginning")
    void shouldReplayBufferFromBeginning() {
        // Given
        String conferenceId = "conf-1";
        String participantId = "user-1";
        
        // Add 5 frames
        for (int i = 0; i < 5; i++) {
            byte[] frameData = new byte[]{(byte) i};
            videoStreamBuffer.addFrame(conferenceId, participantId, frameData, System.currentTimeMillis());
        }

        // When - replay buffer
        byte[][] frames = videoStreamBuffer.replayBuffer(conferenceId, participantId);

        // Then - should get all 5 frames from beginning
        assertEquals(5, frames.length);
        for (int i = 0; i < 5; i++) {
            assertEquals(i, frames[i][0]);
        }
    }

    @Test
    @DisplayName("Should return empty array for non-existent buffer")
    void shouldReturnEmptyArrayForNonExistentBuffer() {
        // When
        byte[][] frames = videoStreamBuffer.replayBuffer("non-existent", "user");

        // Then
        assertNotNull(frames);
        assertEquals(0, frames.length);
    }

    @Test
    @DisplayName("Should clear specific buffer")
    void shouldClearSpecificBuffer() {
        // Given
        String conferenceId = "conf-1";
        String participantId = "user-1";
        videoStreamBuffer.addFrame(conferenceId, participantId, new byte[]{0x01}, System.currentTimeMillis());

        // When
        videoStreamBuffer.clearBuffer(conferenceId, participantId);

        // Then
        VideoStreamBuffer.BufferStatus status = videoStreamBuffer.getStatus(conferenceId, participantId);
        assertNull(status);
    }

    @Test
    @DisplayName("Should clear all buffers for conference")
    void shouldClearAllBuffersForConference() {
        // Given
        String conferenceId = "conf-1";
        videoStreamBuffer.addFrame(conferenceId, "user-1", new byte[]{0x01}, System.currentTimeMillis());
        videoStreamBuffer.addFrame(conferenceId, "user-2", new byte[]{0x02}, System.currentTimeMillis());
        videoStreamBuffer.addFrame("conf-2", "user-1", new byte[]{0x03}, System.currentTimeMillis());

        // When
        videoStreamBuffer.clearConferenceBuffers(conferenceId);

        // Then
        assertNull(videoStreamBuffer.getStatus(conferenceId, "user-1"));
        assertNull(videoStreamBuffer.getStatus(conferenceId, "user-2"));
        assertNotNull(videoStreamBuffer.getStatus("conf-2", "user-1")); // Other conference should remain
    }

    @Test
    @DisplayName("Should handle multiple participants independently")
    void shouldHandleMultipleParticipantsIndependently() {
        // Given
        String conferenceId = "conf-1";
        
        // When
        videoStreamBuffer.addFrame(conferenceId, "user-1", new byte[]{0x01}, System.currentTimeMillis());
        videoStreamBuffer.addFrame(conferenceId, "user-1", new byte[]{0x02}, System.currentTimeMillis());
        videoStreamBuffer.addFrame(conferenceId, "user-2", new byte[]{0x03}, System.currentTimeMillis());

        // Then
        VideoStreamBuffer.BufferStatus status1 = videoStreamBuffer.getStatus(conferenceId, "user-1");
        VideoStreamBuffer.BufferStatus status2 = videoStreamBuffer.getStatus(conferenceId, "user-2");
        
        assertEquals(2, status1.getFrameCount());
        assertEquals(1, status2.getFrameCount());
    }

    @Test
    @DisplayName("Should handle concurrent frame additions")
    void shouldHandleConcurrentFrameAdditions() throws InterruptedException {
        // Given
        String conferenceId = "conf-1";
        String participantId = "user-1";
        int threadCount = 10;
        int framesPerThread = 10;

        // When - add frames from multiple threads concurrently
        Thread[] threads = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            final int threadNum = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < framesPerThread; i++) {
                    byte[] frameData = new byte[]{(byte) threadNum, (byte) i};
                    videoStreamBuffer.addFrame(conferenceId, participantId, frameData, System.currentTimeMillis());
                }
            });
            threads[t].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - should have exactly 60 frames (max buffer size)
        VideoStreamBuffer.BufferStatus status = videoStreamBuffer.getStatus(conferenceId, participantId);
        assertNotNull(status);
        assertEquals(60, status.getFrameCount());
    }

    @Test
    @DisplayName("Should maintain sequence numbers correctly")
    void shouldMaintainSequenceNumbersCorrectly() {
        // Given
        String conferenceId = "conf-1";
        String participantId = "user-1";
        
        // Add 5 frames
        for (int i = 0; i < 5; i++) {
            videoStreamBuffer.addFrame(conferenceId, participantId, new byte[]{0x01}, System.currentTimeMillis());
        }

        // Then
        VideoStreamBuffer.BufferStatus status = videoStreamBuffer.getStatus(conferenceId, participantId);
        assertEquals(5, status.getLastSequenceNumber());
        
        // Add more frames
        for (int i = 0; i < 3; i++) {
            videoStreamBuffer.addFrame(conferenceId, participantId, new byte[]{0x01}, System.currentTimeMillis());
        }
        
        status = videoStreamBuffer.getStatus(conferenceId, participantId);
        assertEquals(8, status.getLastSequenceNumber());
    }
}
