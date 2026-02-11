package com.messenger.controller;

import com.messenger.dto.ReactionDTOs.*;
import com.messenger.service.EmojiService;
import com.messenger.service.ReactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;
    private final EmojiService emojiService;

    @PostMapping
    public ResponseEntity<ReactionDTO> addReaction(
            @RequestBody AddReactionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} adding reaction {} to message {}",
                userDetails.getUsername(), request.getEmojiCode(), request.getMessageId());

        ReactionDTO reaction = reactionService.addReaction(request, userDetails.getUsername());
        return ResponseEntity.ok(reaction);
    }

    @DeleteMapping
    public ResponseEntity<Void> removeReaction(
            @RequestBody RemoveReactionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} removing reaction {} from message {}",
                userDetails.getUsername(), request.getEmojiCode(), request.getMessageId());

        reactionService.removeReaction(request, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/toggle")
    public ResponseEntity<ReactionDTO> toggleReaction(
            @RequestBody AddReactionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} toggling reaction {} on message {}",
                userDetails.getUsername(), request.getEmojiCode(), request.getMessageId());

        // Получаем текущее состояние
        MessageReactionsDTO summary = reactionService
                .getMessageReactionsSummary(request.getMessageId(), userDetails.getUsername());

        // Если пользователь уже поставил эту реакцию - удаляем, иначе добавляем
        if (summary.isUserHasReacted() && request.getEmojiCode().equals(summary.getUserReaction())) {
            reactionService.removeReaction(RemoveReactionRequest.builder()
                    .messageId(request.getMessageId())
                    .emojiCode(request.getEmojiCode())
                    .build(), userDetails.getUsername());
            return ResponseEntity.ok().build();
        } else {
            // Если есть другая реакция - удаляем её
            if (summary.isUserHasReacted()) {
                reactionService.removeReaction(RemoveReactionRequest.builder()
                        .messageId(request.getMessageId())
                        .emojiCode(summary.getUserReaction())
                        .build(), userDetails.getUsername());
            }
            ReactionDTO reaction = reactionService.addReaction(request, userDetails.getUsername());
            return ResponseEntity.ok(reaction);
        }
    }

    @GetMapping("/message/{messageId}")
    public ResponseEntity<List<ReactionDTO>> getReactionsForMessage(
            @PathVariable UUID messageId) {
        List<ReactionDTO> reactions = reactionService.getReactionsForMessage(messageId);
        return ResponseEntity.ok(reactions);
    }

    @GetMapping("/message/{messageId}/summary")
    public ResponseEntity<MessageReactionsDTO> getMessageReactionsSummary(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        MessageReactionsDTO summary = reactionService
                .getMessageReactionsSummary(messageId, userDetails.getUsername());
        return ResponseEntity.ok(summary);
    }
}
