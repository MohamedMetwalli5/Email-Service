package com.seamail.mail.entity;

public enum Mailbox {
    INBOX("Inbox"),
    OUTBOX("Outbox"),
    TRASHBOX("Trashbox");

    private final String value;

    Mailbox(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Mailbox fromString(String text) {
        if (text == null) return INBOX;
        for (Mailbox m : values()) {
            if (m.value.equalsIgnoreCase(text)) return m;
        }
        return INBOX;
    }
}
