package com.messenger.repository;

import com.messenger.entity.VideoConference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoConferenceRepository extends JpaRepository<VideoConference, UUID> {

    Optional<VideoConference> findByRoomId(String roomId);

    List<VideoConference> findByChatId(UUID chatId);

    @Query("SELECT vc FROM VideoConference vc WHERE vc.chat.id = :chatId AND vc.status = 'ACTIVE'")
    Optional<VideoConference> findActiveByChatId(@Param("chatId") UUID chatId);

    List<VideoConference> findByInitiatorId(UUID initiatorId);

    @Query("SELECT vc FROM VideoConference vc WHERE vc.status = 'ACTIVE' AND vc.createdAt < :threshold")
    List<VideoConference> findStaleActiveConferences(@Param("threshold") java.time.LocalDateTime threshold);

    @Query("SELECT vc FROM VideoConference vc WHERE vc.initiator.id = :userId AND vc.status = 'ENDED' ORDER BY vc.endedAt DESC")
    List<VideoConference> findEndedConferencesByInitiator(@Param("userId") UUID userId);

    @Query("SELECT vc FROM VideoConference vc WHERE vc.status = 'ENDED' AND vc.initiator.id != :userId AND EXISTS (SELECT 1 FROM vc.chat.userChats uc WHERE uc.user.id = :userId)")
    List<VideoConference> findMissedCallsForUser(@Param("userId") UUID userId);
}
