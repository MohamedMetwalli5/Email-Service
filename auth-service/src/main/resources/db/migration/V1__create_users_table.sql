-- users schema for auth-service (replaces the users slice of SQL Scripts/Tables.sql)
CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    language VARCHAR(255),
    profile_picture LONGBLOB,
    PRIMARY KEY (email)
);
