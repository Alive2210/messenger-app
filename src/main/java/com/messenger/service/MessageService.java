package com.messenger.service;

import com.messenger.dto.*;
import com.messenger.entity.*;
import com.messenger.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final UserChatRepository userChatRepository;
    private final MessageStatusRepository messageStatusRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final FileStorageService fileStorageService;

    @Value("${rabbitmq.exchange:messenger.exchange}")
    private String exchange;

    @Transactional
    public MessageDTO sendMessage(SendMessageRequest request, String username) {
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        // Check if user is member of chat
        boolean isMember = userChatRepository.existsByUserIdAndChatId(sender.getId(), chat.getId());
        if (!isMember) {
            throw new RuntimeException("User is not a member of this chat");
        }

        // Create message
        Message.MessageType messageType = Message.MessageType.valueOf(request.getMessageType());
        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .messageType(messageType)
                .encryptedContent(request.getEncryptedContent())
                .encryptionIv(request.getEncryptionIv())
                .isDeleted(false)
                .isEdited(false)
                .createdAt(LocalDateTime.now())
                .build();

        if (request.getReplyToMessageId() != null) {
            Message replyTo = messageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new RuntimeException("Reply message not found"));
            message.setReplyToMessage(replyTo);
        }

        // Handle file attachment
        if (request.getFileAttachment() != null && messageType == Message.MessageType.FILE) {
            FileAttachmentDTO fileDto = request.getFileAttachment();
            FileAttachment attachment = FileAttachment.builder()
                    .message(message)
                    .fileName(fileDto.getFileName())
                    .fileType(fileDto.getFileType())
                    .fileSize(fileDto.getFileSize())
                    .fileUrl(fileDto.getFileUrl())
                    .isEncrypted(true)
                    .build();
            message.setFileAttachment(attachment);
        }

        // Handle voice message
        if (request.getVoiceMessage() != null && messageType == Message.MessageType.VOICE) {
            VoiceMessageDTO voiceDto = request.getVoiceMessage();
            VoiceMessage voice = VoiceMessage.builder()
                    .message(message)
                    .audioUrl(voiceDto.getAudioUrl())
                    .durationSeconds(voiceDto.getDurationSeconds())
                    .fileSize(voiceDto.getFileSize())
                    .waveformData(voiceDto.getWaveformData())
                    .isEncrypted(true)
                    .build();
            message.setVoiceMessage(voice);
        }

        messageRepository.save(message);

        // Create message statuses for all chat members
        List<UserChat> members = userChatRepository.findByChatId(chat.getId());
        for (UserChat member : members) {
            MessageStatus status = MessageStatus.builder()
                    .message(message)
                    .user(member.getUser())
                    .status(member.getUser().getId().equals(sender.getId()) 
                            ? MessageStatus.MessageDeliveryStatus.READ 
                            : MessageStatus.MessageDeliveryStatus.SENT)
                    .build();
            messageStatusRepository.save(status);
        }

        // Send to RabbitMQ for async processing (notifications, etc.)
        rabbitTemplate.convertAndSend(exchange, "message.sent", 
                new MessageEventDTO(message.getId(), chat.getId(), sender.getUsername()));

        log.info("Message sent: {} by {} to chat {}", message.getId(), username, chat.getId());
        
        return mapToDTO(message);
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getChatMessages(UUID chatId, String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check membership
        if (!userChatRepository.existsByUserIdAndChatId(user.getId(), chatId)) {
            throw new RuntimeException("User is not a member of this chat");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Message> messages = messageRepository.findByChatIdAndIsDeletedFalse(chatId, pageable);

        return messages.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markMessagesAsRead(UUID chatId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<MessageStatus> unreadStatuses = messageStatusRepository
                .findByUserIdAndMessageChatIdAndStatusNot(user.getId(), chatId, 
                        MessageStatus.MessageDeliveryStatus.READ);

        for (MessageStatus status : unreadStatuses) {
            status.setStatus(MessageStatus.MessageDeliveryStatus.READ);
            status.setUpdatedAt(LocalDateTime.now());
            messageStatusRepository.save(status);
        }

        // Update last read message in UserChat
        userChatRepository.findByUserIdAndChatId(user.getId(), chatId)
                .ifPresent(userChat -> {
                    if (!unreadStatuses.isEmpty()) {
                        userChat.setLastReadMessageId(
                                unreadStatuses.get(unreadStatuses.size() - 1).getMessage().getId()
                        );
                        userChatRepository.save(userChat);
                    }
                });

        log.info("Marked {} messages as read for user {} in chat {}", 
                unreadStatuses.size(), username, chatId);
    }

    @Transactional
    public void updateUserOnlineStatus(String username, boolean online) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setIsOnline(online);
            if (!online) {
                user.setLastSeen(LocalDateTime.now());
            }
            userRepository.save(user);
        });
    }

    private MessageDTO mapToDTO(Message message) {
        MessageDTO.MessageDTOBuilder builder = MessageDTO.builder()
                .id(message.getId())
                .chatId(message.getChat().getId())
                .senderId(message.getSender().getId().toString())
                .senderUsername(message.getSender().getUsername())
                .messageType(message.getMessageType().name())
                .encryptedContent(message.getEncryptedContent())
                .encryptionIv(message.getEncryptionIv())
                .createdAt(message.getCreatedAt())
                .isEdited(message.getIsEdited())
                .replyToMessageId(message.getReplyToMessage() != null 
                        ? message.getReplyToMessage().getId() 
                        : null);

        if (message.getFileAttachment() != null) {
            FileAttachment attachment = message.getFileAttachment();
            builder.fileAttachment(FileAttachmentDTO.builder()
                    .fileName(attachment.getFileName())
                    .fileType(attachment.getFileType())
                    .fileSize(attachment.getFileSize())
                    .fileUrl(fileStorageService.getFileUrl(attachment.getFileUrl()))
                    .build());
        }

        if (message.getVoiceMessage() != null) {
            VoiceMessage voice = message.getVoiceMessage();
            builder.voiceMessage(VoiceMessageDTO.builder()
                    .audioUrl(fileStorageService.getFileUrl(voice.getAudioUrl()))
                    .durationSeconds(voice.getDurationSeconds())
                    .fileSize(voice.getFileSize())
                    .waveformData(voice.getWaveformData())
                    .build());
        }

        return builder.build();
    }

    @Data
    @AllArgsConstructor
    private static class MessageEventDTO {
        private UUID messageId;
        private UUID chatId;
        private String senderUsername;
    }
}
