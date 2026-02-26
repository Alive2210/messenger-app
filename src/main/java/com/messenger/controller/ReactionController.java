package com.messenger.controller;

import com.messenger.dto.MessageReactionDTO;
import com.messenger.service.MessageReactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * REST and WebSocket controller for message reaction operations.
 * Provides endpoints for adding, removing, and querying message reactions.
 */
@RestController
@RequestMapping("/api/reactions")
@Slf4j
@RequiredArgsConstructor
public class ReactionController {

    /**
     * Service for managing reactions.
     */
    private final MessageReactionService messageReactionService;

    /**
     * Adds a reaction to a message.
     *
     * @param messageId the message identifier
     * @param request   the reaction request containing emoji code
     * @param userId    the user identifier (from authentication)
     * @return the created reaction DTO
     */
    @PostMapping("/{messageId}")
    public ResponseEntity<MessageReactionDTO> addReaction(
            @PathVariable String messageId,
            @RequestBody ReactionRequest request,
            @RequestParam(required = false) String userId) {

        log.info("Adding reaction to message {} by user {}", messageId, userId);

        if (userId == null || userId.isEmpty()) {
            userId = "anonymous";
        }

        if (request.getEmojiCode() == null || request.getEmojiCode().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        MessageReactionDTO reaction = messageReactionService.addReaction(
                messageId, userId, request.getEmojiCode());

        return ResponseEntity.ok(reaction);
    }

    /**
     * Removes a reaction from a message.
     *
     * @param messageId the message identifier
     * @param emoji     the emoji code to remove (URL encoded)
     * @param userId    the user identifier (from authentication)
     * @return 204 No Content if successful, 404 if not found
     */
    @DeleteMapping("/{messageId}/{emoji}")
    public ResponseEntity<Void> removeReaction(
            @PathVariable String messageId,
            @PathVariable String emoji,
            @RequestParam(required = false) String userId) {

        // Decode the emoji (it may be URL encoded)
        String decodedEmoji = URLDecoder.decode(emoji, StandardCharsets.UTF_8);

        log.info("Removing reaction {} from message {} by user {}", decodedEmoji, messageId, userId);

        if (userId == null || userId.isEmpty()) {
            userId = "anonymous";
        }

        boolean removed = messageReactionService.removeReaction(messageId, userId, decodedEmoji);

        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Toggles a reaction on a message.
     * If the user has already reacted with this emoji, it removes the reaction.
     * Otherwise, it adds the reaction.
     *
     * @param messageId the message identifier
     * @param request   the reaction request containing emoji code
     * @param userId    the user identifier (from authentication)
     * @return toggle response
     */
    @PostMapping("/{messageId}/toggle")
    public ResponseEntity<MessageReactionDTO.ToggleReactionResponse> toggleReaction(
            @PathVariable String messageId,
            @RequestBody ReactionRequest request,
            @RequestParam(required = false) String userId) {

        log.info("Toggling reaction on message {} by user {}", messageId, userId);

        if (userId == null || userId.isEmpty()) {
            userId = "anonymous";
        }

        if (request.getEmojiCode() == null || request.getEmojiCode().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        MessageReactionDTO.ToggleReactionResponse response = messageReactionService.toggleReaction(
                messageId, userId, request.getEmojiCode());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets all reactions for a specific message.
     *
     * @param messageId the message identifier
     * @return list of reaction DTOs
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<List<MessageReactionDTO>> getMessageReactions(
            @PathVariable String messageId) {

        log.debug("Getting reactions for message {}", messageId);

        List<MessageReactionDTO> reactions = messageReactionService.getMessageReactions(messageId);
        return ResponseEntity.ok(reactions);
    }

    /**
     * Gets reaction summary for a specific message.
     * Groups reactions by emoji with counts.
     *
     * @param messageId the message identifier
     * @param userId    optional user ID for userHasReacted flag
     * @return list of reaction summaries
     */
    @GetMapping("/{messageId}/summary")
    public ResponseEntity<List<MessageReactionDTO.ReactionSummary>> getReactionSummary(
            @PathVariable String messageId,
            @RequestParam(required = false) String userId) {

        log.debug("Getting reaction summary for message {}", messageId);

        List<MessageReactionDTO.ReactionSummary> summaries;
        if (userId != null && !userId.isEmpty()) {
            summaries = messageReactionService.getReactionSummary(messageId, userId);
        } else {
            summaries = messageReactionService.getReactionSummary(messageId);
        }

        return ResponseEntity.ok(summaries);
    }

    /**
     * Gets complete reaction data for a message.
     *
     * @param messageId the message identifier
     * @param userId    optional user ID for userHasReacted flag
     * @return complete reaction response
     */
    @GetMapping("/{messageId}/complete")
    public ResponseEntity<MessageReactionDTO.MessageReactionsResponse> getCompleteReactions(
            @PathVariable String messageId,
            @RequestParam(required = false) String userId) {

        log.debug("Getting complete reactions for message {}", messageId);

        MessageReactionDTO.MessageReactionsResponse response;
        if (userId != null && !userId.isEmpty()) {
            response = messageReactionService.getMessageReactionsComplete(messageId, userId);
        } else {
            response = messageReactionService.getMessageReactionsComplete(messageId, null);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Gets the count of reactions for a specific message and emoji.
     *
     * @param messageId the message identifier
     * @param emoji     the emoji code
     * @return count response
     */
    @GetMapping("/{messageId}/count")
    public ResponseEntity<Map<String, Long>> getReactionCount(
            @PathVariable String messageId,
            @RequestParam String emoji) {

        log.debug("Getting reaction count for message {} and emoji {}", messageId, emoji);

        long count = messageReactionService.getReactionCount(messageId, emoji);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Gets the total count of reactions for a message.
     *
     * @param messageId the message identifier
     * @return total count
     */
    @GetMapping("/{messageId}/total")
    public ResponseEntity<Map<String, Long>> getTotalReactionCount(
            @PathVariable String messageId) {

        log.debug("Getting total reaction count for message {}", messageId);

        long count = messageReactionService.getTotalReactionCount(messageId);
        return ResponseEntity.ok(Map.of("totalCount", count));
    }

    /**
     * Checks if a user has reacted to a message.
     *
     * @param messageId the message identifier
     * @param userId    the user identifier
     * @return response with hasReacted flag
     */
    @GetMapping("/{messageId}/check")
    public ResponseEntity<Map<String, Object>> checkUserReaction(
            @PathVariable String messageId,
            @RequestParam String userId) {

        log.debug("Checking if user {} has reacted to message {}", userId, messageId);

        boolean hasReacted = messageReactionService.hasUserReacted(messageId, userId);
        String emojiCode = null;

        if (hasReacted) {
            // Get the emoji the user reacted with
            List<MessageReactionDTO> reactions = messageReactionService.getMessageReactions(messageId);
            for (MessageReactionDTO reaction : reactions) {
                if (reaction.getUserId().equals(userId)) {
                    emojiCode = reaction.getEmojiCode();
                    break;
                }
            }
        }

        Map<String, Object> response = Map.of(
                "hasReacted", hasReacted,
                "emojiCode", emojiCode != null ? emojiCode : ""
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets reactions by a specific user.
     *
     * @param userId the user identifier
     * @return list of reaction DTOs
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MessageReactionDTO>> getUserReactions(
            @PathVariable String userId) {

        log.debug("Getting reactions for user {}", userId);

        List<MessageReactionDTO> reactions = messageReactionService.getUserReactions(userId);
        return ResponseEntity.ok(reactions);
    }

    // ==================== WebSocket Handlers ====================

    /**
     * WebSocket handler for adding a reaction.
     * Clients can send: /app/reaction.add
     *
     * @param request the reaction request
     * @return the created reaction DTO
     */
    @MessageMapping("/reaction.add")
    public MessageReactionDTO wsAddReaction(@Payload ReactionRequest request) {
        log.debug("WebSocket: Adding reaction to message {} by user {}",
                request.getMessageId(), request.getUserId());

        return messageReactionService.addReaction(
                request.getMessageId(), request.getUserId(), request.getEmojiCode());
    }

    /**
     * WebSocket handler for removing a reaction.
     * Clients can send: /app/reaction.remove
     *
     * @param request the reaction request
     * @return true if removed, false if not found
     */
    @MessageMapping("/reaction.remove")
    public Map<String, Object> wsRemoveReaction(@Payload ReactionRequest request) {
        log.debug("WebSocket: Removing reaction {} from message {} by user {}",
                request.getEmojiCode(), request.getMessageId(), request.getUserId());

        boolean removed = messageReactionService.removeReaction(
                request.getMessageId(), request.getUserId(), request.getEmojiCode());

        return Map.of(
                "success", removed,
                "messageId", request.getMessageId(),
                "emojiCode", request.getEmojiCode()
        );
    }

    /**
     * WebSocket handler for toggling a reaction.
     * Clients can send: /app/reaction.toggle
     *
     * @param request the reaction request
     * @return toggle response
     */
    @MessageMapping("/reaction.toggle")
    public MessageReactionDTO.ToggleReactionResponse wsToggleReaction(@Payload ReactionRequest request) {
        log.debug("WebSocket: Toggling reaction on message {} by user {}",
                request.getMessageId(), request.getUserId());

        return messageReactionService.toggleReaction(
                request.getMessageId(), request.getUserId(), request.getEmojiCode());
    }

    /**
     * WebSocket handler for getting reactions.
     * Clients can send: /app/reaction.get
     *
     * @param request the reaction request with message ID
     * @return list of reaction DTOs
     */
    @MessageMapping("/reaction.get")
    public List<MessageReactionDTO> wsGetReactions(@Payload MessageIdRequest request) {
        log.debug("WebSocket: Getting reactions for message {}", request.getMessageId());

        return messageReactionService.getMessageReactions(request.getMessageId());
    }

    /**
     * WebSocket handler for getting reaction summary.
     * Clients can send: /app/reaction.summary
     *
     * @param request the request with message ID and optional user ID
     * @return list of reaction summaries
     */
    @MessageMapping("/reaction.summary")
    public List<MessageReactionDTO.ReactionSummary> wsGetReactionSummary(@Payload SummaryRequest request) {
        log.debug("WebSocket: Getting reaction summary for message {}", request.getMessageId());

        if (request.getUserId() != null && !request.getUserId().isEmpty()) {
            return messageReactionService.getReactionSummary(request.getMessageId(), request.getUserId());
        }
        return messageReactionService.getReactionSummary(request.getMessageId());
    }

    /**
     * Request payload for reaction operations.
     */
    @lombok.Data
    public static class ReactionRequest {
        private String messageId;
        private String userId;
        private String emojiCode;
    }

    /**
     * Request payload for message ID only operations.
     */
    @lombok.Data
    public static class MessageIdRequest {
        private String messageId;
    }

    /**
     * Request payload for summary operations.
     */
    @lombok.Data
    public static class SummaryRequest {
        private String messageId;
        private String userId;
    }
}
