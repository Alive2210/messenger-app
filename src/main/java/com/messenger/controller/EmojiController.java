package com.messenger.controller;

import com.messenger.dto.ReactionDTOs.*;
import com.messenger.service.EmojiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/emojis")
@RequiredArgsConstructor
public class EmojiController {

    private final EmojiService emojiService;

    @GetMapping
    public ResponseEntity<List<EmojiDTO>> getAllEmojis() {
        List<EmojiDTO> emojis = emojiService.getAllEmojis();
        return ResponseEntity.ok(emojis);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<EmojiDTO>> getEmojisByCategory(
            @PathVariable String category) {
        List<EmojiDTO> emojis = emojiService.getEmojisByCategory(category);
        return ResponseEntity.ok(emojis);
    }

    @GetMapping("/animated")
    public ResponseEntity<List<EmojiDTO>> getAnimatedEmojis() {
        List<EmojiDTO> emojis = emojiService.getAnimatedEmojis();
        return ResponseEntity.ok(emojis);
    }

    @GetMapping("/custom")
    public ResponseEntity<List<EmojiDTO>> getCustomEmojis() {
        List<EmojiDTO> emojis = emojiService.getCustomEmojis();
        return ResponseEntity.ok(emojis);
    }

    @GetMapping("/{code}")
    public ResponseEntity<EmojiDTO> getEmojiByCode(@PathVariable String code) {
        EmojiDTO emoji = emojiService.getEmojiByCode(code);
        if (emoji != null) {
            return ResponseEntity.ok(emoji);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/init")
    public ResponseEntity<Void> initializeEmojis() {
        log.info("Initializing default emojis...");
        emojiService.initializeDefaultEmojis();
        return ResponseEntity.ok().build();
    }
}
