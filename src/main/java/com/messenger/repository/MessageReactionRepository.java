package com.messenger.repository;

import com.messenger.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {

    List<MessageReaction> findByMessageId(UUID messageId);

    Optional<MessageReaction> findByMessageIdAndUserIdAndEmojiCode(
            UUID messageId, UUID userId, String emojiCode);

    @Query("SELECT mr FROM MessageReaction mr WHERE mr.message.id = :messageId ORDER BY mr.createdAt")
    List<MessageReaction> findByMessageIdOrderByCreatedAt(@Param("messageId") UUID messageId);

    @Query("SELECT mr.emojiCode, COUNT(mr) FROM MessageReaction mr WHERE mr.message.id = :messageId GROUP BY mr.emojiCode")
    List<Object[]> countByEmojiForMessage(@Param("messageId") UUID messageId);

    long countByMessageId(UUID messageId);

    boolean existsByMessageIdAndUserIdAndEmojiCode(
            UUID messageId, UUID userId, String emojiCode);

    void deleteByMessageIdAndUserIdAndEmojiCode(
            UUID messageId, UUID userId, String emojiCode);
}
