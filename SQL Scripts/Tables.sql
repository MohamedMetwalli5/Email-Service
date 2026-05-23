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
CREATE TABLE IF NOT EXISTS emails (
    email_id BIGINT NOT NULL PRIMARY KEY,
    sender VARCHAR(255) NOT NULL,
    receiver VARCHAR(255),
    subject NVARCHAR(40),
    body NVARCHAR(100),
    priority VARCHAR(255),
    date DATETIME NOT NULL,
    trash TINYINT(1) NOT NULL DEFAULT 0
);

-- Creating the users table
CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(40) NOT NULL,
    password VARCHAR(70) NOT NULL,
    language VARCHAR(20),
    profile_picture LONGBLOB,
    PRIMARY KEY (email)
);