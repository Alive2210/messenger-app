package com.messenger.repository;

import com.messenger.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query("SELECT c FROM Chat c JOIN c.userChats uc WHERE uc.user.id = :userId")
    List<Chat> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Chat c JOIN c.userChats uc1 JOIN c.userChats uc2 " +
           "WHERE c.chatType = 'PERSONAL' AND uc1.user.id = :user1Id AND uc2.user.id = :user2Id")
    Optional<Chat> findPersonalChatBetweenUsers(@Param("user1Id") UUID user1Id, 
                                                 @Param("user2Id") UUID user2Id);

    @Query("SELECT c FROM Chat c JOIN c.userChats uc WHERE uc.user.id = :userId AND c.chatType = :chatType")
    List<Chat> findByUserIdAndChatType(@Param("userId") UUID userId, 
                                        @Param("chatType") Chat.ChatType chatType);
}
