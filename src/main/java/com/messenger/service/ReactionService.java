package com.messenger.service;

import com.messenger.dto.ReactionDTOs.*;
import com.messenger.entity.Emoji;
import com.messenger.entity.Message;
import com.messenger.entity.MessageReaction;
import com.messenger.entity.User;
import com.messenger.repository.EmojiRepository;
import com.messenger.repository.MessageReactionRepository;
import com.messenger.repository.MessageRepository;
import com.messenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactionService {

    private final MessageReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final EmojiRepository emojiRepository;

    @Transactional
    public ReactionDTO addReaction(AddReactionRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Message message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Проверяем, существует ли уже такая реакция
        Optional<MessageReaction> existingReaction = reactionRepository
                .findByMessageIdAndUserIdAndEmojiCode(
                        request.getMessageId(), user.getId(), request.getEmojiCode());

        if (existingReaction.isPresent()) {
            log.info("User {} already reacted with {} to message {}",
                    username, request.getEmojiCode(), request.getMessageId());
            return mapToDTO(existingReaction.get());
        }

        // Получаем информацию о смайлике
        Optional<Emoji> emoji = emojiRepository.findByEmojiCode(request.getEmojiCode());

        MessageReaction reaction = MessageReaction.builder()
                .message(message)
                .user(user)
                .emojiCode(request.getEmojiCode())
                .emojiShortcode(request.getEmojiShortcode())
                .isAnimated(emoji.map(Emoji::getIsAnimated).orElse(false))
                .animatedUrl(emoji.map(Emoji::getAnimatedUrl).orElse(null))
                .build();

        reactionRepository.save(reaction);

        log.info("User {} added reaction {} to message {}",
                username, request.getEmojiCode(), request.getMessageId());

        return mapToDTO(reaction);
    }

    @Transactional
    public void removeReaction(RemoveReactionRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        reactionRepository.deleteByMessageIdAndUserIdAndEmojiCode(
                request.getMessageId(), user.getId(), request.getEmojiCode());

        log.info("User {} removed reaction {} from message {}",
                username, request.getEmojiCode(), request.getMessageId());
    }

    @Transactional(readOnly = true)
    public List<ReactionDTO> getReactionsForMessage(UUID messageId) {
        return reactionRepository.findByMessageIdOrderByCreatedAt(messageId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MessageReactionsDTO getMessageReactionsSummary(UUID messageId, String username) {
        List<MessageReaction> reactions = reactionRepository.findByMessageId(messageId);

        User user = userRepository.findByUsername(username).orElse(null);

        // Подсчет реакций по типам
        Map<String, Long> counts = reactions.stream()
                .collect(Collectors.groupingBy(
                        MessageReaction::getEmojiCode,
                        Collectors.counting()
                ));

        Map<String, ReactionCountDTO> reactionCounts = new HashMap<>();
        counts.forEach((emojiCode, count) -> {
            reactionCounts.put(emojiCode, ReactionCountDTO.builder()
                    .emojiCode(emojiCode)
                    .count(count)
                    .build());
        });

        // Проверяем, ставил ли текущий пользователь реакцию
        boolean userHasReacted = false;
        String userReaction = null;

        if (user != null) {
            for (MessageReaction reaction : reactions) {
                if (reaction.getUser().getId().equals(user.getId())) {
                    userHasReacted = true;
                    userReaction = reaction.getEmojiCode();
                    break;
                }
            }
        }

        return MessageReactionsDTO.builder()
                .messageId(messageId)
                .reactionCounts(reactionCounts)
                .userHasReacted(userHasReacted)
                .userReaction(userReaction)
                .build();
    }

    @Transactional
    public void toggleReaction(AddReactionRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<MessageReaction> existing = reactionRepository
                .findByMessageIdAndUserIdAndEmojiCode(
                        request.getMessageId(), user.getId(), request.getEmojiCode());

        if (existing.isPresent()) {
            removeReaction(RemoveReactionRequest.builder()
                    .messageId(request.getMessageId())
                    .emojiCode(request.getEmojiCode())
                    .build(), username);
        } else {
            addReaction(request, username);
        }
    }

    private ReactionDTO mapToDTO(MessageReaction reaction) {
        return ReactionDTO.builder()
                .id(reaction.getId())
                .messageId(reaction.getMessage().getId())
                .userId(reaction.getUser().getId().toString())
                .username(reaction.getUser().getUsername())
                .emojiCode(reaction.getEmojiCode())
                .emojiShortcode(reaction.getEmojiShortcode())
                .isAnimated(reaction.getIsAnimated())
                .animatedUrl(reaction.getAnimatedUrl())
                .createdAt(reaction.getCreatedAt())
                .build();
    }
}
