package com.messenger.repository;

import com.messenger.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByChatId(UUID chatId);

    Page<Message> findByChatIdAndIsDeletedFalse(UUID chatId, Pageable pageable);

    List<Message> findByChatIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID chatId);

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId AND m.createdAt > :since ORDER BY m.createdAt ASC")
    List<Message> findByChatIdAndCreatedAtAfter(@Param("chatId") UUID chatId, 
                                                 @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat.id = :chatId AND m.createdAt > :timestamp")
    long countByChatIdAndCreatedAtAfter(@Param("chatId") UUID chatId, 
                                         @Param("timestamp") LocalDateTime timestamp);

    @Query("SELECT m FROM Message m WHERE m.sender.id = :userId AND m.createdAt BETWEEN :start AND :end")
    List<Message> findBySenderIdAndCreatedAtBetween(@Param("userId") UUID userId,
                                                     @Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);
}
