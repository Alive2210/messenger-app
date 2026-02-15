package com.messenger.controller;

import com.messenger.dto.ChatDTOs.*;
import com.messenger.service.ChatService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatDTO> createChat(
            @Valid @RequestBody CreateChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Creating chat by user: {}", userDetails.getUsername());
        ChatDTO chat = chatService.createChat(request, userDetails.getUsername());
        return ResponseEntity.ok(chat);
    }

    @GetMapping
    public ResponseEntity<List<ChatDTO>> getMyChats(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ChatDTO> chats = chatService.getUserChats(userDetails.getUsername());
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDTO> getChatById(
            @PathVariable UUID chatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ChatDTO chat = chatService.getChatById(chatId, userDetails.getUsername());
        return ResponseEntity.ok(chat);
    }

    @PostMapping("/{chatId}/participants")
    public ResponseEntity<Void> addParticipant(
            @PathVariable UUID chatId,
            @RequestBody AddParticipantRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        chatService.addParticipant(chatId, request.getUsername(), userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{chatId}/participants/{username}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable UUID chatId,
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        chatService.removeParticipant(chatId, username, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/personal/{username}")
    public ResponseEntity<ChatDTO> getOrCreatePersonalChat(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        ChatDTO chat = chatService.getOrCreatePersonalChat(userDetails.getUsername(), username);
        return ResponseEntity.ok(chat);
    }
}
