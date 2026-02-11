package com.messenger.repository;

import com.messenger.entity.UserChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserChatRepository extends JpaRepository<UserChat, UUID> {

    List<UserChat> findByUserId(UUID userId);

    List<UserChat> findByChatId(UUID chatId);

    Optional<UserChat> findByUserIdAndChatId(UUID userId, UUID chatId);

    boolean existsByUserIdAndChatId(UUID userId, UUID chatId);

    long countByChatId(UUID chatId);
}
