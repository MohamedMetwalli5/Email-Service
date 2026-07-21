-- emails schema for mail-service (replaces the mail slice of SQL Scripts/Tables.sql)
CREATE TABLE IF NOT EXISTS email_id_seq (
    next_val BIGINT NOT NULL
);

INSERT INTO email_id_seq (next_val) VALUES (1);

CREATE TABLE IF NOT EXISTS emails (
    email_id BIGINT NOT NULL PRIMARY KEY,
    sender VARCHAR(255) NOT NULL,
    receiver VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    priority VARCHAR(255) NOT NULL,
    date DATETIME NOT NULL,
    trash TINYINT(1) NOT NULL DEFAULT 0
);
