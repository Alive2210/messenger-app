package com.messenger.repository;

import com.messenger.entity.ConferenceParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConferenceParticipantRepository extends JpaRepository<ConferenceParticipant, UUID> {

    List<ConferenceParticipant> findByConferenceId(UUID conferenceId);

    List<ConferenceParticipant> findByUserId(UUID userId);

    Optional<ConferenceParticipant> findByConferenceIdAndUserId(UUID conferenceId, UUID userId);

    List<ConferenceParticipant> findByConferenceIdAndLeftAtIsNull(UUID conferenceId);

    long countByConferenceIdAndLeftAtIsNull(UUID conferenceId);

    long countByConferenceId(UUID conferenceId);
}
