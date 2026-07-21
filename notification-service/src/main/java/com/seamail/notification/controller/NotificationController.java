package com.seamail.notification.controller;

import com.seamail.notification.dto.NotificationResponseDto;
import com.seamail.notification.dto.UnreadCountDto;
import com.seamail.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponseDto>> getNotifications(
            @AuthenticationPrincipal(expression = "subject") String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getNotifications(email, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountDto> getUnreadCount(
            @AuthenticationPrincipal(expression = "subject") String email) {
        return ResponseEntity.ok(new UnreadCountDto(notificationService.getUnreadCount(email)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal(expression = "subject") String email,
            @PathVariable Long id) {
        notificationService.markAsRead(id, email);
        return ResponseEntity.noContent().build();
    }
}