package com.messenger.repository;

import com.messenger.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for MessageReaction entities.
 * Provides CRUD operations and custom queries for reaction management.
 */
@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    /**
     * Finds all reactions for a specific message.
     *
     * @param messageId the message identifier
     * @return list of reactions for the message
     */
    List<MessageReaction> findByMessageId(String messageId);

    /**
     * Finds all reactions created by a specific user.
     *
     * @param userId the user identifier
     * @return list of reactions created by the user
     */
    List<MessageReaction> findByUserId(String userId);

    /**
     * Finds a specific reaction by message ID and user ID.
     * A user can have only one reaction per message.
     *
     * @param messageId the message identifier
     * @param userId    the user identifier
     * @return optional containing the reaction if found
     */
    Optional<MessageReaction> findByMessageIdAndUserId(String messageId, String userId);

    /**
     * Finds all reactions for a specific message and emoji.
     *
     * @param messageId the message identifier
     * @param emojiCode the emoji code
     * @return list of reactions with the specified emoji
     */
    List<MessageReaction> findByMessageIdAndEmojiCode(String messageId, String emojiCode);

    /**
     * Finds a specific reaction by message ID, user ID, and emoji.
     *
     * @param messageId the message identifier
     * @param userId    the user identifier
     * @param emojiCode the emoji code
     * @return optional containing the reaction if found
     */
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmojiCode(
            String messageId, String userId, String emojiCode);

    /**
     * Counts reactions for a specific message and emoji.
     *
     * @param messageId the message identifier
     * @param emojiCode the emoji code
     * @return count of reactions
     */
    long countByMessageIdAndEmojiCode(String messageId, String emojiCode);

    /**
     * Counts all reactions for a specific message.
     *
     * @param messageId the message identifier
     * @return total count of reactions
     */
    long countByMessageId(String messageId);

    /**
     * Counts reactions by a specific user.
     *
     * @param userId the user identifier
     * @return count of reactions
     */
    long countByUserId(String userId);

    /**
     * Counts reactions for a specific message by emoji codes.
     *
     * @param messageId  the message identifier
     * @param emojiCodes list of emoji codes to count
     * @return list of reaction counts in the same order as emojiCodes
     */
    @Query("SELECT COUNT(mr) FROM MessageReaction mr WHERE mr.messageId = :messageId AND mr.emojiCode IN :emojiCodes")
    List<Long> countByMessageIdAndEmojiCodes(@Param("messageId") String messageId, @Param("emojiCodes") List<String> emojiCodes);

    /**
     * Finds all reactions for a specific message with specific emoji codes.
     *
     * @param messageId  the message identifier
     * @param emojiCodes list of emoji codes to filter by
     * @return list of reactions
     */
    List<MessageReaction> findByMessageIdAndEmojiCodeIn(String messageId, List<String> emojiCodes);

    /**
     * Deletes a specific reaction by message ID and user ID.
     *
     * @param messageId the message identifier
     * @param userId    the user identifier
     */
    void deleteByMessageIdAndUserId(String messageId, String userId);

    /**
     * Deletes all reactions for a specific message.
     *
     * @param messageId the message identifier
     */
    void deleteByMessageId(String messageId);

    /**
     * Deletes all reactions by a specific user.
     *
     * @param userId the user identifier
     */
    void deleteByUserId(String userId);

    /**
     * Deletes a specific reaction by message ID, user ID, and emoji.
     *
     * @param messageId the message identifier
     * @param userId    the user identifier
     * @param emojiCode the emoji code
     */
    void deleteByMessageIdAndUserIdAndEmojiCode(String messageId, String userId, String emojiCode);

    /**
     * Deletes reactions older than the specified timestamp.
     * Used for cleanup of old reaction data.
     *
     * @param timestamp the cutoff timestamp
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM MessageReaction mr WHERE mr.timestamp < :timestamp")
    int deleteByTimestampBefore(@Param("timestamp") Instant timestamp);

    /**
     * Gets distinct emoji codes used for a specific message.
     *
     * @param messageId the message identifier
     * @return list of unique emoji codes
     */
    @Query("SELECT DISTINCT mr.emojiCode FROM MessageReaction mr WHERE mr.messageId = :messageId")
    List<String> findDistinctEmojiCodesByMessageId(@Param("messageId") String messageId);

    /**
     * Gets reaction counts grouped by emoji for a specific message.
     *
     * @param messageId the message identifier
     * @return list of object arrays with [emojiCode, count]
     */
    @Query("SELECT mr.emojiCode, COUNT(mr) FROM MessageReaction mr WHERE mr.messageId = :messageId GROUP BY mr.emojiCode")
    List<Object[]> countByMessageIdGroupedByEmoji(@Param("messageId") String messageId);

    /**
     * Checks if a user has reacted to a specific message.
     *
     * @param messageId the message identifier
     * @param userId    the user identifier
     * @return true if the user has reacted
     */
    boolean existsByMessageIdAndUserId(String messageId, String userId);

    /**
     * Checks if a user has reacted to a specific message with a specific emoji.
     *
     * @param messageId the message identifier
     * @param userId    the user identifier
     * @param emojiCode the emoji code
     * @return true if the user has reacted with the emoji
     */
    boolean existsByMessageIdAndUserIdAndEmojiCode(String messageId, String userId, String emojiCode);

    /**
     * Saves all reactions in a batch operation.
     * Optimized for bulk inserts/updates.
     *
     * @param reactions list of reactions to save
     * @return list of saved reactions
     */
    @Override
    <S extends MessageReaction> List<S> saveAll(Iterable<S> reactions);
}
