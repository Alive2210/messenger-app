package com.messenger.controller;

import com.messenger.dto.ChatDTOs.*;
import com.messenger.dto.MessageDTO;
import com.messenger.service.ChatService;
import com.messenger.service.MessageService;
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
    private final MessageService messageService;

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

    @GetMapping("/contacts")
    public ResponseEntity<List<ContactDTO>> getContacts(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting contacts for user {}", userDetails.getUsername());
        List<ContactDTO> contacts = chatService.getContacts(userDetails.getUsername());
        return ResponseEntity.ok(contacts);
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

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageDTO>> getChatMessages(
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting messages for chat {} by user {}", chatId, userDetails.getUsername());
        List<MessageDTO> messages = messageService.getChatMessages(chatId, userDetails.getUsername(), page, size);
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(
            @PathVariable UUID chatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Deleting chat {} by user {}", chatId, userDetails.getUsername());
        chatService.deleteChat(chatId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
