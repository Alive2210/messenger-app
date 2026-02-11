package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class ReactionDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddReactionRequest {
        private UUID messageId;
        private String emojiCode;
        private String emojiShortcode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RemoveReactionRequest {
        private UUID messageId;
        private String emojiCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReactionDTO {
        private UUID id;
        private UUID messageId;
        private String userId;
        private String username;
        private String emojiCode;
        private String emojiShortcode;
        private Boolean isAnimated;
        private String animatedUrl;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReactionCountDTO {
        private String emojiCode;
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageReactionsDTO {
        private UUID messageId;
        private Map<String, ReactionCountDTO> reactionCounts;
        private boolean userHasReacted;
        private String userReaction;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmojiDTO {
        private UUID id;
        private String emojiCode;
        private String shortcode;
        private String name;
        private String category;
        private Boolean isAnimated;
        private String staticUrl;
        private String animatedUrl;
        private String soundUrl;
        private Integer width;
        private Integer height;
        private Boolean isCustom;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReactionEventDTO {
        private String eventType;
        private ReactionDTO reaction;
        private MessageReactionsDTO summary;
        private String chatId;
    }
}
