package com.messenger.service;

import com.messenger.dto.LocationDTOs.LocationShareRequest;
import com.messenger.dto.LocationShareResponse;
import com.messenger.entity.Message;
import com.messenger.entity.Chat;
import com.messenger.entity.User;
import com.messenger.entity.Message.MessageType;
import com.messenger.repository.ChatRepository;
import com.messenger.repository.MessageRepository;
import com.messenger.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class LocationService {
    @Autowired private MessageRepository messageRepository;
    @Autowired private ChatRepository chatRepository;
    @Autowired private UserRepository userRepository;

    @Transactional
    public LocationShareResponse shareLocation(UUID chatId, String username, double lat, double lon, String label) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("Chat not found"));

        // Create a location message using builder (Lombok-enabled)
        Message msg = Message.builder()
            .chat(chat)
            .sender(user)
            .messageType(Message.MessageType.LOCATION)
            .locationLat(lat)
            .locationLng(lon)
            .locationLabel(label)
            .encryptedContent(null)
            .encryptionIv(null)
            .isDeleted(false)
            .isEdited(false)
            .createdAt(LocalDateTime.now())
            .build();

        messageRepository.save(msg);

        log.info("Location message created: {} in chat {} by {}", msg.getId(), chatId, username);
        return LocationShareResponse.builder()
                .messageId(msg.getId())
                .status("CREATED")
                .message("Location shared successfully")
                .build();
    }
}
