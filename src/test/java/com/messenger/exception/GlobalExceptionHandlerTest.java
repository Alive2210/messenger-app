package com.messenger.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should handle generic exception")
    void shouldHandleGenericException() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/test");
        
        Exception ex = new Exception("Generic error");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
                handler.handleAllExceptions(ex, request);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
    }

    @Test
    @DisplayName("Should handle runtime exception")
    void shouldHandleRuntimeException() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/test");
        
        RuntimeException ex = new RuntimeException("Business error");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
                handler.handleRuntimeException(ex, request);
        
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Business error", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle bad credentials")
    void shouldHandleBadCredentials() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/login");
        
        BadCredentialsException ex = new BadCredentialsException("Invalid credentials");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
                handler.handleBadCredentials(ex, request);
        
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
    }

    @Test
    @DisplayName("Should handle access denied")
    void shouldHandleAccessDenied() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/admin");
        
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
                handler.handleAccessDenied(ex, request);
        
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
    }

    @Test
    @DisplayName("Should handle resource not found")
    void shouldHandleResourceNotFound() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/users/123");
        
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "id", "123");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
                handler.handleResourceNotFound(ex, request);
        
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("123"));
    }

    @Test
    @DisplayName("Should handle device already exists")
    void shouldHandleDeviceAlreadyExists() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/devices");
        
        DeviceAlreadyExistsException ex = new DeviceAlreadyExistsException("Device conflict");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
                handler.handleDeviceAlreadyExists(ex, request);
        
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
    }

    @Test
    @DisplayName("Should handle max devices exceeded")
    void shouldHandleMaxDevicesExceeded() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/devices");
        
        MaxDevicesExceededException ex = new MaxDevicesExceededException(10);
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
                handler.handleMaxDevicesExceeded(ex, request);
        
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getBody().getStatus());
        assertEquals("Too Many Devices", response.getBody().getError());
    }
}
