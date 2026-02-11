package com.messenger.service;

import com.messenger.dto.ChatDTOs.*;
import com.messenger.entity.*;
import com.messenger.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final UserChatRepository userChatRepository;
    private final MessageRepository messageRepository;
    private final MessageStatusRepository messageStatusRepository;

    @Transactional
    public ChatDTO createChat(CreateChatRequest request, String creatorUsername) {
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Chat.ChatType chatType = Chat.ChatType.valueOf(request.getChatType());

        Chat chat = Chat.builder()
                .chatType(chatType)
                .chatName(request.getChatName())
                .createdBy(creator)
                .isEncrypted(true)
                .build();

        chatRepository.save(chat);

        // Add creator as admin
        UserChat creatorMembership = UserChat.builder()
                .user(creator)
                .chat(chat)
                .isAdmin(true)
                .joinedAt(LocalDateTime.now())
                .build();
        userChatRepository.save(creatorMembership);

        // Add other participants
        if (request.getParticipantUsernames() != null) {
            for (String username : request.getParticipantUsernames()) {
                if (!username.equals(creatorUsername)) {
                    addParticipant(chat.getId(), username, creatorUsername);
                }
            }
        }

        log.info("Chat created: {} by {}", chat.getId(), creatorUsername);
        return mapToDTO(chat, creator.getId());
    }

    @Transactional(readOnly = true)
    public List<ChatDTO> getUserChats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Chat> chats = chatRepository.findByUserId(user.getId());
        
        return chats.stream()
                .map(chat -> mapToDTO(chat, user.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatDTO getChatById(UUID chatId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!userChatRepository.existsByUserIdAndChatId(user.getId(), chatId)) {
            throw new RuntimeException("User is not a member of this chat");
        }

        return mapToDTO(chat, user.getId());
    }

    @Transactional
    public void addParticipant(UUID chatId, String participantUsername, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        User participant = userRepository.findByUsername(participantUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        // Check if admin
        UserChat adminMembership = userChatRepository.findByUserIdAndChatId(admin.getId(), chatId)
                .orElseThrow(() -> new RuntimeException("Admin is not a member of this chat"));

        if (chat.getChatType() != Chat.ChatType.PERSONAL && !adminMembership.getIsAdmin()) {
            throw new RuntimeException("Only admins can add participants");
        }

        // Check if already member
        if (userChatRepository.existsByUserIdAndChatId(participant.getId(), chatId)) {
            throw new RuntimeException("User is already a member");
        }

        UserChat membership = UserChat.builder()
                .user(participant)
                .chat(chat)
                .isAdmin(false)
                .joinedAt(LocalDateTime.now())
                .build();

        userChatRepository.save(membership);
        log.info("User {} added to chat {} by {}", participantUsername, chatId, adminUsername);
    }

    @Transactional
    public void removeParticipant(UUID chatId, String participantUsername, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        User participant = userRepository.findByUsername(participantUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserChat adminMembership = userChatRepository.findByUserIdAndChatId(admin.getId(), chatId)
                .orElseThrow(() -> new RuntimeException("Admin is not a member"));

        if (!adminMembership.getIsAdmin()) {
            throw new RuntimeException("Only admins can remove participants");
        }

        UserChat participantMembership = userChatRepository
                .findByUserIdAndChatId(participant.getId(), chatId)
                .orElseThrow(() -> new RuntimeException("User is not a member"));

        userChatRepository.delete(participantMembership);
        log.info("User {} removed from chat {} by {}", participantUsername, chatId, adminUsername);
    }

    @Transactional
    public ChatDTO getOrCreatePersonalChat(String username1, String username2) {
        User user1 = userRepository.findByUsername(username1)
                .orElseThrow(() -> new RuntimeException("User not found: " + username1));
        User user2 = userRepository.findByUsername(username2)
                .orElseThrow(() -> new RuntimeException("User not found: " + username2));

        // Check if chat already exists
        return chatRepository.findPersonalChatBetweenUsers(user1.getId(), user2.getId())
                .map(chat -> mapToDTO(chat, user1.getId()))
                .orElseGet(() -> {
                    // Create new personal chat
                    CreateChatRequest request = CreateChatRequest.builder()
                            .chatName(user2.getUsername()) // Chat name = other user's name
                            .chatType(Chat.ChatType.PERSONAL.name())
                            .participantUsernames(List.of(username2))
                            .build();
                    return createChat(request, username1);
                });
    }

    private ChatDTO mapToDTO(Chat chat, UUID currentUserId) {
        List<UserChat> memberships = userChatRepository.findByChatId(chat.getId());
        
        List<ChatParticipantDTO> participants = memberships.stream()
                .map(uc -> ChatParticipantDTO.builder()
                        .userId(uc.getUser().getId().toString())
                        .username(uc.getUser().getUsername())
                        .avatarUrl(uc.getUser().getAvatarUrl())
                        .isAdmin(uc.getIsAdmin())
                        .joinedAt(uc.getJoinedAt())
                        .isOnline(uc.getUser().getIsOnline())
                        .build())
                .collect(Collectors.toList());

        // Get last message
        List<Message> lastMessages = messageRepository
                .findByChatIdAndIsDeletedFalseOrderByCreatedAtDesc(chat.getId());
        MessageDTO lastMessage = null;
        if (!lastMessages.isEmpty()) {
            lastMessage = mapToMessageDTO(lastMessages.get(0));
        }

        // Count unread messages
        long unreadCount = messageStatusRepository
                .countByUserIdAndMessageChatIdAndStatusNot(
                        currentUserId, 
                        chat.getId(), 
                        MessageStatus.MessageDeliveryStatus.READ
                );

        return ChatDTO.builder()
                .id(chat.getId())
                .chatName(chat.getChatName())
                .chatType(chat.getChatType().name())
                .chatAvatar(chat.getChatAvatar())
                .createdBy(chat.getCreatedBy().getUsername())
                .createdAt(chat.getCreatedAt())
                .participants(participants)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .isEncrypted(chat.getIsEncrypted())
                .build();
    }

    private MessageDTO mapToMessageDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .chatId(message.getChat().getId())
                .senderId(message.getSender().getId().toString())
                .senderUsername(message.getSender().getUsername())
                .messageType(message.getMessageType().name())
                .encryptedContent(message.getEncryptedContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
