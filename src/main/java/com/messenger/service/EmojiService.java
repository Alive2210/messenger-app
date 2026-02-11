package com.messenger.service;

import com.messenger.dto.ReactionDTOs.*;
import com.messenger.entity.Emoji;
import com.messenger.repository.EmojiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmojiService {

    private final EmojiRepository emojiRepository;

    @Transactional(readOnly = true)
    public List<EmojiDTO> getAllEmojis() {
        return emojiRepository.findByIsActiveTrueOrderByCategoryAscSortOrderAsc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmojiDTO> getEmojisByCategory(String category) {
        Emoji.EmojiCategory cat = Emoji.EmojiCategory.valueOf(category.toUpperCase());
        return emojiRepository.findByCategoryAndIsActiveTrueOrderBySortOrder(cat)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmojiDTO> getAnimatedEmojis() {
        return emojiRepository.findByIsAnimatedTrueAndIsActiveTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmojiDTO> getCustomEmojis() {
        return emojiRepository.findByIsCustomTrueAndIsActiveTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmojiDTO getEmojiByCode(String code) {
        return emojiRepository.findByEmojiCode(code)
                .map(this::mapToDTO)
                .orElse(null);
    }

    @Transactional
    public void initializeDefaultEmojis() {
        if (emojiRepository.count() > 0) {
            log.info("Emojis already initialized");
            return;
        }

        log.info("Initializing default emojis...");

        // Стандартные смайлики
        addEmoji("😀", ":grinning:", "Grinning Face", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 1);
        addEmoji("😃", ":smiley:", "Smiley Face", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 2);
        addEmoji("😄", ":smile:", "Smiling Face", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 3);
        addEmoji("😁", ":grin:", "Grinning Face", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 4);
        addEmoji("😆", ":laughing:", "Laughing Face", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 5);
        addEmoji("😅", ":sweat_smile:", "Sweat Smile", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 6);
        addEmoji("🤣", ":rofl:", "Rolling on the Floor Laughing", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 7);
        addEmoji("😂", ":joy:", "Joy", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 8);
        addEmoji("🙂", ":slightly_smiling_face:", "Slightly Smiling Face", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 9);
        addEmoji("😊", ":blush:", "Smiling Face with Smiling Eyes", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 10);
        addEmoji("😇", ":innocent:", "Smiling Face with Halo", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 11);
        addEmoji("🥰", ":smiling_face_with_three_hearts:", "Smiling Face with Hearts", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 12);
        addEmoji("😍", ":heart_eyes:", "Smiling Face with Heart-Eyes", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 13);
        addEmoji("🤩", ":star_struck:", "Star-Struck", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 14);
        addEmoji("😘", ":kissing_heart:", "Face Blowing a Kiss", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 15);
        addEmoji("😗", ":kissing:", "Kissing Face", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 16);
        addEmoji("😚", ":kissing_closed_eyes:", "Kissing Face with Closed Eyes", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 17);
        addEmoji("😙", ":kissing_smiling_eyes:", "Kissing Face with Smiling Eyes", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 18);
        addEmoji("🥲", ":smiling_face_with_tear:", "Smiling Face with Tear", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 19);
        addEmoji("😋", ":yum:", "Face Savoring Food", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 20);

        // Реакции
        addEmoji("👍", ":+1:", "Thumbs Up", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 21);
        addEmoji("👎", ":-1:", "Thumbs Down", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 22);
        addEmoji("❤️", ":heart:", "Red Heart", Emoji.EmojiCategory.SYMBOLS, 1);
        addEmoji("🔥", ":fire:", "Fire", Emoji.EmojiCategory.SYMBOLS, 2);
        addEmoji("😮", ":open_mouth:", "Open Mouth", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 23);
        addEmoji("😢", ":cry:", "Crying Face", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 24);
        addEmoji("🎉", ":tada:", "Party Popper", Emoji.EmojiCategory.ACTIVITY, 1);
        addEmoji("🤔", ":thinking:", "Thinking Face", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 25);
        addEmoji("👀", ":eyes:", "Eyes", Emoji.EmojiCategory.SMILEYS_AND_PEOPLE, 26);

        log.info("Default emojis initialized successfully");
    }

    private void addEmoji(String code, String shortcode, String name, Emoji.EmojiCategory category, int sortOrder) {
        if (emojiRepository.existsByEmojiCode(code)) {
            return;
        }

        Emoji emoji = Emoji.builder()
                .emojiCode(code)
                .shortcode(shortcode)
                .name(name)
                .category(category)
                .isAnimated(false)
                .isCustom(false)
                .isActive(true)
                .sortOrder(sortOrder)
                .build();

        emojiRepository.save(emoji);
    }

    private EmojiDTO mapToDTO(Emoji emoji) {
        return EmojiDTO.builder()
                .id(emoji.getId())
                .emojiCode(emoji.getEmojiCode())
                .shortcode(emoji.getShortcode())
                .name(emoji.getName())
                .category(emoji.getCategory().name())
                .isAnimated(emoji.getIsAnimated())
                .staticUrl(emoji.getStaticUrl())
                .animatedUrl(emoji.getAnimatedUrl())
                .soundUrl(emoji.getSoundUrl())
                .width(emoji.getWidth())
                .height(emoji.getHeight())
                .isCustom(emoji.getIsCustom())
                .build();
    }
}
