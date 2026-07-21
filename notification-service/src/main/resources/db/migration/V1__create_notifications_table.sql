CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    source_email_id BIGINT NOT NULL,
    subject_snapshot VARCHAR(255) NOT NULL,
    sender_snapshot VARCHAR(255) NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uc_notifications_event_id UNIQUE (event_id),
    INDEX idx_notifications_recipient_read (recipient_email, is_read)
);