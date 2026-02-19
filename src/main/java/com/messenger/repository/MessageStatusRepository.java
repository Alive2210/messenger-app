package com.messenger.repository;

import com.messenger.entity.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageStatusRepository extends JpaRepository<MessageStatus, UUID> {

    List<MessageStatus> findByMessageId(UUID messageId);

    List<MessageStatus> findByUserId(UUID userId);

    Optional<MessageStatus> findByMessageIdAndUserId(UUID messageId, UUID userId);

    @Query("SELECT ms FROM MessageStatus ms WHERE ms.user.id = :userId AND ms.message.chat.id = :chatId AND ms.status != :status")
    List<MessageStatus> findByUserIdAndMessageChatIdAndStatusNot(@Param("userId") UUID userId,
                                                                  @Param("chatId") UUID chatId,
                                                                  @Param("status") MessageStatus.MessageDeliveryStatus status);

    long countByMessageChatIdAndStatus(UUID chatId, MessageStatus.MessageDeliveryStatus status);

    @Query("SELECT COUNT(ms) FROM MessageStatus ms WHERE ms.user.id = :userId AND ms.message.chat.id = :chatId AND ms.status != 'READ'")
    long countUnreadByUserIdAndChatId(@Param("userId") UUID userId, @Param("chatId") UUID chatId);
}
