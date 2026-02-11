package com.messenger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "emoji", indexes = {
    @Index(name = "idx_emoji_category", columnList = "category"),
    @Index(name = "idx_emoji_shortcode", columnList = "shortcode")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Emoji {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "emoji_code", nullable = false, unique = true, length = 50)
    private String emojiCode;

    @Column(name = "shortcode", nullable = false, length = 50)
    private String shortcode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", length = 50)
    @Enumerated(EnumType.STRING)
    private EmojiCategory category;

    @Column(name = "is_animated")
    private Boolean isAnimated = false;

    @Column(name = "static_url")
    private String staticUrl;

    @Column(name = "animated_url")
    private String animatedUrl;

    @Column(name = "sound_url")
    private String soundUrl;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "is_custom")
    private Boolean isCustom = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum EmojiCategory {
        SMILEYS_AND_PEOPLE,
        ANIMALS_AND_NATURE,
        FOOD_AND_DRINK,
        ACTIVITY,
        TRAVEL_AND_PLACES,
        OBJECTS,
        SYMBOLS,
        FLAGS,
        CUSTOM_ANIMATED
    }
}
