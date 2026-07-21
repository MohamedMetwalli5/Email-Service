package com.seamail.notification.exception;

public class NotificationNotFoundException extends ApplicationException {

    public NotificationNotFoundException(String message) {
        super("NOTIFICATION_NOT_FOUND", message);
    }
}