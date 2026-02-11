package com.messenger.repository;

import com.messenger.entity.Emoji;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmojiRepository extends JpaRepository<Emoji, UUID> {

    Optional<Emoji> findByEmojiCode(String emojiCode);

    Optional<Emoji> findByShortcode(String shortcode);

    List<Emoji> findByCategoryAndIsActiveTrueOrderBySortOrder(Emoji.EmojiCategory category);

    List<Emoji> findByIsActiveTrueOrderByCategoryAscSortOrderAsc();

    List<Emoji> findByIsAnimatedTrueAndIsActiveTrue();

    List<Emoji> findByIsCustomTrueAndIsActiveTrue();

    boolean existsByEmojiCode(String emojiCode);

    boolean existsByShortcode(String shortcode);
}
