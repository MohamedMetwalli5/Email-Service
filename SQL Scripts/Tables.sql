-- Creating the database schema
CREATE SCHEMA IF NOT EXISTS seamail;

-- Using the newly created schema
USE seamail;

-- Creating a sequence table for IDs if needed (optional)
CREATE TABLE IF NOT EXISTS emails_seq (
    next_val INT NOT NULL
);
-- Initializing the sequence with a starting value
INSERT INTO emails_seq (next_val) VALUES (1) ON DUPLICATE KEY UPDATE next_val = next_val;

-- Creating the emails table with auto-increment
CREATE TABLE IF NOT EXISTS emails (
    emailID INT AUTO_INCREMENT PRIMARY KEY,
    sender VARCHAR(255) NOT NULL,
    receiver VARCHAR(255),
    subject NVARCHAR(40),
    body NVARCHAR(100),
    priority INT,
    date DATETIME,
    trash ENUM('Yes', 'No') NOT NULL DEFAULT 'No'
);

-- Creating the users table
CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(40) NOT NULL,
    password VARCHAR(70) NOT NULL,
    language VARCHAR(20),
    profile_picture LONGBLOB,
    PRIMARY KEY (email)
);