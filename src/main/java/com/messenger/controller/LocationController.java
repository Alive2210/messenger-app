package com.messenger.controller;

import com.messenger.dto.LocationDTOs.LocationShareRequest;
import com.messenger.dto.LocationShareResponse;
import com.messenger.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {
    // Use lightweight, offline response until backend persistence is stabilized
    private final LocationService locationService;

    @PostMapping("/share")
    public ResponseEntity<LocationShareResponse> shareLocation(
            @RequestBody LocationShareRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Location share requested by {}: chatId={}, lat={}, lon={}, label={}",
                userDetails.getUsername(), request.getChatId(), request.getLatitude(), request.getLongitude(), request.getLabel());
        // Temporary: return a mock response while backend persistence is stabilized
        LocationShareResponse resp = LocationShareResponse.builder()
                .messageId(java.util.UUID.randomUUID())
                .status("CREATED")
                .message("Location shared (mock)")
                .build();
        return ResponseEntity.ok(resp);
    }
}
