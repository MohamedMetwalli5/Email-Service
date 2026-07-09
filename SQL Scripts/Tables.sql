-- Creating the database schema
CREATE SCHEMA IF NOT EXISTS seamail;

-- Using the newly created schema
USE seamail;

-- Creating a sequence table for IDs matching JPA SEQUENCE strategy
CREATE TABLE IF NOT EXISTS email_id_seq (
    next_val BIGINT NOT NULL
);

-- Initializing the sequence with a starting value
INSERT INTO email_id_seq (next_val) VALUES (1) ON DUPLICATE KEY UPDATE next_val = next_val;

-- Creating the emails table
-- Column types/nullability/lengths match the JPA entities (Email.java) and the
-- DTO @Size constraints (SendEmailRequestDto) so ddl-auto=validate passes in
-- prod/local. body is TEXT to honour the 5000-char DTO limit; subject/priority
-- are VARCHAR(255) matching the entity defaults.
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

-- Creating the users table
-- email/password/language column lengths match the User entity (VARCHAR(255)).
-- email is the primary key (inherently unique); password uses VARCHAR(255) to
-- accommodate BCrypt hashes (60 chars) with room for future encoder changes.
CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    language VARCHAR(255),
    profile_picture LONGBLOB,
    PRIMARY KEY (email)
);