package com.messenger.service;

import com.messenger.entity.*;
import com.messenger.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoConferenceService {

    private final VideoConferenceRepository conferenceRepository;
    private final ConferenceParticipantRepository participantRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    @Transactional
    public VideoConference createConference(UUID chatId, String initiatorUsername, 
                                          VideoConference.ConferenceType type) {
        User initiator = userRepository.findByUsername(initiatorUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        // Check if user is member of chat
        if (!chat.getUserChats().stream()
                .anyMatch(uc -> uc.getUser().getId().equals(initiator.getId()))) {
            throw new RuntimeException("User is not a member of this chat");
        }

        // Generate unique room ID
        String roomId = UUID.randomUUID().toString();

        VideoConference conference = VideoConference.builder()
                .roomId(roomId)
                .chat(chat)
                .initiator(initiator)
                .conferenceType(type)
                .status(VideoConference.ConferenceStatus.SCHEDULED)
                .isRecorded(false)
                .maxParticipants(50)
                .build();

        conferenceRepository.save(conference);

        // Add initiator as participant
        ConferenceParticipant participant = ConferenceParticipant.builder()
                .conference(conference)
                .user(initiator)
                .joinedAt(LocalDateTime.now())
                .isVideoEnabled(type != VideoConference.ConferenceType.AUDIO)
                .isAudioEnabled(true)
                .isHost(true)
                .build();

        participantRepository.save(participant);

        conference.setStartedAt(LocalDateTime.now());
        conference.setStatus(VideoConference.ConferenceStatus.ACTIVE);
        conferenceRepository.save(conference);

        log.info("Conference created: {} by {}", roomId, initiatorUsername);
        return conference;
    }

    @Transactional
    public ConferenceParticipant joinConference(UUID conferenceId, String username, 
                                               boolean videoEnabled, boolean audioEnabled) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        VideoConference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new RuntimeException("Conference not found"));

        if (conference.getStatus() != VideoConference.ConferenceStatus.ACTIVE) {
            throw new RuntimeException("Conference is not active");
        }

        // Check if already joined
        var existing = participantRepository.findByConferenceIdAndUserId(conferenceId, user.getId());
        if (existing.isPresent()) {
            var participant = existing.get();
            participant.setIsVideoEnabled(videoEnabled);
            participant.setIsAudioEnabled(audioEnabled);
            return participantRepository.save(participant);
        }

        // Check max participants
        long currentParticipants = participantRepository.countByConferenceIdAndLeftAtIsNull(conferenceId);
        if (currentParticipants >= conference.getMaxParticipants()) {
            throw new RuntimeException("Conference is full");
        }

        ConferenceParticipant participant = ConferenceParticipant.builder()
                .conference(conference)
                .user(user)
                .joinedAt(LocalDateTime.now())
                .isVideoEnabled(videoEnabled)
                .isAudioEnabled(audioEnabled)
                .isScreenSharing(false)
                .isHost(false)
                .build();

        log.info("User {} joined conference {}", username, conferenceId);
        return participantRepository.save(participant);
    }

    @Transactional
    public void leaveConference(UUID conferenceId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ConferenceParticipant participant = participantRepository
                .findByConferenceIdAndUserId(conferenceId, user.getId())
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        participant.setLeftAt(LocalDateTime.now());
        participantRepository.save(participant);

        // Check if conference should end (no participants left)
        long activeParticipants = participantRepository
                .countByConferenceIdAndLeftAtIsNull(conferenceId);
        
        if (activeParticipants == 0) {
            VideoConference conference = participant.getConference();
            conference.setStatus(VideoConference.ConferenceStatus.ENDED);
            conference.setEndedAt(LocalDateTime.now());
            conferenceRepository.save(conference);
            log.info("Conference {} ended (no participants)", conferenceId);
        }

        log.info("User {} left conference {}", username, conferenceId);
    }

    @Transactional
    public void updateMediaState(UUID conferenceId, String username, 
                                boolean videoEnabled, boolean audioEnabled, 
                                boolean screenSharing) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ConferenceParticipant participant = participantRepository
                .findByConferenceIdAndUserId(conferenceId, user.getId())
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        participant.setIsVideoEnabled(videoEnabled);
        participant.setIsAudioEnabled(audioEnabled);
        participant.setIsScreenSharing(screenSharing);
        participantRepository.save(participant);
    }

    @Transactional(readOnly = true)
    public List<ConferenceParticipant> getConferenceParticipants(UUID conferenceId) {
        return participantRepository.findByConferenceIdAndLeftAtIsNull(conferenceId);
    }

    @Transactional
    public void endConference(UUID conferenceId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        VideoConference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new RuntimeException("Conference not found"));

        // Only host or admin can end conference
        boolean isHost = participantRepository
                .findByConferenceIdAndUserId(conferenceId, user.getId())
                .map(ConferenceParticipant::getIsHost)
                .orElse(false);

        if (!isHost && !conference.getInitiator().getId().equals(user.getId())) {
            throw new RuntimeException("Only host can end the conference");
        }

        conference.setStatus(VideoConference.ConferenceStatus.ENDED);
        conference.setEndedAt(LocalDateTime.now());
        conferenceRepository.save(conference);

        // Mark all participants as left
        List<ConferenceParticipant> activeParticipants = 
                participantRepository.findByConferenceIdAndLeftAtIsNull(conferenceId);
        for (ConferenceParticipant p : activeParticipants) {
            p.setLeftAt(LocalDateTime.now());
            participantRepository.save(p);
        }

        log.info("Conference {} ended by {}", conferenceId, username);
    }
}
